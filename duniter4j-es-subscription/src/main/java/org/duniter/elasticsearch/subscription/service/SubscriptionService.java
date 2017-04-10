package org.duniter.elasticsearch.subscription.service;

/*
 * #%L
 * UCoin Java Client :: Core API
 * %%
 * Copyright (C) 2014 - 2015 EIS
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableSet;
import org.duniter.core.client.model.ModelUtils;
import org.duniter.core.client.model.bma.jackson.JacksonUtils;
import org.duniter.core.client.model.elasticsearch.Record;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.service.CryptoService;
import org.duniter.core.util.CollectionUtils;
import org.duniter.core.util.Preconditions;
import org.duniter.core.util.StringUtils;
import org.duniter.core.util.crypto.CryptoUtils;
import org.duniter.elasticsearch.client.Duniter4jClient;
import org.duniter.elasticsearch.subscription.PluginSettings;
import org.duniter.elasticsearch.subscription.dao.record.SubscriptionRecordDao;
import org.duniter.elasticsearch.subscription.model.Subscription;
import org.duniter.elasticsearch.subscription.model.email.EmailSubscription;
import org.duniter.elasticsearch.subscription.util.stringtemplate.DateRenderer;
import org.duniter.elasticsearch.subscription.util.stringtemplate.I18nRenderer;
import org.duniter.elasticsearch.threadpool.ThreadPool;
import org.duniter.elasticsearch.user.model.UserEvent;
import org.duniter.elasticsearch.user.service.AdminService;
import org.duniter.elasticsearch.user.service.MailService;
import org.duniter.elasticsearch.user.service.UserEventService;
import org.duniter.elasticsearch.user.service.UserService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.unit.TimeValue;
import org.nuiton.i18n.I18n;
import org.stringtemplate.v4.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Benoit on 30/03/2015.
 */
public class SubscriptionService extends AbstractService {

    private SubscriptionRecordDao subscriptionRecordDao;
    private ThreadPool threadPool;
    private MailService mailService;
    private AdminService adminService;
    private UserEventService userEventService;
    private UserService userService;
    private String emailSubjectPrefix;

    @Inject
    public SubscriptionService(Duniter4jClient client,
                               PluginSettings settings,
                               CryptoService cryptoService,
                               SubscriptionRecordDao subscriptionRecordDao,
                               ThreadPool threadPool,
                               MailService mailService,
                               AdminService adminService,
                               UserService userService,
                               UserEventService userEventService) {
        super("subscription.service", client, settings, cryptoService);
        this.subscriptionRecordDao = subscriptionRecordDao;
        this.threadPool = threadPool;
        this.mailService = mailService;
        this.adminService = adminService;
        this.userService = userService;
        this.userEventService = userEventService;
        this.emailSubjectPrefix = pluginSettings.getMailSubjectPrefix().trim();
        if (StringUtils.isNotBlank(emailSubjectPrefix)) {
            emailSubjectPrefix += " "; // add one trailing space
        }
    }

    public String create(String json) {
        JsonNode actualObj = readAndVerifyIssuerSignature(json);
        String issuer = getIssuer(actualObj);

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Indexing a subscription from issuer [%s]", issuer.substring(0, 8)));
        }

        return subscriptionRecordDao.create(json);
    }

    public void update(String id, String json) {
        JsonNode actualObj = readAndVerifyIssuerSignature(json);
        String issuer = getIssuer(actualObj);

        // Check same document issuer
        subscriptionRecordDao.checkSameDocumentIssuer(id, issuer);

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Updating subscription [%s] from issuer [%s]", id, issuer.substring(0, 8)));
        }

        subscriptionRecordDao.update(id, json);
    }

    public SubscriptionService startScheduling() {
        if (!pluginSettings.getMailEnable()) {
            logger.warn(I18n.t("duniter4j.es.subscription.error.mailDisabling"));
            return this;
        }

        threadPool.scheduleWithFixedDelay(
                this::executeEmailSubscriptions,
                new TimeValue(pluginSettings.getExecuteEmailSubscriptionsInterval()));

        return this;
    }

    public void executeEmailSubscriptions() {

        final String senderPubkey = pluginSettings.getNodePubkey();

        int from = 0;
        int size = 10;

        boolean hasMore = true;
        while (hasMore) {
            List<Subscription> subscriptions = subscriptionRecordDao.getSubscriptions(from, size, senderPubkey, EmailSubscription.TYPE);

            // Get profiles titles, for issuers and the sender
            Set<String> issuers =  subscriptions.stream()
                    .map(Subscription::getIssuer)
                    .distinct()
                    .collect(Collectors.toSet());
            final Map<String, String> profileTitles = userService.getProfileTitles(
                    ImmutableSet.<String>builder().addAll(issuers).add(senderPubkey).build());
            final String senderName = (profileTitles != null && profileTitles.containsKey(senderPubkey)) ? profileTitles.get(senderPubkey) :
                ModelUtils.minifyPubkey(senderPubkey);

            subscriptions.stream()
                    .map(record -> decryptEmailSubscription((EmailSubscription)record))
                    .filter(Objects::nonNull)
                    .map(record -> processEmailSubscription(record, senderPubkey, senderName, profileTitles))
                    .filter(Objects::nonNull)
                    .forEach(this::saveSubscription);

            hasMore = CollectionUtils.size(subscriptions) >= size;
            from += size;
        }
    }

    /* -- protected methods -- */


    protected EmailSubscription decryptEmailSubscription(EmailSubscription subscription) {
        Preconditions.checkNotNull(subscription);
        Preconditions.checkNotNull(subscription.getId());

        if (StringUtils.isBlank(subscription.getRecipientContent()) || StringUtils.isBlank(subscription.getNonce()) ||
                StringUtils.isBlank(subscription.getIssuer())) {
            logger.error(String.format("Invalid subscription [%s]. Missing field 'recipientContent', 'nonce' or 'issuer'.", subscription.getId()));
            return null;
        }

        String jsonContent;
        try {
            jsonContent = cryptoService.openBox(subscription.getRecipientContent(),
                    CryptoUtils.decodeBase58(subscription.getNonce()),
                    CryptoUtils.decodeBase58(subscription.getIssuer()),
                    pluginSettings.getNodeKeypair().getSecKey()
            );
        } catch(Exception e) {
            logger.error(String.format("Could not decrypt email subscription content for subscription [%s]", subscription.getId()));
            return null;
        }

        try {
            EmailSubscription.Content content = objectMapper.readValue(jsonContent, EmailSubscription.Content.class);
            subscription.setContent(content);
        } catch(Exception e) {
            logger.error(String.format("Could not parse email subscription content [%s]: %s", jsonContent, e.getMessage()));
            return null;
        }

        return subscription;
    }

    protected EmailSubscription processEmailSubscription(final EmailSubscription subscription,
                                                         final String senderPubkey,
                                                         final String senderName,
                                                         final Map<String, String> profileTitles) {
        Preconditions.checkNotNull(subscription);

        logger.info(String.format("Processing email subscription [%s]", subscription.getId()));

        Long lastTime = 0l; // TODO get it from subscription ?

        // Get last user events
        String[] includes = subscription.getContent() == null ? null : subscription.getContent().getIncludes();
        String[] excludes = subscription.getContent() == null ? null : subscription.getContent().getExcludes();
        List<UserEvent> userEvents = userEventService.getUserEvents(subscription.getIssuer(), lastTime, includes, excludes);


        STGroup templates = new STGroupDir("templates", '$', '$');
        templates.registerRenderer(Date.class, new DateRenderer());
        //templates.registerRenderer(String.class, new StringRenderer());
        //templates.registerRenderer(Number.class, new NumberRenderer());
        templates.registerRenderer(String.class, new I18nRenderer());
        String[] localParts = subscription.getContent() != null && subscription.getContent().getLocale() != null ?
                subscription.getContent().getLocale().split("-") : new String[]{"en", "GB"};

        Locale issuerLocale = localParts.length >= 2 ? new Locale(localParts[0].toLowerCase(), localParts[1].toUpperCase()) : new Locale(localParts[0].toLowerCase());

        // Compute text
        String text = fillTemplate(
                templates.getInstanceOf("text_email"),
                subscription,
                senderPubkey,
                senderName,
                profileTitles,
                userEvents,
                pluginSettings.getCesiumUrl())
                .render(issuerLocale);

        // Compute HTML content
        String html = fillTemplate(
                templates.getInstanceOf("html_email_content"),
                subscription,
                senderPubkey,
                senderName,
                profileTitles,
                userEvents,
                pluginSettings.getCesiumUrl())
                .render(issuerLocale);

        mailService.sendHtmlEmailWithText(
                emailSubjectPrefix + I18n.t("duniter4j.es.subscription.email.subject", userEvents.size()),
                text,
                "<body>" + html + "</body>",
                subscription.getContent().getEmail());
        return subscription;
    }


    public static ST fillTemplate(ST template,
                                      EmailSubscription subscription,
                                      String senderPubkey,
                                      String senderName,
                                      Map<String, String> issuerProfilNames,
                                      List<UserEvent> userEvents,
                                      String cesiumSiteUrl) {
        String issuerName = issuerProfilNames != null && issuerProfilNames.containsKey(subscription.getIssuer()) ?
                issuerProfilNames.get(subscription.getIssuer()) :
                ModelUtils.minifyPubkey(subscription.getIssuer());


        try {
            // Compute body
            template.add("url", cesiumSiteUrl);
            template.add("issuer", issuerName);
            template.add("senderPubkey", senderPubkey);
            template.add("senderName", senderName);
            userEvents.forEach(userEvent -> {
                String description = userEvent.getParams() != null ?
                        I18n.t("duniter.user.event." + userEvent.getCode().toUpperCase(), userEvent.getParams()) :
                        I18n.t("duniter.user.event." + userEvent.getCode().toUpperCase());
                template.addAggr("events.{description, time}", new Object[]{
                    description,
                    new Date(userEvent.getTime() * 1000)
                });

            });

            return template;

        }
        catch (Exception e) {
          throw new TechnicalException(e);
        }
    }


    public static String computeTextEmail(STGroup templates,
                                          Locale issuerLocale,
                                          EmailSubscription subscription,
                                          String senderPubkey,
                                          String senderName,
                                          Map<String, String> issuerProfilNames,
                                          List<UserEvent> userEvents,
                                          String cesiumSiteUrl) {
        String issuerName = issuerProfilNames != null && issuerProfilNames.containsKey(subscription.getIssuer()) ?
                issuerProfilNames.get(subscription.getIssuer()) :
                ModelUtils.minifyPubkey(subscription.getIssuer());

        try {
            // Compute text content
            ST tpl = templates.getInstanceOf("text_email");
            tpl.add("url", cesiumSiteUrl);
            tpl.add("issuer", issuerName);
            tpl.add("url", cesiumSiteUrl);
            tpl.add("senderPubkey", senderPubkey);
            tpl.add("senderName", senderName);
            userEvents.forEach(userEvent -> {
                String description = userEvent.getParams() != null ?
                        I18n.t("duniter.user.event." + userEvent.getCode().toUpperCase(), userEvent.getParams()) :
                        I18n.t("duniter.user.event." + userEvent.getCode().toUpperCase());
                tpl.addAggr("events.{description, time}", new Object[]{
                        description,
                        new Date(userEvent.getTime() * 1000)
                });

            });

            return tpl.render();
        }
        catch (Exception e) {
            throw new TechnicalException(e);
        }
    }

    protected EmailSubscription saveSubscription(EmailSubscription subscription) {
        Preconditions.checkNotNull(subscription);

        //mailService.sendEmail();
        return subscription;
    }

    private String toJson(EmailSubscription subscription) {
        return toJson(subscription, false);
    }

    private String toJson(EmailSubscription subscription, boolean cleanHashAndSignature) {
        try {
            String json = objectMapper.writeValueAsString(subscription);
            if (cleanHashAndSignature) {
                json = JacksonUtils.removeAttribute(json, Record.PROPERTY_SIGNATURE);
                json = JacksonUtils.removeAttribute(json, Record.PROPERTY_HASH);
            }
            return json;
        } catch(JsonProcessingException e) {
            throw new TechnicalException("Unable to serialize UserEvent object", e);
        }
    }
}

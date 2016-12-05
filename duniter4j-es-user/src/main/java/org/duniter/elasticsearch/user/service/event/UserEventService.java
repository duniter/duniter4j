package org.duniter.elasticsearch.user.service.event;

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
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.service.CryptoService;
import org.duniter.core.service.MailService;
import org.duniter.core.util.CollectionUtils;
import org.duniter.core.util.StringUtils;
import org.duniter.core.util.crypto.CryptoUtils;
import org.duniter.core.util.crypto.KeyPair;
import org.duniter.elasticsearch.PluginSettings;
import org.duniter.elasticsearch.service.AbstractService;
import org.duniter.elasticsearch.service.changes.ChangeEvent;
import org.duniter.elasticsearch.service.changes.ChangeListener;
import org.duniter.elasticsearch.service.changes.ChangeService;
import org.duniter.elasticsearch.service.changes.ChangeUtils;
import org.duniter.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.nuiton.i18n.I18n;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by Benoit on 30/03/2015.
 */
public class UserEventService extends AbstractService {

    public static final String INDEX = "user";
    public static final String EVENT_TYPE = "event";
    private static final Map<String, UserEventListener> LISTENERS = new HashMap<>();

    public static void registerListener(UserEventListener listener) {
        LISTENERS.put(listener.getId(), listener);
    }

    public static void unregisterListener(UserEventListener listener) {
        LISTENERS.remove(listener.getId());
    }

    private final MailService mailService;
    private final ThreadPool threadPool;
    public final KeyPair nodeKeyPair;
    public final String nodePubkey;
    public final boolean mailEnable;

    @Inject
    public UserEventService(Client client, PluginSettings settings, CryptoService cryptoService, MailService mailService,
                            ThreadPool threadPool) {
        super("duniter.event." + INDEX, client, settings, cryptoService);
        this.mailService = mailService;
        this.threadPool = threadPool;
        this.nodeKeyPair = getNodeKeyPairOrNull(pluginSettings);
        this.nodePubkey = getNodePubKey(this.nodeKeyPair);
        this.mailEnable = pluginSettings.getMailEnable();
        if (!this.mailEnable && logger.isTraceEnabled()) {
            logger.trace("Mail disable");
        }
    }

    /**
     * Notify cluster admin
     */
    public void notifyAdmin(UserEvent event) {
        Locale locale = I18n.getDefaultLocale(); // TODO get locale from admin

        // Add new event to index
        if (StringUtils.isNotBlank(nodePubkey)) {
            indexEvent(nodePubkey, locale, event);
        }

        // Retrieve admin email
        String adminEmail = pluginSettings.getMailAdmin();
        if (StringUtils.isBlank(adminEmail) && StringUtils.isNotBlank(nodePubkey)) {
            adminEmail = getEmailByPk(nodePubkey);
        }

        // Send email to admin
        if (StringUtils.isNotBlank(adminEmail)) {
            String subjectPrefix = pluginSettings.getMailSubjectPrefix();
            sendEmail(adminEmail,
                    I18n.l(locale, "duniter4j.event.subject."+event.getType().name(), subjectPrefix),
                    event.getLocalizedMessage(locale));
        }
    }

    /**
     * Notify a user
     */
    public void notifyUser(String recipient, UserEvent event) {
        // Notify user
        threadPool.schedule(() -> {
            doNotifyUser(recipient, event);
        }, TimeValue.timeValueMillis(100));
    }

    public String indexEvent(String recipient, Locale locale, UserEvent event) {
        // Generate json
        String eventJson;
        if (StringUtils.isNotBlank(nodePubkey)) {
            eventJson = UserEventUtils.toJson(nodePubkey, recipient, locale, event, null);
            String signature = cryptoService.sign(eventJson, nodeKeyPair.getSecKey());
            eventJson = UserEventUtils.toJson(nodePubkey, recipient, locale, event, signature);
        } else {
            // Node has not keyring : TODO no issuer ?
            eventJson = UserEventUtils.toJson(recipient, recipient, locale, event, null);
        }

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Indexing a event to recipient [%s]", recipient.substring(0, 8)));
        }

        // do indexation
        return indexEvent(eventJson, false /*checkSignature*/);
    }

    public String indexEvent(String eventJson) {
        return indexEvent(eventJson, true);
    }

    public String indexEvent(String eventJson, boolean checkSignature) {

        if (checkSignature) {
            JsonNode jsonNode = readAndVerifyIssuerSignature(eventJson);
            String recipient = jsonNode.get(org.duniter.core.client.model.elasticsearch.Event.PROPERTY_ISSUER).asText();
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Indexing a event to recipient [%s]", recipient.substring(0, 8)));
            }
        }
        if (logger.isTraceEnabled()) {
            logger.trace(eventJson);
        }

        IndexResponse response = client.prepareIndex(INDEX, EVENT_TYPE)
                .setSource(eventJson)
                .setRefresh(false)
                .execute().actionGet();

        return response.getId();
    }

    /* -- Internal methods -- */

    public static XContentBuilder createEventType() {
        try {
            XContentBuilder mapping = XContentFactory.jsonBuilder().startObject().startObject(EVENT_TYPE)
                    .startObject("properties")

                    // type
                    .startObject("type")
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()

                    // issuer
                    .startObject("issuer")
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()

                    // recipient
                    .startObject("recipient")
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()

                    // time
                    .startObject("time")
                    .field("type", "integer")
                    .endObject()

                    // code
                    .startObject("code")
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()

                    // link
                    .startObject("link")
                        .field("type", "nested")
                        .field("dynamic", "false")
                        .startObject("properties")
                            .startObject("index")
                            .field("type", "string")
                            .field("index", "not_analyzed")
                            .endObject()
                            .startObject("type")
                                .field("type", "string")
                                .field("index", "not_analyzed")
                            .endObject()
                            .startObject("id")
                                .field("type", "string")
                                .field("index", "not_analyzed")
                            .endObject()
                        .endObject()
                    .endObject()

                    // message
                    .startObject("message")
                    .field("type", "string")
                    .endObject()

                    // params
                    .startObject("params")
                    .field("type", "string")
                    .endObject()

                    .endObject()
                    .endObject().endObject();

            return mapping;
        }
        catch(IOException ioe) {
            throw new TechnicalException(String.format("Error while getting mapping for index [%s/%s]: %s", INDEX, EVENT_TYPE, ioe.getMessage()), ioe);
        }
    }

    private String getEmailByPk(String issuerPk) {
        // TODO get it from user profile ?
        return pluginSettings.getMailAdmin();
    }

    private String getEmailSubject(Locale locale, UserEvent event) {

        return  I18n.l(locale, "duniter4j.event.subject."+event.getType().name());
    }

    /**
     * Send email
     */
    private void sendEmail(String recipients, String subject, String textContent) {
        if (!this.mailEnable) return;

        String smtpHost =  pluginSettings.getMailSmtpHost();
        int smtpPort =  pluginSettings.getMailSmtpPort();
        String smtpUsername =  pluginSettings.getMailSmtpUsername();
        String smtpPassword =  pluginSettings.getMailSmtpPassword();
        String from =  pluginSettings.getMailFrom();

        try {
            mailService.sendTextEmail(smtpHost, smtpPort, smtpUsername, smtpPassword, from, recipients, subject, textContent);
        }
        catch(TechnicalException e) {
            logger.error(String.format("Could not send email: %s", e.getMessage())/*, e*/);
        }
    }



    private KeyPair getNodeKeyPairOrNull(PluginSettings pluginSettings) {

        if (StringUtils.isNotBlank(pluginSettings.getKeyringSalt()) &&
                StringUtils.isNotBlank(pluginSettings.getKeyringPassword())) {
            return cryptoService.getKeyPair(pluginSettings.getKeyringSalt(),
                    pluginSettings.getKeyringPassword());
        }

        return null;
    }

    private String getNodePubKey(KeyPair nodeKeyPair) {
        if (nodeKeyPair == null) return null;
        return CryptoUtils.encodeBase58(nodeKeyPair.getPubKey());
    }

    /**
     * Notify a user
     */
    private void doNotifyUser(String recipient, UserEvent event) {

        String email = getEmailByPk(recipient);
        Locale locale = I18n.getDefaultLocale(); // TODO get locale

        // Add new event to index
        indexEvent(recipient, locale, event);

        // Send email to user
        // TODO : group email by day ?
        if (StringUtils.isNotBlank(email)) {
            String subjectPrefix = pluginSettings.getMailSubjectPrefix();
            sendEmail(email,
                    I18n.l(locale, "duniter4j.event.subject."+event.getType().name(), subjectPrefix),
                    event.getLocalizedMessage(locale));
        }

        for (UserEventListener listener: LISTENERS.values()) {
            if (recipient.equals(listener.getPubkey())) {
                listener.onEvent(event);
            }
        }
    }
}

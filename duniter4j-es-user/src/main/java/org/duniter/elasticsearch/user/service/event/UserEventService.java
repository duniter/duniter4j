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
public class UserEventService extends AbstractService implements ChangeListener {

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
        ChangeService.registerListener(this);
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
     * Notify a new document
     */
    public void notifyNewDocument(String index, String type, String id, String issuer) {

        String docId = String.format("%s/%s/%s", index, type, id);
        logger.info(String.format("Detected new document at: %s", docId));

        notifyUser(issuer, new UserEvent(UserEvent.EventType.INFO, UserEventCodes.CREATE_DOC.name(), new String[]{docId}));
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

    @Override
    public void onChanges(String json) {
        // TODO get doc issuer
        String issuer = nodePubkey;

        ChangeEvent event = ChangeUtils.fromJson(objectMapper, json);

        // Skip event itself (avoid recursive call)
        if (event.getIndex().equals(INDEX) && event.getType().equals(EVENT_TYPE)) {
            return;
        }

        if (event.getOperation() == ChangeEvent.Operation.CREATE) {
            notifyNewDocument(event.getIndex(), event.getType(), event.getId(), issuer);
        }

    }

    @Override
    public String getId() {
        return "UserEventService";
    }

    /**
     * Delete blockchain index, and all data
     */
    public UserEventService deleteIndex() {
        deleteIndexIfExists(INDEX);
        return this;
    }

    public boolean existsIndex() {
        return super.existsIndex(INDEX);
    }

    /**
     * Create index need for blockchain registry, if need
     */
    public UserEventService createIndexIfNotExists() {
        try {
            if (!existsIndex(INDEX)) {
                createIndex();
            }
        }
        catch(JsonProcessingException e) {
            throw new TechnicalException(String.format("Error while creating index [%s]", INDEX));
        }

        return this;
    }

    /**
     * Create index need for category registry
     * @throws JsonProcessingException
     */
    public UserEventService createIndex() throws JsonProcessingException {
        logger.info(String.format("Creating index [%s/%s]", INDEX, EVENT_TYPE));

        CreateIndexRequestBuilder createIndexRequestBuilder = client.admin().indices().prepareCreate(INDEX);
        Settings indexSettings = Settings.settingsBuilder()
                .put("number_of_shards", 2)
                .put("number_of_replicas", 1)
                //.put("analyzer", createDefaultAnalyzer())
                .build();
        createIndexRequestBuilder.setSettings(indexSettings);
        createIndexRequestBuilder.addMapping(EVENT_TYPE, createEventType());
        createIndexRequestBuilder.execute().actionGet();

        return this;
    }

    public String indexEvent(String recipient, Locale locale, UserEvent event) {
        // Generate json
        String eventJson;
        if (StringUtils.isNotBlank(nodePubkey)) {
            eventJson = toJson(nodePubkey, recipient, locale, event, null);
            String signature = cryptoService.sign(eventJson, nodeKeyPair.getSecKey());
            eventJson = toJson(nodePubkey, recipient, locale, event, signature);
        } else {
            // Node has not keyring : TODO no issuer ?
            eventJson = toJson(recipient, recipient, locale, event, null);
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

    public XContentBuilder createEventType() {
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

                    // params
                    .startObject("params")
                    .field("type", "string")
                    .endObject()

                    // message
                    .startObject("message")
                    .field("type", "string")
                    .field("index", "not_analyzed")
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

    private String toJson(String issuer, String recipient, Locale locale, UserEvent event, String signature) {
        try {
            XContentBuilder eventObject = XContentFactory.jsonBuilder().startObject()
                    .field("type", event.getType().name())
                    .field("issuer", issuer) // TODO isuer = node pubkey
                    .field("recipient", recipient)
                    .field("time", event.getTime())
                    .field("code", event.getCode())
                    .field("message", event.getLocalizedMessage(locale));
            if (CollectionUtils.isNotEmpty(event.getParams())) {
                eventObject.array("params", event.getParams());
            }
            if (StringUtils.isNotBlank(signature)) {
                eventObject.field("signature", signature);
            }
            eventObject.endObject();
            return eventObject.string();
        }
        catch(IOException e) {
            throw new TechnicalException(e);
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
        if (StringUtils.isNotBlank(email)) {
            String subjectPrefix = pluginSettings.getMailSubjectPrefix();
            sendEmail(email,
                    I18n.l(locale, "duniter4j.event.subject."+event.getType().name(), subjectPrefix),
                    event.getLocalizedMessage(locale));
        }

        for (UserEventListener listener: LISTENERS.values()) {
            listener.onEvent(event);
        }
    }
}

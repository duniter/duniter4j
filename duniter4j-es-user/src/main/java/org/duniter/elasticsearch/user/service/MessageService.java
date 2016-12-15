package org.duniter.elasticsearch.user.service;

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
import org.duniter.core.client.model.ModelUtils;
import org.duniter.core.client.model.elasticsearch.Record;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.service.CryptoService;
import org.duniter.elasticsearch.PluginSettings;
import org.duniter.elasticsearch.exception.InvalidSignatureException;
import org.duniter.elasticsearch.service.AbstractService;
import org.duniter.elasticsearch.service.BlockchainService;
import org.duniter.elasticsearch.user.model.Message;
import org.duniter.elasticsearch.user.model.UserEvent;
import org.duniter.elasticsearch.user.model.UserEventCodes;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.nuiton.i18n.I18n;

import java.io.IOException;
import java.util.Map;

/**
 * Created by Benoit on 30/03/2015.
 */
public class MessageService extends AbstractService {

    public static final String INDEX = "message";
    public static final String RECORD_TYPE = "record";
    public static final String OUTBOX_TYPE = "outbox";

    private final UserEventService userEventService;

    @Inject
    public MessageService(Client client, PluginSettings settings,
                          CryptoService cryptoService, UserEventService userEventService) {
        super("duniter." + INDEX, client, settings, cryptoService);
        this.userEventService = userEventService;
    }

    /**
     * Delete blockchain index, and all data
     * @throws JsonProcessingException
     */
    public MessageService deleteIndex() {
        deleteIndexIfExists(INDEX);
        return this;
    }

    public boolean existsIndex() {
        return super.existsIndex(INDEX);
    }

    /**
     * Create index need for blockchain registry, if need
     */
    public MessageService createIndexIfNotExists() {
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
    public MessageService createIndex() throws JsonProcessingException {
        logger.info(String.format("Creating index [%s/%s]", INDEX, RECORD_TYPE));

        CreateIndexRequestBuilder createIndexRequestBuilder = client.admin().indices().prepareCreate(INDEX);
        Settings indexSettings = Settings.settingsBuilder()
                .put("number_of_shards", 2)
                .put("number_of_replicas", 1)
                //.put("analyzer", createDefaultAnalyzer())
                .build();
        createIndexRequestBuilder.setSettings(indexSettings);
        createIndexRequestBuilder.addMapping(RECORD_TYPE, createRecordType());
        createIndexRequestBuilder.addMapping(OUTBOX_TYPE, createOutboxType());
        createIndexRequestBuilder.execute().actionGet();

        return this;
    }

    public String indexRecordFromJson(String recordJson) {

        JsonNode actualObj = readAndVerifyIssuerSignature(recordJson);
        String issuer = getIssuer(actualObj);
        String recipient = getMandatoryField(actualObj, Message.PROPERTY_RECIPIENT).asText();
        Long time = getMandatoryField(actualObj, Message.PROPERTY_TIME).asLong();

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Indexing a message from issuer [%s]", issuer.substring(0, 8)));
        }

        IndexResponse response = client.prepareIndex(INDEX, RECORD_TYPE)
                .setSource(recordJson)
                .setRefresh(false)
                .execute().actionGet();

        String messageId = response.getId();

        // Notify recipient
        userEventService.notifyUser(UserEvent.newBuilder(UserEvent.EventType.INFO, UserEventCodes.MESSAGE_RECEIVED.name())
                .setRecipient(recipient)
                .setMessage(I18n.n("duniter.user.event.message.received"), issuer, ModelUtils.minifyPubkey(issuer))
                .setTime(time)
                .setReference(INDEX, RECORD_TYPE, messageId)
                .build());

        return messageId;
    }

    public String indexOuboxFromJson(String recordJson) {

        JsonNode actualObj = readAndVerifyIssuerSignature(recordJson);
        String issuer = getIssuer(actualObj);

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Indexing a message from issuer [%s]", issuer.substring(0, 8)));
        }

        IndexResponse response = client.prepareIndex(INDEX, OUTBOX_TYPE)
                .setSource(recordJson)
                .setRefresh(false)
                .execute().actionGet();

        return response.getId();
    }

    public void markMessageAsRead(String signature, String id) {
        Map<String, Object> fields = getMandatoryFieldsById(INDEX, RECORD_TYPE, id, Message.PROPERTY_HASH, Message.PROPERTY_RECIPIENT);
        String recipient = fields.get(UserEvent.PROPERTY_RECIPIENT).toString();
        String hash = fields.get(UserEvent.PROPERTY_HASH).toString();

        // Check signature
        boolean valid = cryptoService.verify(hash, signature, recipient);
        if (!valid) {
            throw new InvalidSignatureException("Invalid signature: only the recipient can mark an message as read.");
        }

        UpdateRequestBuilder request = client.prepareUpdate(INDEX, RECORD_TYPE, id)
                .setDoc("read_signature", signature);
        request.execute();
    }

    /* -- Internal methods -- */

    public XContentBuilder createRecordType() {
        return createMapping(RECORD_TYPE);
    }

    public XContentBuilder createOutboxType() {
        return createMapping(OUTBOX_TYPE);
    }

    public XContentBuilder createMapping(String typeName) {
        try {
            XContentBuilder mapping = XContentFactory.jsonBuilder().startObject().startObject(typeName)
                    .startObject("properties")

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

                    // nonce
                    .startObject("nonce")
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()

                    // content
                    .startObject("content")
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()

                    // read_signature
                    .startObject("read_signature")
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()

                    .endObject()
                    .endObject().endObject();

            return mapping;
        }
        catch(IOException ioe) {
            throw new TechnicalException(String.format("Error while getting mapping for index [%s/%s]: %s", INDEX, RECORD_TYPE, ioe.getMessage()), ioe);
        }
    }
}

package org.duniter.elasticsearch.service;

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
import org.duniter.elasticsearch.PluginSettings;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.IOException;

/**
 * Created by Benoit on 30/03/2015.
 */
public class MessageService extends AbstractService {

    public static final String INDEX = "message";
    public static final String RECORD_TYPE = "record";
    public static final String OUTBOX_TYPE = "outbox";


    @Inject
    public MessageService(Client client, PluginSettings settings, CryptoService cryptoService) {
        super("gchange." + INDEX, client, settings, cryptoService);
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

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Indexing a message from issuer [%s]", issuer.substring(0, 8)));
        }

        IndexResponse response = client.prepareIndex(INDEX, RECORD_TYPE)
                .setSource(recordJson)
                .setRefresh(false)
                .execute().actionGet();

        return response.getId();
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

                    .endObject()
                    .endObject().endObject();

            return mapping;
        }
        catch(IOException ioe) {
            throw new TechnicalException(String.format("Error while getting mapping for index [%s/%s]: %s", INDEX, RECORD_TYPE, ioe.getMessage()), ioe);
        }
    }
}

package org.duniter.elasticsearch.service.market;

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
import com.google.common.collect.Sets;
import com.google.gson.JsonSyntaxException;
import org.duniter.core.client.model.elasticsearch.Record;
import org.duniter.core.client.service.bma.WotRemoteService;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.service.CryptoService;
import org.duniter.elasticsearch.PluginSettings;
import org.duniter.elasticsearch.service.AbstractService;
import org.duniter.elasticsearch.service.ServiceLocator;
import org.duniter.elasticsearch.service.exception.InvalidFormatException;
import org.duniter.elasticsearch.service.exception.InvalidSignatureException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Singleton;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.IOException;
import java.util.Set;

/**
 * Created by Benoit on 30/03/2015.
 */
@Singleton
public class RecordMarketService extends AbstractService<RecordMarketService> {

    private static final ESLogger log = ESLoggerFactory.getLogger(RecordMarketService.class.getName());

    private static final String JSON_STRING_PROPERTY_REGEX = "[,]?[\"\\s\\n\\r]*%s[\"]?[\\s\\n\\r]*:[\\s\\n\\r]*\"[^\"]+\"";

    public static final String INDEX_NAME = "market";

    public static final String INDEX_TYPE = "record";

    private WotRemoteService wotRemoteService;

    private CryptoService cryptoService;

    private ServiceLocator serviceLocator;

    @Inject
    public RecordMarketService(Client client, PluginSettings config, ServiceLocator serviceLocator) {
        super(client, config);
        this.serviceLocator = serviceLocator;
    }

    @Override
    public RecordMarketService start() {
        wotRemoteService = serviceLocator.getWotRemoteService();
        cryptoService = serviceLocator.getCryptoService();
        return super.start();
    }

    @Override
    public void close() {
        wotRemoteService = null;
        cryptoService = null;
        super.close();
    }

    /**
     * Delete blockchain index, and all data
     * @throws JsonProcessingException
     */
    public void deleteIndex() throws JsonProcessingException {
        deleteIndexIfExists(INDEX_NAME);
    }


    public boolean existsIndex() {
        return super.existsIndex(INDEX_NAME);
    }

    /**
     * Create index need for blockchain registry, if need
     */
    public void createIndexIfNotExists() {
        try {
            if (!existsIndex(INDEX_NAME)) {
                createIndex();
            }
        }
        catch(JsonProcessingException e) {
            throw new TechnicalException(String.format("Error while creating index [%s]", INDEX_NAME));
        }
    }

    /**
     * Create index need for record registry
     * @throws JsonProcessingException
     */
    public void createIndex() throws JsonProcessingException {
        CreateIndexRequestBuilder createIndexRequestBuilder = getClient().admin().indices().prepareCreate(INDEX_NAME);
        org.elasticsearch.common.settings.Settings indexSettings = org.elasticsearch.common.settings.Settings.settingsBuilder()
                .put("number_of_shards", 3)
                .put("number_of_replicas", 2)
                //.put("analyzer", createDefaultAnalyzer())
                .build();

        // Create record index type
        log.info(String.format("Creating index [%s/%s]", INDEX_NAME, INDEX_TYPE));
        createIndexRequestBuilder.setSettings(indexSettings);
        createIndexRequestBuilder.addMapping(INDEX_NAME, createIndexMapping(INDEX_TYPE));
        createIndexRequestBuilder.execute().actionGet();
    }

    /**
     * Index a new record
     * @param recordJson
     * @return the record id
     */
    public String indexRecordFromJson(String recordJson) {

        return indexRecordFromJson(recordJson, INDEX_TYPE);
    }

    /* -- Internal methods -- */


    public XContentBuilder createIndexMapping(String indexType) {
        String stringAnalyzer = getPluginSettings().getIndexStringAnalyzer();


        try {
            XContentBuilder mapping = XContentFactory.jsonBuilder().startObject().startObject(indexType)
                    .startObject("properties")

                    // title
                    .startObject("title")
                    .field("type", "string")
                    .field("analyzer", stringAnalyzer)
                    .endObject()

                    // description
                    .startObject("description")
                    .field("type", "string")
                    .field("analyzer", stringAnalyzer)
                    .endObject()

                    // time
                    .startObject("time")
                    .field("type", "integer")
                    .endObject()

                    // price
                    .startObject("price")
                    .field("type", "double")
                    .endObject()

                    // price Id UD
                    .startObject("priceInUD")
                    .field("type", "boolean")
                    .endObject()

                    // issuer
                    .startObject("issuer")
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()

                    // location
                    .startObject("location")
                    .field("type", "string")
                    .endObject()

                    // geoPoint
                    .startObject("geoPoint")
                    .field("type", "geo_point")
                    .endObject()

                    // pictures
                    .startObject("pictures")
                    .field("type", "nested")
                    .startObject("properties")
                    .startObject("src") // src
                    .field("type", "attachment")
                    .field("index", "not_analyzed")
                    .endObject()
                    .startObject("title") // title
                    .field("title", "string")
                    .field("analyzer", stringAnalyzer)
                    .startObject("norms") // disabled norms on title
                    .field("enabled", "false")
                    .endObject()
                    .endObject()
                    .endObject()
                    .endObject()

                    // categories
                    .startObject("categories")
                    .field("type", "nested")
                    .startObject("properties")
                    .startObject("cat1") // cat1
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()
                    .startObject("cat2") // cat2
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()
                    .endObject()
                    .endObject()

                    // tags
                    .startObject("tags")
                    .field("type", "completion")
                    .field("search_analyzer", "simple")
                    .field("analyzer", "simple")
                    .field("preserve_separators", "false")
                    .endObject()

                    .endObject()
                    .endObject().endObject();

            return mapping;
        }
        catch(IOException ioe) {
            throw new TechnicalException(String.format("Error while getting mapping for index [%s/%s]: %s", INDEX_NAME, indexType, ioe.getMessage()), ioe);
        }
    }

    public String indexRecordFromJson(String recordJson, String indexType) {

        try {
            JsonNode actualObj = getObjectMapper().readTree(recordJson);
            Set<String> fieldNames = Sets.newHashSet(actualObj.fieldNames());
            if (!fieldNames.contains(Record.PROPERTY_ISSUER)
                    || !fieldNames.contains(Record.PROPERTY_SIGNATURE)) {
                throw new InvalidFormatException("Invalid record JSON format. Required fields [issuer,signature]");
            }
            String issuer = actualObj.get(Record.PROPERTY_ISSUER).asText();

            String signature = actualObj.get(Record.PROPERTY_SIGNATURE).asText();

            String recordNoSign = recordJson.replaceAll(String.format(JSON_STRING_PROPERTY_REGEX, Record.PROPERTY_SIGNATURE), "")
                    .replaceAll(String.format(JSON_STRING_PROPERTY_REGEX, Record.PROPERTY_HASH), "");

            if (!cryptoService.verify(recordNoSign, signature, issuer)) {
                throw new InvalidSignatureException("Invalid signature for JSON string: " + recordNoSign);
            }

            // TODO : check if issuer is a valid member
            //wotRemoteService.getRequirments();

            if (log.isDebugEnabled()) {
                log.debug(String.format("Indexing a record from issuer [%s]", issuer.substring(0, 8)));
            }

        }
        catch(IOException | JsonSyntaxException e) {
            throw new InvalidFormatException("Invalid record JSON: " + e.getMessage(), e);
        }

        // Preparing indexBlocksFromNode
        IndexRequestBuilder indexRequest = getClient().prepareIndex(INDEX_NAME, indexType)
                .setSource(recordJson);

        // Execute indexBlocksFromNode
        IndexResponse response = indexRequest
                .setRefresh(false)
                .execute().actionGet();

        return response.getId();
    }

}

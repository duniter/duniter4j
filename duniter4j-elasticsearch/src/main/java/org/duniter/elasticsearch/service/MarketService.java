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
import com.google.common.collect.Sets;
import com.google.gson.JsonSyntaxException;
import org.duniter.core.client.model.elasticsearch.DeleteRecord;
import org.duniter.core.client.model.elasticsearch.Record;
import org.duniter.core.client.service.bma.WotRemoteService;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.service.CryptoService;
import org.duniter.elasticsearch.PluginSettings;
import org.duniter.elasticsearch.exception.InvalidFormatException;
import org.duniter.elasticsearch.exception.InvalidSignatureException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.IOException;
import java.util.Set;

/**
 * Created by Benoit on 30/03/2015.
 */
public class MarketService extends AbstractService {

    public static final String INDEX = "market";
    public static final String RECORD_CATEGORY_TYPE = "category";
    public static final String RECORD_TYPE = "record";
    public static final String RECORD_COMMENT_TYPE = "comment";

    private static final String CATEGORIES_BULK_CLASSPATH_FILE = "market-categories-bulk-insert.json";

    private WotRemoteService wotRemoteService;

    @Inject
    public MarketService(Client client, PluginSettings settings, CryptoService cryptoService, WotRemoteService wotRemoteService) {
        super(client, settings, cryptoService);
        this.wotRemoteService = wotRemoteService;
    }

    /**
     * Delete blockchain index, and all data
     * @throws JsonProcessingException
     */
    public MarketService  deleteIndex() {
        deleteIndexIfExists(INDEX);
        return this;
    }


    public boolean existsIndex() {
        return super.existsIndex(INDEX);
    }

    /**
     * Create index need for blockchain registry, if need
     */
    public MarketService createIndexIfNotExists() {
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
    public MarketService createIndex() throws JsonProcessingException {
        logger.info(String.format("Creating index [%s/%s]", INDEX, RECORD_CATEGORY_TYPE));

        CreateIndexRequestBuilder createIndexRequestBuilder = client.admin().indices().prepareCreate(INDEX);
        Settings indexSettings = Settings.settingsBuilder()
                .put("number_of_shards", 2)
                .put("number_of_replicas", 1)
                //.put("analyzer", createDefaultAnalyzer())
                .build();
        createIndexRequestBuilder.setSettings(indexSettings);
        createIndexRequestBuilder.addMapping(RECORD_CATEGORY_TYPE, createRecordCategoryType());
        createIndexRequestBuilder.addMapping(RECORD_TYPE, createRecordType());
        createIndexRequestBuilder.addMapping(RECORD_COMMENT_TYPE, createRecordCommentType());
        createIndexRequestBuilder.execute().actionGet();

        return this;
    }

    /**
     *
     * @param jsonCategory
     * @return the product id
     */
    public String indexCategoryFromJson(String jsonCategory) {
        if (logger.isDebugEnabled()) {
            logger.debug("Indexing a category");
        }

        // Preparing indexBlocksFromNode
        IndexRequestBuilder indexRequest = client.prepareIndex(INDEX, RECORD_CATEGORY_TYPE)
                .setSource(jsonCategory);

        // Execute indexBlocksFromNode
        IndexResponse response = indexRequest
                .setRefresh(false)
                .execute().actionGet();

        return response.getId();
    }

    public String indexRecordFromJson(String recordJson) {

        JsonNode actualObj = readAndVerifyIssuerSignature(recordJson);
        String issuer = getIssuer(actualObj);

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Indexing market record from issuer [%s]", issuer.substring(0, 8)));
        }

        IndexResponse response = client.prepareIndex(INDEX, RECORD_TYPE)
                .setSource(recordJson)
                .setRefresh(false)
                .execute().actionGet();

        return response.getId();
    }

    public void updateRecordFromJson(String recordJson, String id) {

        JsonNode actualObj = readAndVerifyIssuerSignature(recordJson);
        String issuer = getIssuer(actualObj);

        // Check same document issuer
        checkSameDocumentIssuer(INDEX, RECORD_TYPE, id, issuer);

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Updating market record [%s] from issuer [%s]", id, issuer.substring(0, 8)));
        }

        client.prepareUpdate(INDEX, RECORD_TYPE, id)
                .setDoc(recordJson)
                .execute().actionGet();
    }

    public String indexCommentFromJson(String commentJson) {

        JsonNode actualObj = readAndVerifyIssuerSignature(commentJson);
        String issuer = getIssuer(actualObj);

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Indexing a comment from issuer [%s]", issuer.substring(0, 8)));
        }

        IndexResponse response = client.prepareIndex(INDEX, RECORD_COMMENT_TYPE)
                .setSource(commentJson)
                .setRefresh(false)
                .execute().actionGet();
        return response.getId();
    }

    public void updateCommentFromJson(String commentJson, String id) {

        JsonNode actualObj = readAndVerifyIssuerSignature(commentJson);
        String issuer = getIssuer(actualObj);

        // Check same document issuer
        checkSameDocumentIssuer(INDEX, RECORD_COMMENT_TYPE, id, issuer);

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Updating comment [%s] from issuer [%s]", id, issuer.substring(0, 8)));
        }

        // Execute indexBlocksFromNode
        client.prepareUpdate(INDEX, RECORD_COMMENT_TYPE, id)
                .execute().actionGet();
    }

    public void fillRecordCategories() {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("[%s/%s] Fill data", INDEX, RECORD_CATEGORY_TYPE));
        }

        // Insert categories
        bulkFromClasspathFile(CATEGORIES_BULK_CLASSPATH_FILE, INDEX, RECORD_CATEGORY_TYPE);
    }

    /* -- Internal methods -- */


    public XContentBuilder createRecordCategoryType() {
        try {
            XContentBuilder mapping = XContentFactory.jsonBuilder().startObject().startObject(RECORD_CATEGORY_TYPE)
                    .startObject("properties")

                    // name
                    .startObject("name")
                    .field("type", "string")
                    .endObject()

                    // description
                    /*.startObject("description")
                    .field("type", "string")
                    .endObject()*/

                    // parent
                    .startObject("parent")
                    .field("type", "string")
                    .endObject()

                    // tags
                    /*.startObject("tags")
                    .field("type", "completion")
                    .field("search_analyzer", "simple")
                    .field("analyzer", "simple")
                    .field("preserve_separators", "false")
                    .endObject()*/

                    .endObject()
                    .endObject().endObject();

            return mapping;
        }
        catch(IOException ioe) {
            throw new TechnicalException(String.format("Error while getting mapping for index [%s/%s]: %s", INDEX, RECORD_CATEGORY_TYPE, ioe.getMessage()), ioe);
        }
    }

    public XContentBuilder createRecordType() {
        String stringAnalyzer = pluginSettings.getDefaultStringAnalyzer();

        try {
            XContentBuilder mapping = XContentFactory.jsonBuilder().startObject().startObject(RECORD_TYPE)
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

                    // price Unit
                    .startObject("unit")
                    .field("type", "string")
                    .endObject()

                    // currency
                    .startObject("currency")
                    .field("type", "string")
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

                    // thumbnail
                    .startObject("thumbnail")
                    .field("type", "attachment")
                        .startObject("fields") // src
                        .startObject("content") // title
                            .field("index", "no")
                        .endObject()
                        .startObject("title") // title
                            .field("type", "string")
                            .field("store", "no")
                        .endObject()
                        .startObject("author") // title
                            .field("store", "no")
                        .endObject()
                        .startObject("content_type") // title
                            .field("store", "yes")
                        .endObject()
                    .endObject()
                    .endObject()

                    // pictures
                    .startObject("pictures")
                    .field("type", "nested")
                    .field("dynamic", "false")
                        .startObject("properties")
                            .startObject("file") // file
                                .field("type", "attachment")
                                .startObject("fields")
                                    .startObject("content") // content
                                        .field("index", "no")
                                    .endObject()
                                    .startObject("title") // title
                                        .field("type", "string")
                                        .field("store", "yes")
                                        .field("analyzer", stringAnalyzer)
                                    .endObject()
                                    .startObject("author") // author
                                        .field("type", "string")
                                        .field("store", "no")
                                    .endObject()
                                    .startObject("content_type") // content_type
                                        .field("store", "yes")
                                    .endObject()
                                .endObject()
                            .endObject()
                        .endObject()
                    .endObject()

                    // picturesCount
                    .startObject("picturesCount")
                    .field("type", "integer")
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
            throw new TechnicalException(String.format("Error while getting mapping for index [%s/%s]: %s", INDEX, RECORD_TYPE, ioe.getMessage()), ioe);
        }
    }

    public XContentBuilder createRecordCommentType() {
        String stringAnalyzer = pluginSettings.getDefaultStringAnalyzer();

        try {
            XContentBuilder mapping = XContentFactory.jsonBuilder().startObject().startObject(RECORD_COMMENT_TYPE)
                    .startObject("properties")

                    // issuer
                    .startObject("issuer")
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()

                    // time
                    .startObject("time")
                    .field("type", "integer")
                    .endObject()

                    // message
                    .startObject("message")
                    .field("type", "string")
                    .field("analyzer", stringAnalyzer)
                    .endObject()

                    // record
                    .startObject("record")
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()

                    // reply to
                    .startObject("reply_to")
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()

                    .endObject()
                    .endObject().endObject();

            return mapping;
        }
        catch(IOException ioe) {
            throw new TechnicalException(String.format("Error while getting mapping for index [%s/%s]: %s", INDEX, RECORD_COMMENT_TYPE, ioe.getMessage()), ioe);
        }
    }

}
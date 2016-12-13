package org.duniter.elasticsearch.gchange.service;

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
import org.apache.commons.collections4.MapUtils;
import org.duniter.core.client.model.elasticsearch.RecordComment;
import org.duniter.core.client.service.bma.WotRemoteService;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.service.CryptoService;
import org.duniter.elasticsearch.exception.DocumentNotFoundException;
import org.duniter.elasticsearch.gchange.PluginSettings;
import org.duniter.elasticsearch.gchange.model.MarketRecord;
import org.duniter.elasticsearch.gchange.model.event.GchangeEventCodes;
import org.duniter.elasticsearch.service.AbstractService;
import org.duniter.elasticsearch.user.model.UserEvent;
import org.duniter.elasticsearch.user.service.UserService;
import org.duniter.elasticsearch.user.service.UserEventService;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
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
public class MarketService extends AbstractService {

    public static final String INDEX = "market";
    public static final String RECORD_CATEGORY_TYPE = "category";
    public static final String RECORD_TYPE = "record";
    public static final String RECORD_COMMENT_TYPE = "comment";

    private static final String CATEGORIES_BULK_CLASSPATH_FILE = "market-categories-bulk-insert.json";

    private WotRemoteService wotRemoteService;
    private UserEventService userEventService;
    private CommentService commentService;

    @Inject
    public MarketService(Client client, PluginSettings settings,
                         CryptoService cryptoService,
                         CommentService commentService,
                         WotRemoteService wotRemoteService,
                         UserEventService userEventService) {
        super("gchange." + INDEX, client, settings, cryptoService);
        this.commentService = commentService;
        this.wotRemoteService = wotRemoteService;
        this.userEventService = userEventService;
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

                // Fill categories
                fillRecordCategories();
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
        logger.info(String.format("Creating index [%s]", INDEX));

        CreateIndexRequestBuilder createIndexRequestBuilder = client.admin().indices().prepareCreate(INDEX);
        Settings indexSettings = Settings.settingsBuilder()
                .put("number_of_shards", 2)
                .put("number_of_replicas", 1)
                //.put("analyzer", createDefaultAnalyzer())
                .build();
        createIndexRequestBuilder.setSettings(indexSettings);
        createIndexRequestBuilder.addMapping(RECORD_CATEGORY_TYPE, createRecordCategoryType());
        createIndexRequestBuilder.addMapping(RECORD_TYPE, createRecordType());
        createIndexRequestBuilder.addMapping(RECORD_COMMENT_TYPE, commentService.createRecordCommentType(INDEX, RECORD_COMMENT_TYPE));
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

    public String indexRecordFromJson(String json) {
        return checkIssuerAndIndexDocumentFromJson(INDEX, RECORD_TYPE, json);
    }

    public void updateRecordFromJson(String json, String id) {
        checkIssuerAndUpdateDocumentFromJson(INDEX, RECORD_TYPE, json, id);
    }

    public String indexCommentFromJson(String json) {
        return commentService.indexCommentFromJson(INDEX, RECORD_TYPE, RECORD_COMMENT_TYPE, json);
    }

    public void updateCommentFromJson(String json, String id) {
        commentService.updateCommentFromJson(INDEX, RECORD_TYPE, RECORD_COMMENT_TYPE, json, id);
    }

    public MarketService fillRecordCategories() {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("[%s/%s] Fill data", INDEX, RECORD_CATEGORY_TYPE));
        }

        // Insert categories
        bulkFromClasspathFile(CATEGORIES_BULK_CLASSPATH_FILE, INDEX, RECORD_CATEGORY_TYPE,
                // Add order attribute (auto incremented)
                new AddSequenceAttributeHandler("order", "\\{.*\"name\".*\\}", 1));

        return this;
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

                    // order
                    .startObject("order")
                    .field("type", "integer")
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

                    // creationTime
                    .startObject("creationTime")
                    .field("type", "integer")
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
                    .field("index", "not_analyzed")
                    .endObject()

                    // currency
                    .startObject("currency")
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()

                    // issuer
                    .startObject("issuer")
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()

                    // type (offer, need, ...)
                    .startObject("type")
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()

                    // location
                    .startObject("location")
                    .field("type", "string")
                    .field("analyzer", stringAnalyzer)
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

                    // category
                    .startObject("category")
                    .field("type", "nested")
                    .field("dynamic", "false")
                    .startObject("properties")
                    .startObject("id") // author
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()
                    .startObject("parent") // author
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()
                    .startObject("name") // author
                    .field("type", "string")
                    .field("analyzer", stringAnalyzer)
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

}

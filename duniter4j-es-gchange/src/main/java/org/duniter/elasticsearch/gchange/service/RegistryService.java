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
import org.duniter.core.client.service.bma.BlockchainRemoteService;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.service.CryptoService;
import org.duniter.elasticsearch.gchange.PluginSettings;
import org.duniter.elasticsearch.service.AbstractService;
import org.duniter.elasticsearch.user.service.UserEventService;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.IOException;

/**
 * Created by Benoit on 30/03/2015.
 */
public class RegistryService extends AbstractService {

    public static final String INDEX = "registry";
    public static final String RECORD_TYPE = "record";
    public static final String RECORD_CATEGORY_TYPE = "category";
    public static final String RECORD_COMMENT_TYPE = "comment";
    private static final String CATEGORIES_BULK_CLASSPATH_FILE = "registry-categories-bulk-insert.json";

    private CommentService commentService;
    private UserEventService userEventService;

    @Inject
    public RegistryService(Client client,
                           PluginSettings settings,
                           CryptoService cryptoService,
                           CommentService commentService,
                            UserEventService userEventService) {
        super("gchange." + INDEX, client, settings, cryptoService);
        this.commentService = commentService;
        this.userEventService = userEventService;
    }

    /**
     * Create index need for blockchain registry, if need
     */
    public RegistryService createIndexIfNotExists() {
        try {
            if (!existsIndex(INDEX)) {
                createIndex();

                fillRecordCategories();
            }
        }
        catch(JsonProcessingException e) {
            throw new TechnicalException(String.format("Error while creating index [%s]", INDEX));
        }
        return this;
    }

    /**
     * Create index for registry
     * @throws JsonProcessingException
     */
    public RegistryService createIndex() throws JsonProcessingException {
        logger.info(String.format("Creating index [%s]", INDEX));

        CreateIndexRequestBuilder createIndexRequestBuilder = client.admin().indices().prepareCreate(INDEX);
        org.elasticsearch.common.settings.Settings indexSettings = org.elasticsearch.common.settings.Settings.settingsBuilder()
                .put("number_of_shards", 3)
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

    public RegistryService deleteIndex() {
        deleteIndexIfExists(INDEX);
        return this;
    }

    public boolean existsIndex() {
        return super.existsIndex(INDEX);
    }

    public RegistryService fillRecordCategories() {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("[%s/%s] Fill data", INDEX, RECORD_CATEGORY_TYPE));
        }

        // Insert categories
        bulkFromClasspathFile(CATEGORIES_BULK_CLASSPATH_FILE, INDEX, RECORD_CATEGORY_TYPE,
                // Add order attribute
                new AddSequenceAttributeHandler("order", "\\{.*\"name\".*\\}", 1));

        return this;
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

    /* -- Internal methods -- */

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

                    // issuer
                    .startObject("issuer")
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()

                    // pubkey
                    .startObject("pubkey")
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()

                    // address
                    .startObject("address")
                    .field("type", "string")
                    .field("analyzer", stringAnalyzer)
                    .endObject()

                    // city
                    .startObject("city")
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

                    // category
                    .startObject("category")
                    .field("type", "nested")
                    .field("dynamic", "false")
                    .startObject("properties")
                    .startObject("id") // id
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()
                    .startObject("parent") // parent
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()
                    .startObject("name") // name
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

    public XContentBuilder createRecordCategoryType() {
        try {
            XContentBuilder mapping = XContentFactory.jsonBuilder().startObject().startObject(RECORD_CATEGORY_TYPE)
                    .startObject("properties")

                    // name
                    .startObject("name")
                    .field("type", "string")
                    .field("analyzer", pluginSettings.getDefaultStringAnalyzer())
                    .endObject()

                    // description
                    /*.startObject("description")
                    .field("type", "string")
                    .endObject()*/

                    // parent
                    .startObject("parent")
                    .field("type", "string")
                    .field("index", "not_analyzed")
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

}

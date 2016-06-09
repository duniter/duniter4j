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
import org.duniter.core.exception.TechnicalException;
import org.duniter.elasticsearch.service.BaseIndexerService;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * Created by Benoit on 30/03/2015.
 */
public class MarketCategoryIndexerService extends BaseIndexerService {

    private static final Logger log = LoggerFactory.getLogger(MarketCategoryIndexerService.class);
    private static final String CATEGORIES_BULK_CLASSPATH_FILE = "market-categories-bulk-insert.json";


    public static final String INDEX_NAME = "market";
    public static final String INDEX_TYPE = "category";

    /**
     * Delete currency index, and all data
     * @throws JsonProcessingException
     */
    public void deleteIndex() throws JsonProcessingException {
        deleteIndexIfExists(INDEX_NAME);
    }


    public boolean existsIndex() {
        return super.existsIndex(INDEX_NAME);
    }

    /**
     * Create index need for currency registry, if need
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
     * Create index need for category registry
     * @throws JsonProcessingException
     */
    public void createIndex() throws JsonProcessingException {
        log.info(String.format("Creating index [%s/%s]", INDEX_NAME, INDEX_TYPE));

        CreateIndexRequestBuilder createIndexRequestBuilder = getClient().admin().indices().prepareCreate(INDEX_NAME);
        Settings indexSettings = Settings.settingsBuilder()
                .put("number_of_shards", 1)
                .put("number_of_replicas", 1)
                //.put("analyzer", createDefaultAnalyzer())
                .build();
        createIndexRequestBuilder.setSettings(indexSettings);
        createIndexRequestBuilder.addMapping(INDEX_TYPE, createIndexMapping());
        createIndexRequestBuilder.execute().actionGet();
    }

    /**
     *
     * @param jsonCategory
     * @return the product id
     */
    public String indexCategoryFromJson(String jsonCategory) {
        if (log.isDebugEnabled()) {
            log.debug("Indexing a category");
        }

        // Preparing indexBlocksFromNode
        IndexRequestBuilder indexRequest = getClient().prepareIndex(INDEX_NAME, INDEX_TYPE)
                .setSource(jsonCategory);

        // Execute indexBlocksFromNode
        IndexResponse response = indexRequest
                .setRefresh(false)
                .execute().actionGet();

        return response.getId();
    }


    public void initCategories() {
        if (log.isDebugEnabled()) {
            log.debug("Initializing all market categories");
        }

        // Insert categories
        bulkFromClasspathFile(CATEGORIES_BULK_CLASSPATH_FILE, INDEX_NAME, INDEX_TYPE);
    }

    /* -- Internal methods -- */


    public XContentBuilder createIndexMapping() {
        try {
            XContentBuilder mapping = XContentFactory.jsonBuilder().startObject().startObject(INDEX_TYPE)
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
            throw new TechnicalException(String.format("Error while getting mapping for index [%s/%s]: %s", INDEX_NAME, INDEX_TYPE, ioe.getMessage()), ioe);
        }
    }

}

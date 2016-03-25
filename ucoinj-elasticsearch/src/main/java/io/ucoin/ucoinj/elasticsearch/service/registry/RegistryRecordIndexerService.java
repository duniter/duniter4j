package io.ucoin.ucoinj.elasticsearch.service.registry;

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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import io.ucoin.ucoinj.core.client.model.bma.gson.GsonUtils;
import io.ucoin.ucoinj.core.client.model.elasticsearch.Record;
import io.ucoin.ucoinj.core.client.service.bma.WotRemoteService;
import io.ucoin.ucoinj.core.exception.TechnicalException;
import io.ucoin.ucoinj.core.service.CryptoService;
import io.ucoin.ucoinj.elasticsearch.config.Configuration;
import io.ucoin.ucoinj.elasticsearch.service.BaseIndexerService;
import io.ucoin.ucoinj.elasticsearch.service.ServiceLocator;
import io.ucoin.ucoinj.elasticsearch.service.exception.InvalidFormatException;
import io.ucoin.ucoinj.elasticsearch.service.exception.InvalidSignatureException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * Created by Benoit on 30/03/2015.
 */
public class RegistryRecordIndexerService extends BaseIndexerService {

    private static final Logger log = LoggerFactory.getLogger(RegistryRecordIndexerService.class);

    private static final String JSON_STRING_PROPERTY_REGEX = "[,]?[\"\\s\\n\\r]*%s[\"]?[\\s\\n\\r]*:[\\s\\n\\r]*\"[^\"]+\"";

    public static final String INDEX_NAME = "registry";
    public static final String INDEX_TYPE = "record";

    private Gson gson;

    private Configuration config;

    private WotRemoteService wotRemoteService;

    private CryptoService cryptoService;

    public RegistryRecordIndexerService() {
        gson = GsonUtils.newBuilder().create();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        wotRemoteService = ServiceLocator.instance().getWotRemoteService();
        cryptoService = ServiceLocator.instance().getCryptoService();
        config = Configuration.instance();
    }

    @Override
    public void close() throws IOException {
        super.close();
        wotRemoteService = null;
        config = null;
        gson = null;
    }

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
     * Create index need for record registry
     * @throws JsonProcessingException
     */
    public void createIndex() throws JsonProcessingException {
        log.info(String.format("Creating index [%s/%s]", INDEX_NAME, INDEX_TYPE));

        CreateIndexRequestBuilder createIndexRequestBuilder = getClient().admin().indices().prepareCreate(INDEX_NAME);
        Settings indexSettings = Settings.settingsBuilder()
                .put("number_of_shards", 1)
                .put("number_of_replicas", 1)
                .put("analyzer", createDefaultAnalyzer())
                .build();
        createIndexRequestBuilder.setSettings(indexSettings);
        createIndexRequestBuilder.addMapping(INDEX_TYPE, createIndexMapping());
        createIndexRequestBuilder.execute().actionGet();
    }

    /**
     *
     * @param recordJson
     * @return the record id
     */
    public String indexRecordFromJson(String recordJson) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode actualObj = mapper.readTree(recordJson);
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

            if (log.isDebugEnabled()) {
                log.debug(String.format("Indexing a record from issuer [%s]", issuer.substring(0, 8)));
            }

        }
        catch(IOException | JsonSyntaxException e) {
            throw new InvalidFormatException("Invalid record JSON: " + e.getMessage(), e);
        }

        // Preparing indexBlocksFromNode
        IndexRequestBuilder indexRequest = getClient().prepareIndex(INDEX_NAME, INDEX_TYPE)
                .setSource(recordJson);

        // Execute indexBlocksFromNode
        IndexResponse response = indexRequest
                .setRefresh(false)
                .execute().actionGet();

        return response.getId();
    }


    public void insertRecordFromBulkFile(File bulkFile) {

        if (log.isDebugEnabled()) {
            log.debug("Inserting records from file");
        }

        // Insert cities
        bulkFromFile(bulkFile, INDEX_NAME, INDEX_TYPE);
    }

    /* -- Internal methods -- */


    public XContentBuilder createIndexMapping() {
        try {
            XContentBuilder mapping = XContentFactory.jsonBuilder().startObject().startObject(INDEX_TYPE)
                    .startObject("properties")

                    // title
                    .startObject("title")
                    .field("type", "string")
                    .endObject()

                    // description
                    .startObject("description")
                    .field("type", "string")
                    .endObject()

                    // time
                    .startObject("time")
                    .field("type", "integer")
                    .endObject()

                    // issuer
                    .startObject("issuer")
                    .field("type", "string")
                    .endObject()

                    // issuer
                    .startObject("location")
                    .field("type", "geo_point")
                    .endObject()

                    // categories
                    .startObject("categories")
                    .field("type", "nested")
                    .startObject("properties")
                    .startObject("cat1") // cat1
                    .field("type", "string")
                    .endObject()
                    .startObject("cat2") // cat2
                    .field("type", "string")
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
            throw new TechnicalException(String.format("Error while getting mapping for index [%s/%s]: %s", INDEX_NAME, INDEX_TYPE, ioe.getMessage()), ioe);
        }
    }

}

package org.duniter.elasticsearch.service.registry;

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
import org.duniter.core.client.model.bma.gson.GsonUtils;
import org.duniter.core.client.model.elasticsearch.Record;
import org.duniter.core.client.service.ServiceLocator;
import org.duniter.core.client.service.bma.WotRemoteService;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.service.CryptoService;
import org.duniter.core.util.StringUtils;
import org.duniter.elasticsearch.PluginSettings;
import org.duniter.elasticsearch.service.AbstractService;
import org.duniter.elasticsearch.service.exception.InvalidFormatException;
import org.duniter.elasticsearch.service.exception.InvalidSignatureException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * Created by Benoit on 30/03/2015.
 */
public class RecordRegistryService extends AbstractService<RecordRegistryService> {

    private static final ESLogger log = ESLoggerFactory.getLogger(RecordRegistryService.class.getName());

    private static final String JSON_STRING_PROPERTY_REGEX = "[,]?[\"\\s\\n\\r]*%s[\"]?[\\s\\n\\r]*:[\\s\\n\\r]*\"[^\"]+\"";

    public static final String INDEX_NAME = "registry";
    public static final String INDEX_TYPE = "record";

    private Gson gson;

    private WotRemoteService wotRemoteService;

    private CryptoService cryptoService;

    @Inject
    public RecordRegistryService(Client client, PluginSettings settings) {
        super(client, settings);
        gson = GsonUtils.newBuilder().create();
    }

    @Override
    public RecordRegistryService start() {
        wotRemoteService = ServiceLocator.instance().getWotRemoteService();
        cryptoService = ServiceLocator.instance().getCryptoService();
        return super.start();
    }

    @Override
    public void close(){
        wotRemoteService = null;
        gson = null;
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
        log.info(String.format("Creating index [%s/%s]", INDEX_NAME, INDEX_TYPE));

        CreateIndexRequestBuilder createIndexRequestBuilder = getClient().admin().indices().prepareCreate(INDEX_NAME);
        org.elasticsearch.common.settings.Settings indexSettings = org.elasticsearch.common.settings.Settings.settingsBuilder()
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

            // TODO verify hash
            //if (!cryptoService.verifyHash(recordNoSign, signature, issuer)) {
            //    throw new InvalidSignatureException("Invalid signature for JSON string: " + recordNoSign);
            //}

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
        String stringAnalyzer = getPluginSettings().getIndexStringAnalyzer();
        if (StringUtils.isBlank(stringAnalyzer)) {
            stringAnalyzer = "english";
        }

        try {
            XContentBuilder mapping = XContentFactory.jsonBuilder().startObject().startObject(INDEX_TYPE)
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

                    // avatar
                    .startObject("avatar")
                    .field("type", "string")
                    .field("index", "not_analyzed")
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
            throw new TechnicalException(String.format("Error while getting mapping for index [%s/%s]: %s", INDEX_NAME, INDEX_TYPE, ioe.getMessage()), ioe);
        }
    }

}

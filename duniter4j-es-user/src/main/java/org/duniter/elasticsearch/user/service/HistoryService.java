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
import org.duniter.core.client.model.elasticsearch.DeleteRecord;
import org.duniter.core.client.model.elasticsearch.MessageRecord;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.service.CryptoService;
import org.duniter.elasticsearch.client.Duniter4jClient;
import org.duniter.elasticsearch.exception.NotFoundException;
import org.duniter.elasticsearch.user.service.AbstractService;
import org.duniter.elasticsearch.user.PluginSettings;
import org.duniter.elasticsearch.user.model.UserEvent;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import java.io.IOException;

/**
 * Created by Benoit on 30/03/2015.
 */
public class HistoryService extends AbstractService {

    public static final String INDEX = "history";
    public static final String DELETE_TYPE = "delete";

    @Inject
    public HistoryService(Duniter4jClient client, PluginSettings settings, CryptoService cryptoService) {
        super("gchange." + INDEX, client, settings, cryptoService);
    }

    /**
     * Delete blockchain index, and all data
     * @throws JsonProcessingException
     */
    public HistoryService deleteIndex() {
        client.deleteIndexIfExists(INDEX);
        return this;
    }


    public boolean existsIndex() {
        return client.existsIndex(INDEX);
    }

    /**
     * Create index need for blockchain registry, if need
     */
    public HistoryService createIndexIfNotExists() {
        try {
            if (!client.existsIndex(INDEX)) {
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
    public HistoryService createIndex() throws JsonProcessingException {
        logger.info(String.format("Creating index [%s/%s]", INDEX, DELETE_TYPE));

        CreateIndexRequestBuilder createIndexRequestBuilder = client.admin().indices().prepareCreate(INDEX);
        Settings indexSettings = Settings.settingsBuilder()
                .put("number_of_shards", 2)
                .put("number_of_replicas", 1)
                //.put("analyzer", createDefaultAnalyzer())
                .build();
        createIndexRequestBuilder.setSettings(indexSettings);
        createIndexRequestBuilder.addMapping(DELETE_TYPE, createDeleteType());
        createIndexRequestBuilder.execute().actionGet();

        return this;
    }


    public String indexDeleteFromJson(String recordJson) {
        JsonNode actualObj = readAndVerifyIssuerSignature(recordJson);
        String issuer = actualObj.get(DeleteRecord.PROPERTY_ISSUER).asText();

        String index = actualObj.get(DeleteRecord.PROPERTY_INDEX).asText();
        String type = actualObj.get(DeleteRecord.PROPERTY_TYPE).asText();
        String id = actualObj.get(DeleteRecord.PROPERTY_ID).asText();

        if (!client.existsIndex(index)) {
            throw new NotFoundException(String.format("Index [%s] not exists.", index));
        }

        // Special case for message: check if deletion issuer is the message recipient
        if (MessageService.INDEX.equals(index) && MessageService.INBOX_TYPE.equals(type)) {
            client.checkSameDocumentField(index, type, id, MessageRecord.PROPERTY_RECIPIENT, issuer);
        }
        // Special case for invitation: check if deletion issuer is the invitation recipient
        else if (UserInvitationService.INDEX.equals(index)) {
            client.checkSameDocumentField(index, type, id, MessageRecord.PROPERTY_RECIPIENT, issuer);
        }
        else {
            // Check document issuer
            client.checkSameDocumentIssuer(index, type, id, issuer);
        }

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Deleting document [%s/%s/%s] - issuer [%s]", index, type, id, issuer.substring(0, 8)));
        }

        // Add deletion to history
        IndexResponse response = client.prepareIndex(INDEX, DELETE_TYPE)
                .setSource(recordJson)
                .setRefresh(false)
                .execute().actionGet();

        // Delete the document
        client.prepareDelete(index, type, id).execute().actionGet();

        return response.getId();
    }

    public boolean existsInDeleteHistory(final String index, final String type, final String id) {
        // Prepare search request
        SearchRequestBuilder searchRequest = client
                .prepareSearch(INDEX)
                .setTypes(DELETE_TYPE)
                .setFetchSource(false)
                .setSearchType(SearchType.QUERY_AND_FETCH);

        // Query = filter on index/type/id
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termQuery(DeleteRecord.PROPERTY_INDEX, index))
                .filter(QueryBuilders.termQuery(DeleteRecord.PROPERTY_TYPE, type))
                .filter(QueryBuilders.termQuery(DeleteRecord.PROPERTY_ID, id));

        searchRequest.setQuery(QueryBuilders.nestedQuery(UserEvent.PROPERTY_REFERENCE, QueryBuilders.constantScoreQuery(boolQuery)));

        // Execute query
        SearchResponse response = searchRequest.execute().actionGet();
        return response.getHits().getTotalHits() > 0;
    }

    /* -- Internal methods -- */


    protected XContentBuilder createDeleteType() {
        try {
            XContentBuilder mapping = XContentFactory.jsonBuilder().startObject().startObject(DELETE_TYPE)
                    .startObject("properties")

                    // index
                    .startObject("index")
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()

                    // type
                    .startObject("type")
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()

                    // id
                    .startObject("id")
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()

                    // time
                    .startObject("time")
                    .field("type", "integer")
                    .endObject()

                    .endObject()
                    .endObject().endObject();

            return mapping;
        }
        catch(IOException ioe) {
            throw new TechnicalException(String.format("Error while getting mapping for index [%s/%s]: %s", INDEX, DELETE_TYPE, ioe.getMessage()), ioe);
        }
    }

    public XContentBuilder createRecordCommentType() {
        String stringAnalyzer = pluginSettings.getDefaultStringAnalyzer();

        try {
            XContentBuilder mapping = XContentFactory.jsonBuilder().startObject().startObject(DELETE_TYPE)
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
            throw new TechnicalException(String.format("Error while getting mapping for index [%s/%s]: %s", INDEX, DELETE_TYPE, ioe.getMessage()), ioe);
        }
    }

}

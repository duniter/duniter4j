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
import com.google.common.base.Preconditions;
import org.apache.commons.collections4.MapUtils;
import org.duniter.core.client.model.ModelUtils;
import org.duniter.core.client.model.elasticsearch.Record;
import org.duniter.core.client.model.elasticsearch.UserGroup;
import org.duniter.core.client.model.elasticsearch.UserProfile;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.service.CryptoService;
import org.duniter.elasticsearch.PluginSettings;
import org.duniter.elasticsearch.exception.AccessDeniedException;
import org.duniter.elasticsearch.service.AbstractService;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Created by Benoit on 30/03/2015.
 */
public class GroupService extends AbstractService {

    public static final String INDEX = "group";
    public static final String RECORD_TYPE = "record";

    @Inject
    public GroupService(Client client,
                        PluginSettings settings,
                        CryptoService cryptoService) {
        super("duniter." + INDEX, client, settings,cryptoService);
    }

    /**
     * Create index need for blockchain registry, if need
     */
    public GroupService createIndexIfNotExists() {
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
     * Create index for registry
     * @throws JsonProcessingException
     */
    public GroupService createIndex() throws JsonProcessingException {
        logger.info(String.format("Creating index [%s]", INDEX));

        CreateIndexRequestBuilder createIndexRequestBuilder = client.admin().indices().prepareCreate(INDEX);
        org.elasticsearch.common.settings.Settings indexSettings = org.elasticsearch.common.settings.Settings.settingsBuilder()
                .put("number_of_shards", 3)
                .put("number_of_replicas", 1)
                //.put("analyzer", createDefaultAnalyzer())
                .build();
        createIndexRequestBuilder.setSettings(indexSettings);
        createIndexRequestBuilder.addMapping(RECORD_TYPE, createRecordType());
        createIndexRequestBuilder.execute().actionGet();

        return this;
    }

    public GroupService deleteIndex() {
        deleteIndexIfExists(INDEX);
        return this;
    }

    public boolean existsIndex() {
        return super.existsIndex(INDEX);
    }

    /**
     *
     * Index an record
     * @param profileJson
     * @return the record id
     */
    public String indexRecordProfileFromJson(String profileJson) {

        JsonNode actualObj = readAndVerifyIssuerSignature(profileJson);
        String name = getName(actualObj);

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Indexing a user profile from issuer [%s]", name.substring(0, 8)));
        }

        IndexResponse response = client.prepareIndex(INDEX, RECORD_TYPE)
                .setSource(profileJson)
                .setId(name) // always use the name as id
                .setRefresh(false)
                .execute().actionGet();
        return response.getId();
    }

    /**
     * Update a record
     * @param recordJson
     */
    public ListenableActionFuture<UpdateResponse> updateRecordFromJson(String recordJson, String id) {

        JsonNode actualObj = readAndVerifyIssuerSignature(recordJson);
        String name = getName(actualObj);

        if (!Objects.equals(name, id)) {
            throw new AccessDeniedException(String.format("Could not update this document: not issuer."));
        }
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Updating a group from name [%s]", name));
        }

        return client.prepareUpdate(INDEX, RECORD_TYPE, name)
                .setDoc(recordJson)
                .execute();
    }



    protected String getName(JsonNode actualObj) {
        return  getMandatoryField(actualObj, UserGroup.PROPERTY_NAME).asText();
    }

    public String getTitleByName(String name) {

        Object title = getFieldById(INDEX, RECORD_TYPE, name, UserGroup.PROPERTY_NAME);
        if (title == null) return null;
        return title.toString();
    }

    public Map<String, String> getTitlesByNames(Set<String> names) {

        Map<String, Object> titles = getFieldByIds(INDEX, RECORD_TYPE, names, UserGroup.PROPERTY_NAME);
        if (MapUtils.isEmpty(titles)) return null;
        Map<String, String> result = new HashMap<>();
        titles.entrySet().stream().forEach((entry) -> result.put(entry.getKey(), entry.getValue().toString()));
        return result;
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

                    // avatar
                    .startObject("avatar")
                        .field("type", "attachment")
                        .startObject("fields") // fields
                            .startObject("content") // content
                                .field("index", "no")
                            .endObject()
                            .startObject("title") // title
                                .field("type", "string")
                                .field("store", "no")
                            .endObject()
                                .startObject("author") // author
                                .field("store", "no")
                            .endObject()
                            .startObject("content_type") // content_type
                                .field("store", "yes")
                            .endObject()
                        .endObject()
                    .endObject()

                    // social networks
                    .startObject("socials")
                        .field("type", "nested")
                        .field("dynamic", "false")
                        .startObject("properties")
                            .startObject("type") // type
                                .field("type", "string")
                                .field("index", "not_analyzed")
                            .endObject()
                            .startObject("url") // url
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

}

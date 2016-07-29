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
import org.duniter.core.client.service.bma.BlockchainRemoteService;
import org.duniter.core.client.service.bma.WotRemoteService;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.service.CryptoService;
import org.duniter.elasticsearch.PluginSettings;
import org.duniter.elasticsearch.exception.AccessDeniedException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.IOException;
import java.util.Objects;

/**
 * Created by Benoit on 30/03/2015.
 */
public class UserService extends AbstractService {

    public static final String INDEX = "user";
    public static final String PROFILE_TYPE = "profile";
    private static final String JSON_STRING_PROPERTY_REGEX = "[,]?[\"\\s\\n\\r]*%s[\"]?[\\s\\n\\r]*:[\\s\\n\\r]*\"[^\"]+\"";

    @Inject
    public UserService(Client client,
                       PluginSettings settings,
                       CryptoService cryptoService) {
        super(client, settings,cryptoService);
    }

    /**
     * Create index need for blockchain registry, if need
     */
    public UserService createIndexIfNotExists() {
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
    public UserService createIndex() throws JsonProcessingException {
        logger.info(String.format("Creating index [%s]", INDEX));

        CreateIndexRequestBuilder createIndexRequestBuilder = client.admin().indices().prepareCreate(INDEX);
        org.elasticsearch.common.settings.Settings indexSettings = org.elasticsearch.common.settings.Settings.settingsBuilder()
                .put("number_of_shards", 3)
                .put("number_of_replicas", 1)
                //.put("analyzer", createDefaultAnalyzer())
                .build();
        createIndexRequestBuilder.setSettings(indexSettings);
        createIndexRequestBuilder.addMapping(PROFILE_TYPE, createProfileType());
        createIndexRequestBuilder.execute().actionGet();

        return this;
    }

    public UserService deleteIndex() {
        deleteIndexIfExists(INDEX);
        return this;
    }

    public boolean existsIndex() {
        return super.existsIndex(INDEX);
    }

    /**
     *
     * Index an user profile
     * @param profileJson
     * @return the profile id
     */
    public String indexProfileFromJson(String profileJson) {

        JsonNode actualObj = readAndVerifyIssuerSignature(profileJson);
        String issuer = getIssuer(actualObj);

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Indexing a user profile from issuer [%s]", issuer.substring(0, 8)));
        }

        IndexResponse response = client.prepareIndex(INDEX, PROFILE_TYPE)
                .setSource(profileJson)
                .setRefresh(false)
                .execute().actionGet();
        return response.getId();
    }

    /**
     * Update an user profile
     * @param profileJson
     */
    public void updateProfileFromJson(String profileJson, String id) {

        JsonNode actualObj = readAndVerifyIssuerSignature(profileJson);
        String issuer = getIssuer(actualObj);

        if (!Objects.equals(issuer, id)) {
            throw new AccessDeniedException(String.format("Could not update this document: not issuer."));
        }
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Indexing a user profile from issuer [%s]", issuer.substring(0, 8)));
        }

        UpdateResponse response = client.prepareUpdate(INDEX, PROFILE_TYPE, issuer)
                .setDoc(profileJson)
                .execute().actionGet();
    }


    /* -- Internal methods -- */


    public XContentBuilder createProfileType() {
        String stringAnalyzer = pluginSettings.getDefaultStringAnalyzer();

        try {
            XContentBuilder mapping = XContentFactory.jsonBuilder().startObject().startObject(PROFILE_TYPE)
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

                    // pubkey
                    .startObject("pubkey")
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
            throw new TechnicalException(String.format("Error while getting mapping for index [%s/%s]: %s", INDEX, PROFILE_TYPE, ioe.getMessage()), ioe);
        }
    }

}

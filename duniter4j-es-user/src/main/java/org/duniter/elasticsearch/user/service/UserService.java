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
import org.duniter.core.client.model.elasticsearch.Record;
import org.duniter.core.util.Preconditions;
import org.apache.commons.collections4.MapUtils;
import org.duniter.core.client.model.ModelUtils;
import org.duniter.core.client.model.elasticsearch.UserProfile;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.service.CryptoService;
import org.duniter.elasticsearch.client.Duniter4jClient;
import org.duniter.elasticsearch.exception.InvalidFormatException;
import org.duniter.elasticsearch.user.PluginSettings;
import org.duniter.elasticsearch.exception.AccessDeniedException;
import org.duniter.elasticsearch.service.AbstractService;
import org.elasticsearch.action.ActionWriteResponse;
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
public class UserService extends AbstractService {

    public static final String INDEX = "user";
    public static final String PROFILE_TYPE = "profile";
    public static final String SETTINGS_TYPE = "settings";

    @Inject
    public UserService(Duniter4jClient client,
                       PluginSettings settings,
                       CryptoService cryptoService) {
        super("duniter." + INDEX, client, settings.getDelegate(), cryptoService);
    }

    /**
     * Create index need for blockchain mail, if need
     */
    public UserService createIndexIfNotExists() {
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
     * Create index need for blockchain mail, if need
     */
    public boolean isIndexExists() {
        return client.existsIndex(INDEX);
    }

    /**
     * Create index for mail
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
        createIndexRequestBuilder.addMapping(SETTINGS_TYPE, createSettingsType());
        createIndexRequestBuilder.addMapping(UserEventService.EVENT_TYPE, UserEventService.createEventType());
        createIndexRequestBuilder.execute().actionGet();

        return this;
    }

    public UserService deleteIndex() {
        client.deleteIndexIfExists(INDEX);
        return this;
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

        // Check time is valid - fix #27
        verifyTimeForInsert(actualObj);

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Indexing a user profile from issuer [%s]", issuer.substring(0, 8)));
        }

        IndexResponse response = client.prepareIndex(INDEX, PROFILE_TYPE)
                .setSource(profileJson)
                .setId(issuer) // always use the issuer pubkey as id
                .setRefresh(false)
                .execute().actionGet();
        return response.getId();
    }

    /**
     * Update an user profile
     * @param profileJson
     */
    public ListenableActionFuture<? extends ActionWriteResponse> updateProfileFromJson(String id, String profileJson) {

        JsonNode actualObj = readAndVerifyIssuerSignature(profileJson);
        String issuer = getIssuer(actualObj);

        if (!Objects.equals(issuer, id)) {
            throw new AccessDeniedException(String.format("Could not update this document: only the issuer can update."));
        }

        // Check time is valid - fix #27
        verifyTimeForUpdate(INDEX, PROFILE_TYPE, id, actualObj);

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Updating a user profile from issuer [%s]", issuer.substring(0, 8)));
        }

        // First delete
        client.prepareDelete(INDEX, PROFILE_TYPE, issuer)
                .execute().actionGet();

        // Then re-create
        return client.prepareIndex(INDEX, PROFILE_TYPE, issuer)
                .setSource(profileJson)
                .execute();
    }

    /**
     *
     * Index an user settings
     * @param settingsJson settings, as JSON string
     * @return the settings id (=the issuer pubkey)
     */
    public String indexSettingsFromJson(String settingsJson) {

        JsonNode actualObj = readAndVerifyIssuerSignature(settingsJson);
        String issuer = getIssuer(actualObj);

        // Check time is valid - fix #27
        verifyTimeForInsert(actualObj);

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Indexing a user settings from issuer [%s]", issuer.substring(0, 8)));
        }

        IndexResponse response = client.prepareIndex(INDEX, SETTINGS_TYPE)
                .setSource(settingsJson)
                .setId(issuer) // always use the issuer pubkey as id
                .setRefresh(false)
                .execute().actionGet();
        return response.getId();
    }

    /**
     * Update user settings
     * @param settingsJson settings, as JSON string
     */
    public ListenableActionFuture<UpdateResponse> updateSettingsFromJson(String id, String settingsJson) {

        JsonNode actualObj = readAndVerifyIssuerSignature(settingsJson);
        String issuer = getIssuer(actualObj);

        if (!Objects.equals(issuer, id)) {
            throw new AccessDeniedException(String.format("Could not update this document: not issuer."));
        }

        // Check time is valid - fix #27
        verifyTimeForUpdate(INDEX, SETTINGS_TYPE, id, actualObj);

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Indexing a user settings from issuer [%s]", issuer.substring(0, 8)));
        }

        return client.prepareUpdate(INDEX, SETTINGS_TYPE, issuer)
                .setDoc(settingsJson)
                .execute();
    }


    public String getProfileTitle(String issuer) {

        Object title = client.getFieldById(INDEX, PROFILE_TYPE, issuer, UserProfile.PROPERTY_TITLE);
        if (title == null) return null;
        return title.toString();
    }

    public Map<String, String> getProfileTitles(Set<String> issuers) {

        Map<String, Object> titles = client.getFieldByIds(INDEX, PROFILE_TYPE, issuers, UserProfile.PROPERTY_TITLE);
        if (MapUtils.isEmpty(titles)) return null;
        Map<String, String> result = new HashMap<>();
        titles.entrySet().forEach((entry) -> result.put(entry.getKey(), entry.getValue().toString()));
        return result;
    }

    public String joinNamesFromPubkeys(Set<String> pubkeys, String separator, boolean minify) {
        Preconditions.checkNotNull(pubkeys);
        Preconditions.checkNotNull(separator);
        Preconditions.checkArgument(pubkeys.size()>0);
        if (pubkeys.size() == 1) {
            String pubkey = pubkeys.iterator().next();
            String title = getProfileTitle(pubkey);
            return title != null ? title :
                    (minify ? ModelUtils.minifyPubkey(pubkey) : pubkey);
        }

        Map<String, String> profileTitles = getProfileTitles(pubkeys);
        StringBuilder sb = new StringBuilder();
        pubkeys.forEach((pubkey)-> {
            String title = profileTitles != null ? profileTitles.get(pubkey) : null;
            sb.append(separator);
            sb.append(title != null ? title :
                    (minify ? ModelUtils.minifyPubkey(pubkey) : pubkey));
        });

        return sb.substring(separator.length());
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

    public XContentBuilder createSettingsType() {

        try {
            XContentBuilder mapping = XContentFactory.jsonBuilder().startObject().startObject(SETTINGS_TYPE)
                    .startObject("properties")

                    // time
                    .startObject("time")
                    .field("type", "integer")
                    .endObject()

                    // issuer
                    .startObject("issuer")
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()

                    // nonce
                    .startObject("nonce")
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()

                    // content
                    .startObject("content")
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()

                    .endObject()
                    .endObject().endObject();

            return mapping;
        }
        catch(IOException ioe) {
            throw new TechnicalException(String.format("Error while getting mapping for index [%s/%s]: %s", INDEX, SETTINGS_TYPE, ioe.getMessage()), ioe);
        }
    }
}

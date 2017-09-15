package org.duniter.elasticsearch.service;

/*
 * #%L
 * Duniter4j :: ElasticSearch Plugin
 * %%
 * Copyright (C) 2014 - 2016 EIS
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.duniter.core.client.model.bma.EndpointApi;
import org.duniter.core.client.model.elasticsearch.Record;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.client.service.HttpService;
import org.duniter.core.client.service.exception.HttpUnauthorizeException;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.service.CryptoService;
import org.duniter.core.util.Preconditions;
import org.duniter.core.util.StringUtils;
import org.duniter.elasticsearch.PluginSettings;
import org.duniter.elasticsearch.client.Duniter4jClient;
import org.duniter.elasticsearch.exception.DuniterElasticsearchException;
import org.duniter.elasticsearch.exception.InvalidFormatException;
import org.duniter.elasticsearch.exception.InvalidSignatureException;
import org.duniter.elasticsearch.model.SearchScrollResponse;
import org.duniter.elasticsearch.model.SynchroResult;
import org.duniter.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import java.io.IOException;
import java.util.*;

/**
 * Created by blavenie on 27/10/16.
 */
public abstract class AbstractSynchroService extends AbstractService {

    private static final String SCROLL_PARAM_VALUE = "1m";

    protected HttpService httpService;

    @Inject
    public AbstractSynchroService(Duniter4jClient client,
                                  PluginSettings settings,
                                  CryptoService cryptoService,
                                  ThreadPool threadPool, final ServiceLocator serviceLocator) {
        super("duniter.network.p2p", client, settings,cryptoService);
        threadPool.scheduleOnStarted(() -> {
            httpService = serviceLocator.getHttpService();
            setIsReady(true);
        });
    }

    /* -- protected methods -- */

    protected Peer getPeerFromAPI(EndpointApi api) {
        // TODO : get peers from currency - use peering BMA API, and select peers with ESA (ES API)
        Peer peer = Peer.newBuilder()
                .setHost(pluginSettings.getDataSyncHost())
                .setPort(pluginSettings.getDataSyncPort())
                .setApi(api.name())
                .build();

        return peer;
    }

    protected void safeSynchronizeIndex(Peer peer, String index, String type, long fromTime, SynchroResult result) {
        safeSynchronizeIndexRemap(peer, index, type, index, type, Record.PROPERTY_ISSUER, Record.PROPERTY_TIME, fromTime, result);
    }

    protected void safeSynchronizeIndexRemap(Peer peer,
                                             String fromIndex, String fromType,
                                             String toIndex, String toType,
                                             long fromTime,
                                             SynchroResult result) {
        safeSynchronizeIndexRemap(peer, fromIndex, fromType, toIndex, toType, Record.PROPERTY_ISSUER, Record.PROPERTY_TIME, fromTime, result);
    }

    protected void safeSynchronizeIndexRemap(Peer peer,
                                             String fromIndex, String fromType,
                                             String toIndex, String toType,
                                             String issuerFieldName, String versionFieldName,
                                             long fromTime,
                                             SynchroResult result) {
        Preconditions.checkArgument(fromTime >= 0);

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("[%s] [%s/%s] Synchronizing where [%s > %s]...", peer, toIndex, toType, versionFieldName, fromTime));
        }

        QueryBuilder fromQuery = createDefaultQuery(fromTime);
        safeSynchronizeIndexRemap(peer, fromIndex, fromType, toIndex, toType, issuerFieldName, versionFieldName, fromQuery, result);
    }

    protected void safeSynchronizeIndex(Peer peer,
                                        String index, String type,
                                        QueryBuilder query,
                                        SynchroResult result) {
        Preconditions.checkNotNull(query);

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("[%s] [%s/%s] Synchronizing using query [%s]...", peer, index, type, query.toString()));
        }

        safeSynchronizeIndexRemap(peer, index, type, index, type, Record.PROPERTY_ISSUER, Record.PROPERTY_TIME, query, result);
    }

    protected void safeSynchronizeIndexRemap(Peer peer,
                                             String fromIndex, String fromType,
                                             String toIndex, String toType,
                                             String issuerFieldName, String versionFieldName,
                                             QueryBuilder query,
                                             SynchroResult result) {
        Preconditions.checkNotNull(peer);
        Preconditions.checkNotNull(fromIndex);
        Preconditions.checkNotNull(fromType);
        Preconditions.checkNotNull(toIndex);
        Preconditions.checkNotNull(toType);
        Preconditions.checkNotNull(issuerFieldName);
        Preconditions.checkNotNull(versionFieldName);
        Preconditions.checkNotNull(query);
        Preconditions.checkNotNull(result);

        try {
            synchronizeIndexRemap(peer, fromIndex, fromType, toIndex, toType, issuerFieldName, versionFieldName, query, result);
        }
        catch(Exception e1) {
            // Log the first error
            if (logger.isDebugEnabled()) {
                logger.error(e1.getMessage(), e1);
            }
            else {
                logger.error(e1.getMessage());
            }
        }
    }

    private void synchronizeIndexRemap(Peer peer,
                                       String fromIndex, String fromType,
                                       String toIndex, String toType,
                                       String issuerFieldName,
                                       String versionFieldName,
                                       QueryBuilder query,
                                       SynchroResult result) {

        if (!client.existsIndex(toIndex)) {
           throw new TechnicalException(String.format("Unable to import changes. Index [%s] not exists", toIndex));
        }

        ObjectMapper objectMapper = getObjectMapper();

        long counter = 0;
        boolean stop = false;
        String scrollId = null;
        int total = 0;
        while(!stop) {
            SearchScrollResponse response;
            if (scrollId == null) {
                HttpUriRequest request = createScrollRequest(peer, fromIndex, fromType, query);
                response = executeAndParseRequest(peer, fromIndex, fromType, request);
                if (response != null) {
                    scrollId = response.getScrollId();
                    total = response.getHits().getTotal();
                    if (total > 0 && logger.isDebugEnabled()) {
                        logger.debug(String.format("[%s] [%s/%s] %s docs to check...", peer, toIndex, toType, total));
                    }
                }
            }
            else {
                HttpUriRequest request = createNextScrollRequest(peer, scrollId);
                response =  executeAndParseRequest(peer, fromIndex, fromType, request);
            }

            if (response == null) {
                stop = true;
            }
            else {
                counter += fetchAndIndex(peer, toIndex, toType, issuerFieldName, versionFieldName, response, objectMapper, result);
                stop = counter >= total;
            }
        }
    }

    private QueryBuilder createDefaultQuery(long fromTime) {

        return QueryBuilders.boolQuery()
                .should(QueryBuilders.rangeQuery("time").gte(fromTime));
    }

    private HttpPost createScrollRequest(Peer peer,
                                         String fromIndex, String fromType,
                                         QueryBuilder query) {
        HttpPost httpPost = new HttpPost(httpService.getPath(peer, fromIndex, fromType, "_search?scroll=" + SCROLL_PARAM_VALUE));
        httpPost.setHeader("Content-Type", "application/json;charset=UTF-8");

        try {
            // Query to String
            BytesStreamOutput bos = new BytesStreamOutput();
            XContentBuilder builder = new XContentBuilder(JsonXContent.jsonXContent, bos);
            query.toXContent(builder, null);
            builder.flush();

            // Sort on "_doc" - see https://www.elastic.co/guide/en/elasticsearch/reference/2.4/search-request-scroll.html
            String content = String.format("{\"query\":%s,\"size\":%s, \"sort\": [\"_doc\"]}",
                    bos.bytes().toUtf8(),
                    pluginSettings.getIndexBulkSize());
            httpPost.setEntity(new StringEntity(content, "UTF-8"));

            if (logger.isTraceEnabled()) {
                logger.trace(String.format("[%s] [%s/%s] Sending POST scroll request: %s", peer, fromIndex, fromType, content));
            }

        } catch (IOException e) {
            throw new TechnicalException("Error while preparing search query: " + e.getMessage(), e);
        }

        return httpPost;
    }

    private HttpPost createNextScrollRequest(Peer peer,
                                             String scrollId) {

        HttpPost httpPost = new HttpPost(httpService.getPath(peer, "_search", "scroll"));
        httpPost.setHeader("Content-Type", "application/json;charset=UTF-8");
        httpPost.setEntity(new StringEntity(String.format("{\"scroll\": \"%s\", \"scroll_id\": \"%s\"}",
                SCROLL_PARAM_VALUE,
                scrollId), "UTF-8"));
        return httpPost;
    }

    private SearchScrollResponse executeAndParseRequest(Peer peer, String fromIndex, String fromType, HttpUriRequest request) {
        try {
            // Execute query & parse response
            JsonNode node = httpService.executeRequest(request, JsonNode.class, String.class);
            return node == null ? null : new SearchScrollResponse(node);
        } catch (HttpUnauthorizeException e) {
            throw new TechnicalException(String.format("[%s] [%s/%s] Unable to access (%s).", peer, fromIndex, fromType, e.getMessage()), e);
        } catch (TechnicalException e) {
            throw new TechnicalException(String.format("[%s] [%s/%s] Unable to synchronize: %s", peer, fromIndex, fromType, e.getMessage()), e);
        } catch (Exception e) {
            throw new TechnicalException(String.format("[%s] [%s/%s] Unable to parse response: ", peer, fromIndex, fromType, e.getMessage()), e);
        }
    }

    private long fetchAndIndex(final Peer peer,
                               String toIndex, String toType,
                               String issuerFieldName, String versionFieldName,
                               SearchScrollResponse response,
                               final ObjectMapper objectMapper,
                               SynchroResult result) {
        boolean debug = logger.isTraceEnabled();

        long counter = 0;

        long insertHits = 0;
        long updateHits = 0;
        long invalidSignatureHits = 0;

        BulkRequestBuilder bulkRequest = client.prepareBulk();
        bulkRequest.setRefresh(true);

        for (Iterator<SearchScrollResponse.Hit> hits = response.getHits(); hits.hasNext();){
            SearchScrollResponse.Hit hit = hits.next();
            String id = hit.getId();
            JsonNode source = hit.getSource();

            if (source == null) {
                logger.error("No source for doc " + id);
            }
            else {
                counter++;

                try {
                    String issuer = source.get(issuerFieldName).asText();
                    if (StringUtils.isBlank(issuer)) {
                        throw new InvalidFormatException(String.format("Invalid format: missing or null %s field.", issuerFieldName));
                    }
                    Long version = source.get(versionFieldName).asLong();
                    if (version == null) {
                        throw new InvalidFormatException(String.format("Invalid format: missing or null %s field.", versionFieldName));
                    }

                    Map<String, Object> existingFields = client.getFieldsById(toIndex, toType, id, versionFieldName, issuerFieldName);
                    boolean exists = existingFields != null;

                    // Insert (new doc)
                    if (!exists) {

                        if (debug) {
                            logger.trace(String.format("[%s] [%s/%s] insert _id=%s\n%s", peer, toIndex, toType, id, source.toString()));
                        }

                        // FIXME: some user/profile document failed ! - see issue #11
                        // Il semble que le format JSON ne soit pas le même que celui qui a été signé
                        try {
                            readAndVerifyIssuerSignature(source, issuerFieldName);
                        } catch (InvalidSignatureException e) {
                            invalidSignatureHits++;
                            // FIXME: should enable this log (after issue #11 resolution)
                            //logger.warn(String.format("[%s] [%s/%s/%s] %s.\n%s", peer, toIndex, toType, id, e.getMessage(), source.toString()));
                        }

                        bulkRequest.add(client.prepareIndex(toIndex, toType, id)
                                .setSource(objectMapper.writeValueAsBytes(source))
                        );
                        insertHits++;
                    }

                    // Existing doc
                    else {

                        // Check same issuer
                        String existingIssuer = (String) existingFields.get(issuerFieldName);
                        if (!Objects.equals(issuer, existingIssuer)) {
                            throw new InvalidFormatException(String.format("Invalid document: not same [%s].", issuerFieldName));
                        }

                        // Check version
                        Number existingVersion = ((Number) existingFields.get(versionFieldName));
                        boolean doUpdate = (existingVersion == null || version > existingVersion.longValue());

                        if (doUpdate) {
                            if (debug) {
                                logger.trace(String.format("[%s] [%s/%s] update _id=%s\n%s", peer, toIndex, toType, id, source.toString()));
                            }

                            // FIXME: some user/profile document failed ! - see issue #11
                            // Il semble que le format JSON ne soit pas le même que celui qui a été signé
                            try {
                                readAndVerifyIssuerSignature(source, issuerFieldName);
                            } catch (InvalidSignatureException e) {
                                invalidSignatureHits++;
                                // FIXME: should enable this log (after issue #11 resolution)
                                //logger.warn(String.format("[%s] [%s/%s/%s] %s.\n%s", peer, toIndex, toType, id, e.getMessage(), source.toString()));
                            }

                            bulkRequest.add(client.prepareIndex(toIndex, toType, id)
                                    .setSource(objectMapper.writeValueAsBytes(source)));

                            updateHits++;
                        }
                    }

                } catch (DuniterElasticsearchException e) {
                    if (logger.isDebugEnabled()) {
                        logger.warn(String.format("[%s] [%s/%s/%s] %s. Skipping.\n%s", peer, toIndex, toType, id, e.getMessage(), source.toString()));
                    } else {
                        logger.warn(String.format("[%s] [%s/%s/%s] %s. Skipping.", peer, toIndex, toType, id, e.getMessage()));
                    }
                    // Skipping document (continue)
                } catch (Exception e) {
                    logger.error(String.format("[%s] [%s/%s/%s] %s. Skipping.", peer, toIndex, toType, id, e.getMessage()), e);
                    // Skipping document (continue)
                }
            }
        }

        if (bulkRequest.numberOfActions() > 0) {

            // Flush the bulk if not empty
            BulkResponse bulkResponse = bulkRequest.get();
            Set<String> missingDocIds = new LinkedHashSet<>();

            // If failures, continue but save missing blocks
            if (bulkResponse.hasFailures()) {
                // process failures by iterating through each bulk response item
                for (BulkItemResponse itemResponse : bulkResponse) {
                    boolean skip = !itemResponse.isFailed()
                            || missingDocIds.contains(itemResponse.getId());
                    if (!skip) {
                        if (debug) {
                            logger.debug(String.format("[%s] [%s/%s] could not process _id=%s: %s. Skipping.", peer, toIndex, toType, itemResponse.getId(), itemResponse.getFailureMessage()));
                        }
                        missingDocIds.add(itemResponse.getId());
                    }
                }
            }
        }

        // update result stats
        result.addInserts(toIndex, toType, insertHits);
        result.addUpdates(toIndex, toType, updateHits);
        result.addInvalidSignatures(toIndex, toType, invalidSignatureHits);

        return counter;
    }

}

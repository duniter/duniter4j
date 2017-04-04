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
import com.google.common.base.Joiner;
import org.duniter.core.util.Preconditions;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.duniter.core.client.model.elasticsearch.Record;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.client.service.HttpService;
import org.duniter.core.client.service.exception.HttpUnauthorizeException;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.service.CryptoService;
import org.duniter.core.util.StringUtils;
import org.duniter.elasticsearch.PluginSettings;
import org.duniter.elasticsearch.client.Duniter4jClient;
import org.duniter.elasticsearch.exception.InvalidFormatException;
import org.duniter.elasticsearch.model.SynchroResult;
import org.duniter.elasticsearch.service.AbstractService;
import org.duniter.elasticsearch.service.ServiceLocator;
import org.duniter.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.json.JsonXContent;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Created by blavenie on 27/10/16.
 */
public abstract class AbstractSynchroService extends AbstractService {

    protected HttpService httpService;

    @Inject
    public AbstractSynchroService(Duniter4jClient client,
                                  PluginSettings settings,
                                  CryptoService cryptoService,
                                  ThreadPool threadPool, final ServiceLocator serviceLocator) {
        super("duniter.network.p2p", client, settings,cryptoService);
        threadPool.scheduleOnStarted(() -> {
            httpService = serviceLocator.getHttpService();
        });
    }

    /* -- protected methods -- */

    protected Peer getPeerFromAPI(String filterApiName) {
        // TODO : get peers from currency - use peering BMA API, and select peers with ESA (ES API)
        Peer peer = Peer.newBuilder().setHost(pluginSettings.getDataSyncHost()).setPort(pluginSettings.getDataSyncPort()).build();
        return peer;
    }

    protected long importChangesRemap(SynchroResult result,
                                      Peer peer,
                                      String fromIndex, String fromType,
                                      String toIndex, String toType,
                                      long sinceTime) {
        Preconditions.checkNotNull(result);
        Preconditions.checkNotNull(peer);
        Preconditions.checkNotNull(fromIndex);
        Preconditions.checkNotNull(fromType);
        Preconditions.checkNotNull(toIndex);
        Preconditions.checkNotNull(toType);

        return doImportChanges(result, peer, fromIndex, fromType, toIndex, toType, Record.PROPERTY_ISSUER, Record.PROPERTY_TIME, sinceTime);
    }

    protected long importChanges(SynchroResult result, Peer peer, String index, String type, long sinceTime) {
        Preconditions.checkNotNull(result);
        Preconditions.checkNotNull(peer);
        Preconditions.checkNotNull(index);
        Preconditions.checkNotNull(type);

        return doImportChanges(result, peer, index, type, index, type, Record.PROPERTY_ISSUER, Record.PROPERTY_TIME, sinceTime);
    }

    /* -- private methods -- */

    private long doImportChanges(SynchroResult result,
                                 Peer peer,
                                 String fromIndex, String fromType,
                                 String toIndex, String toType,
                                 String issuerFieldName, String versionFieldName, long sinceTime) {


        long offset = 0;
        int size = pluginSettings.getIndexBulkSize() / 10;
        boolean stop = false;
        while(!stop) {
            long currentRowCount = doImportChangesAtOffset(result, peer,
                    fromIndex, fromType, toIndex, toType,
                    issuerFieldName, versionFieldName, sinceTime,
                    offset, size);
            offset += currentRowCount;
            stop = currentRowCount < size;
        }

        return offset; // = total rows
    }

    private long doImportChangesAtOffset(SynchroResult result, Peer peer,
                                 String fromIndex, String fromType,
                                 String toIndex, String toType,
                                 String issuerFieldName, String versionFieldName,
                                 long sinceTime,
                                 long offset,
                                 int size) {


        // Create the search query
        BytesStreamOutput bos;
        try {
            bos = new BytesStreamOutput();
            XContentBuilder builder = new XContentBuilder(JsonXContent.jsonXContent, bos);
            builder.startObject()
                    .startObject("query")
                    // bool.should
                    .startObject("bool")
                    .startObject("should")
                    // time > sinceDate
                    .startObject("range")
                    .startObject("time")
                    .field("gte", sinceTime)
                    .endObject()
                    .endObject()
                    .endObject()
                    .endObject()
                    // end: query
                    .endObject()
                    .field("from", offset)
                    .field("size", size)
                    .endObject();
            builder.flush();

        } catch(IOException e) {
            throw new TechnicalException("Error while preparing default index analyzer: " + e.getMessage(), e);
        }

        // Execute query
        JsonNode node;
        try {
            HttpPost httpPost = new HttpPost(httpService.getPath(peer, fromIndex, fromType, "_search"));
            httpPost.setHeader("Content-Type", "application/json;charset=UTF-8");
            httpPost.setEntity(new ByteArrayEntity(bos.bytes().array()));
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("[%s] [%s/%s] Sending POST request: %s", peer, fromIndex, fromType, new String(bos.bytes().array())));
            }
            // Parse response
            node = httpService.executeRequest(httpPost, JsonNode.class, String.class);
        }
        catch(HttpUnauthorizeException e) {
            logger.error(String.format("[%s] [%s/%s] Unable to access (%s). Skipping data import.", peer, fromIndex, fromType, e.getMessage()));
            return 0;
        }
        catch(TechnicalException e) {
            throw new TechnicalException("Unable to parse search response", e);
        }

        node = node.get("hits");
        int total = node == null ? 0 : node.get("total").asInt(0);
        if (logger.isDebugEnabled() && offset == 0) {
            logger.debug(String.format("[%s] [%s/%s] Rows to update: %s", peer, toIndex, toType, total));
        }

        boolean debug = logger.isTraceEnabled();

        long counter = 0;

        long insertHits = 0;
        long updateHits = 0;

        if (offset < total) {

            BulkRequestBuilder bulkRequest = client.prepareBulk();
            bulkRequest.setRefresh(true);

            for (Iterator<JsonNode> hits = node.get("hits").iterator(); hits.hasNext();){
                JsonNode hit = hits.next();
                String id = hit.get("_id").asText();
                JsonNode source = hit.get("_source");
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

                    GetResponse existingDoc = client.prepareGet(toIndex, toType, id)
                            .setFields(versionFieldName, issuerFieldName)
                            .execute().actionGet();

                    boolean doInsert = !existingDoc.isExists();

                    // Insert (new doc)
                    if (doInsert) {
                        String json = source.toString();
                        //readAndVerifyIssuerSignature(json, source);
                        if (debug) {
                            logger.trace(String.format("[%s] [%s/%s] insert _id=%s\n%s", peer, toIndex, toType, id, json));
                        }
                        bulkRequest.add(client.prepareIndex(toIndex, toType, id)
                                .setSource(json.getBytes())
                        );
                        insertHits++;
                    }

                    // Existing doc
                    else {

                        // Check same issuer
                        String existingIssuer = (String)existingDoc.getFields().get(issuerFieldName).getValue();
                        if (!Objects.equals(issuer, existingIssuer)) {
                            throw new InvalidFormatException(String.format("Invalid document: not same [%s].", issuerFieldName));
                        }

                        // Check version
                        Long existingVersion = ((Number)existingDoc.getFields().get(versionFieldName).getValue()).longValue();
                        boolean doUpdate = (existingVersion == null || version > existingVersion.longValue());

                        if (doUpdate) {
                            String json = source.toString();
                            //readAndVerifyIssuerSignature(json, source);
                            if (debug) {
                                logger.trace(String.format("[%s] [%s/%s] update _id=%s\n%s", peer, toIndex, toType, id, json));
                            }
                            bulkRequest.add(client.prepareIndex(toIndex, toType, id)
                                    .setSource(json.getBytes()));

                            updateHits++;
                        }
                    }

                } catch (InvalidFormatException e) {
                    if (debug) {
                        logger.debug(String.format("[%s] [%s/%s] %s. Skipping.", peer, toIndex, toType, e.getMessage()));
                    }
                    // Skipping document (continue)
                }
                catch (Exception e) {
                    logger.warn(String.format("[%s] [%s/%s] %s. Skipping.", peer, toIndex, toType, e.getMessage()), e);
                    // Skipping document (continue)
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
        }

        // update result stats
        result.addInserts(toIndex, toType, insertHits);
        result.addUpdates(toIndex, toType, updateHits);

        return counter;

        /*}
        finally {
            //IOUtils.closeQuietly(response);
        }*/
    }
}

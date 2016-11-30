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
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.duniter.core.client.model.elasticsearch.Record;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.client.service.HttpService;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.service.CryptoService;
import org.duniter.core.util.StringUtils;
import org.duniter.elasticsearch.PluginSettings;
import org.duniter.elasticsearch.exception.InvalidFormatException;
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
    public AbstractSynchroService(Client client, PluginSettings settings, CryptoService cryptoService,
                                  ThreadPool threadPool, final ServiceLocator serviceLocator) {
        super("duniter.network.p2p", client, settings,cryptoService);
        threadPool.scheduleOnStarted(() -> {
            httpService = serviceLocator.getHttpService();
        });
    }

    /* -- protected methods -- */

    protected Peer getPeerFromAPI(String filterApiName) {
        // TODO : get peers from currency - use peering BMA API, and select peers with ESA (ES API)
        Peer peer = new Peer(pluginSettings.getDataSyncHost(), pluginSettings.getDataSyncPort());
        return peer;
    }

    protected void importChanges(Peer peer, String index, String type, long sinceTime) {
        importChanges(peer, index, type, Record.PROPERTY_ISSUER, Record.PROPERTY_TIME, sinceTime);
    }

    protected void importChanges(Peer peer, String index, String type, String issuerFieldName, String versionFieldName, long sinceTime) {

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
                    // currency
                            /*.startObject("filter")
                                .startObject("term")
                                    .field("currency", "sou") // todo, filter on configured currency only
                                .endObject()
                            .endObject()*/
                    .endObject()
                    // end: query
                    .endObject()
                    .field("from", 0) // todo
                    .field("size", 100) // todo
                    .endObject();
            builder.flush();

        } catch(IOException e) {
            throw new TechnicalException("Error while preparing default index analyzer: " + e.getMessage(), e);
        }

        // Execute query
        String path = "/" + Joiner.on('/').join(new String[]{index, type, "_search"});
        HttpPost httpPost = new HttpPost(httpService.getPath(peer, "/" + path));
        httpPost.setEntity(new ByteArrayEntity(bos.bytes().array()));
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("[%s] [%s/%s] Sending POST request: %s", peer, index, type, new String(bos.bytes().array())));
        }
        InputStream response = httpService.executeRequest(httpPost, InputStream.class, String.class);


        // Parse response
        try {
            JsonNode node = objectMapper.readTree(response);

            node = node.get("hits");
            int total = node == null ? 0 : node.get("total").asInt(0);
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("[%s] [%s/%s] total to update: %s", peer, index, type, total));
            }

            boolean debug = logger.isTraceEnabled();

            if (total > 0) {

                int batchSize = pluginSettings.getIndexBulkSize();

                BulkRequestBuilder bulkRequest = client.prepareBulk();
                bulkRequest.setRefresh(true);

                for (Iterator<JsonNode> hits = node.get("hits").iterator(); hits.hasNext();){
                    JsonNode hit = hits.next();
                    String hitIndex = hit.get("_index").asText();
                    String hitType = hit.get("_type").asText();
                    String id = hit.get("_id").asText();
                    JsonNode source = hit.get("_source");

                    try {
                        String issuer = source.get(issuerFieldName).asText();
                        if (StringUtils.isBlank(issuer)) {
                            throw new InvalidFormatException(String.format("Invalid format: missing or null %s field.", issuerFieldName));
                        }
                        Long version = source.get(versionFieldName).asLong();
                        if (version == null) {
                            throw new InvalidFormatException(String.format("Invalid format: missing or null %s field.", versionFieldName));
                        }

                        GetResponse existingDoc = client.prepareGet(index, type, id)
                                .setFields(versionFieldName, issuerFieldName)
                                .execute().actionGet();

                        boolean doInsert = !existingDoc.isExists();

                        // Insert (new doc)
                        if (doInsert) {
                            String json = source.toString();
                            //readAndVerifyIssuerSignature(json, source);
                            if (debug) {
                                logger.trace(String.format("[%s] [%s/%s] insert _id=%s\n%s", peer, hitIndex, hitType, id, json));
                            }
                            bulkRequest.add(client.prepareIndex(hitIndex, hitType, id)
                                    .setSource(json.getBytes())
                            );
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
                                    logger.trace(String.format("[%s] [%s/%s] update _id=%s\n%s", peer, hitIndex, hitType, id, json));
                                }
                                bulkRequest.add(client.prepareIndex(hitIndex, hitType, id)
                                        .setSource(json.getBytes()));
                            }
                        }

                    } catch (InvalidFormatException e) {
                        if (debug) {
                            logger.debug(String.format("[%s] [%s/%s] %s. Skipping.", peer, index, type, e.getMessage()));
                        }
                        // Skipping document (continue)
                    }
                    catch (Exception e) {
                        logger.warn(String.format("[%s] [%s/%s] %s. Skipping.", peer, index, type, e.getMessage()), e);
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
                                    logger.debug(String.format("[%s] [%s/%s] could not process _id=%s: %s. Skipping.", peer, index, type, itemResponse.getId(), itemResponse.getFailureMessage()));
                                }
                                missingDocIds.add(itemResponse.getId());
                            }
                        }
                    }
                }
            }

        } catch(IOException e) {
            throw new TechnicalException("Unable to parse search response", e);
        }
        finally {
            IOUtils.closeQuietly(response);
        }
    }
}

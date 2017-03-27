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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.duniter.core.client.model.bma.BlockchainParameters;
import org.duniter.core.client.model.bma.EndpointApi;
import org.duniter.core.client.model.bma.jackson.JacksonUtils;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.model.NullProgressionModel;
import org.duniter.core.model.ProgressionModel;
import org.duniter.core.util.CollectionUtils;
import org.duniter.core.util.Preconditions;
import org.duniter.core.util.StringUtils;
import org.duniter.core.util.concurrent.CompletableFutures;
import org.duniter.core.util.json.JsonAttributeParser;
import org.duniter.core.util.json.JsonSyntaxException;
import org.duniter.core.util.websocket.WebsocketClientEndpoint;
import org.duniter.elasticsearch.PluginSettings;
import org.duniter.elasticsearch.exception.DuplicateIndexIdException;
import org.duniter.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.nuiton.i18n.I18n;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Created by Benoit on 30/03/2015.
 */
public class EndpointService extends AbstractService {

    public static final String ENDPOINT_TYPE = "endpoint";

    private final ProgressionModel nullProgressionModel = new NullProgressionModel();

    private org.duniter.core.client.service.bma.BlockchainRemoteService blockchainRemoteService;
    private org.duniter.core.client.service.local.NetworkService networkService;
    private ThreadPool threadPool;
    private List<WebsocketClientEndpoint.ConnectionListener> connectionListeners = new ArrayList<>();
    private final WebsocketClientEndpoint.ConnectionListener dispatchConnectionListener;

    private final JsonAttributeParser blockCurrencyParser = new JsonAttributeParser("currency");
    private final JsonAttributeParser blockHashParser = new JsonAttributeParser("hash");

    private ObjectMapper objectMapper;

    @Inject
    public EndpointService(Client client, PluginSettings settings, ThreadPool threadPool,
                           final ServiceLocator serviceLocator){
        super("duniter.network", client, settings);
        this.objectMapper = JacksonUtils.newObjectMapper();
        this.threadPool = threadPool;
        threadPool.scheduleOnStarted(() -> {
            this.blockchainRemoteService = serviceLocator.getBlockchainRemoteService();
            this.networkService = serviceLocator.getNetworkService();
        });
        dispatchConnectionListener = new WebsocketClientEndpoint.ConnectionListener() {
            @Override
            public void onSuccess() {
                synchronized (connectionListeners) {
                    connectionListeners.stream().forEach(connectionListener -> connectionListener.onSuccess());
                }
            }
            @Override
            public void onError(Exception e, long lastTimeUp) {
                synchronized (connectionListeners) {
                    connectionListeners.stream().forEach(connectionListener -> connectionListener.onError(e, lastTimeUp));
                }
            }
        };
    }

    public void registerConnectionListener(WebsocketClientEndpoint.ConnectionListener listener) {
        synchronized (connectionListeners) {
            connectionListeners.add(listener);
        }
    }

    public EndpointService indexLastPeers(Peer peer) {
        indexLastPeers(peer, nullProgressionModel);
        return this;
    }

    public EndpointService indexLastPeers(Peer peer, ProgressionModel progressionModel) {

        try {
            // Get the blockchain name from node
            BlockchainParameters parameter = blockchainRemoteService.getParameters(peer);
            if (parameter == null) {
                progressionModel.setStatus(ProgressionModel.Status.FAILED);
                logger.error(I18n.t("duniter4j.es.networkService.indexPeers.remoteParametersError", peer));
                return this;
            }
            String currencyName = parameter.getCurrency();

            indexPeers(currencyName, peer, progressionModel);

        } catch(Exception e) {
            logger.error("Error during indexLastPeers: " + e.getMessage(), e);
            progressionModel.setStatus(ProgressionModel.Status.FAILED);
        }

        return this;
    }


    public EndpointService indexPeers(String currencyName, Peer firstPeer, ProgressionModel progressionModel) {
        progressionModel.setStatus(ProgressionModel.Status.RUNNING);
        progressionModel.setTotal(100);
        long timeStart = System.currentTimeMillis();

        try {

            progressionModel.setTask(I18n.t("duniter4j.es.networkService.indexPeers.task", currencyName, firstPeer));
            logger.info(I18n.t("duniter4j.es.networkService.indexPeers.task", currencyName, firstPeer));

            // Default filter
            org.duniter.core.client.service.local.NetworkService.Filter filterDef = new org.duniter.core.client.service.local.NetworkService.Filter();
            filterDef.filterType = null;
            filterDef.filterStatus = Peer.PeerStatus.UP;
            filterDef.filterEndpoints = ImmutableList.of(EndpointApi.BASIC_MERKLED_API.name(), EndpointApi.BMAS.name());

            // Default sort
            org.duniter.core.client.service.local.NetworkService.Sort sortDef = new org.duniter.core.client.service.local.NetworkService.Sort();
            sortDef.sortType = null;

            try {
                networkService.asyncGetPeers(firstPeer, threadPool.scheduler())
                        .thenCompose(CompletableFutures::allOfToList)
                        .thenApply(networkService::fillPeerStatsConsensus)
                        .thenApply(peers -> peers.stream()
                                // filter, then sort
                                .filter(networkService.peerFilter(filterDef))
                                .map(peer -> savePeer(peer, false))
                                .collect(Collectors.toList()))
                        .thenApply(peers -> {
                            logger.info(I18n.t("duniter4j.es.networkService.indexPeers.succeed", currencyName, firstPeer, peers.size(), (System.currentTimeMillis() - timeStart)));
                            progressionModel.setStatus(ProgressionModel.Status.SUCCESS);
                            return peers;
                        });
            } catch (InterruptedException | ExecutionException e) {
                throw new TechnicalException("Error while loading peers: " + e.getMessage(), e);
            }
        } catch(Exception e) {
            logger.error("Error during indexBlocksFromNode: " + e.getMessage(), e);
            progressionModel.setStatus(ProgressionModel.Status.FAILED);
        }

        return this;
    }

/*
    public void start(Peer peer, FilterAndSortSpec networkSpec) {
        Preconditions.checkNotNull(networkSpec);
        this.networkSpec = networkSpec;
        start(peer);
    }

    public void start(Peer peer) {
        Preconditions.checkNotNull(peer);

        log.debug("Starting network crawler...");

        addListeners(peer);

        this.mainPeer = peer;

        try {
            this.peers = loadPeers(this.mainPeer).get();
        }
        catch(Exception e) {
            throw new TechnicalException("Error during start load peers", e);
        }

        isStarted = true;
        log.info("Network crawler started");
    }

    public void stop() {
        if (!isStarted) return;
        log.debug("Stopping network crawler...");

        removeListeners();

        this.mainPeer = null;
        this.mainPeerWsEp = null;
        this.isStarted = false;

        this.executorService.shutdown();

        log.info("Network crawler stopped");
    }*/

    /**
     * Create or update a peer, depending on its existence and hash
     * @param peer
     * @param wait wait indexBlocksFromNode end
     * @throws DuplicateIndexIdException
     */
    public Peer savePeer(final Peer peer, boolean wait) throws DuplicateIndexIdException {
        Preconditions.checkNotNull(peer, "peer could not be null") ;
        Preconditions.checkNotNull(peer.getCurrency(), "peer attribute 'currency' could not be null");
        //Preconditions.checkNotNull(peer.getHash(), "peer attribute 'hash' could not be null");
        Preconditions.checkNotNull(peer.getPubkey(), "peer attribute 'pubkey' could not be null");
        Preconditions.checkNotNull(peer.getHost(), "peer attribute 'host' could not be null");
        Preconditions.checkNotNull(peer.getApi(), "peer 'api' could not be null");

        Peer existingPeer = getPeerByHash(peer.getCurrency(), peer.getHash());

        // Currency not exists, or has changed, so create it
        if (existingPeer == null) {
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("Insert new peer [%s]", peer));
            }

            // Index new peer
            indexPeer(peer, wait);
        }

        // Update existing peer
        else {
            logger.trace(String.format("Update peer [%s]", peer));
            updatePeer(peer, wait);
        }
        return peer;
    }

    public void indexPeer(Peer peer, boolean wait) {
        Preconditions.checkNotNull(peer);
        Preconditions.checkArgument(StringUtils.isNotBlank(peer.getCurrency()));
        Preconditions.checkNotNull(peer.getHash());
        Preconditions.checkNotNull(peer.getHost());
        Preconditions.checkNotNull(peer.getApi());

        // Serialize into JSON
        // WARN: must use GSON, to have same JSON result (e.g identities and joiners field must be converted into String)
        try {
            String json = objectMapper.writeValueAsString(peer);

            // Preparing indexBlocksFromNode
            IndexRequestBuilder indexRequest = client.prepareIndex(peer.getCurrency(), ENDPOINT_TYPE)
                    .setId(peer.getHash())
                    .setSource(json);

            // Execute indexBlocksFromNode
            ActionFuture<IndexResponse> futureResponse = indexRequest
                    .setRefresh(true)
                    .execute();

            if (wait) {
                futureResponse.actionGet();
            }
        }
        catch(JsonProcessingException e) {
            throw new TechnicalException(e);
        }
    }

    public void updatePeer(Peer peer, boolean wait) {
        Preconditions.checkNotNull(peer);
        Preconditions.checkArgument(StringUtils.isNotBlank(peer.getCurrency()));
        Preconditions.checkNotNull(peer.getHash());
        Preconditions.checkNotNull(peer.getHost());
        Preconditions.checkNotNull(peer.getApi());

        // Serialize into JSON
        // WARN: must use GSON, to have same JSON result (e.g identities and joiners field must be converted into String)
        try {
            String json = objectMapper.writeValueAsString(peer);

            // Preparing indexBlocksFromNode
            UpdateRequestBuilder updateRequest = client.prepareUpdate(peer.getCurrency(), ENDPOINT_TYPE, peer.getHash())
                    .setDoc(json);

            // Execute indexBlocksFromNode
            ActionFuture<UpdateResponse> futureResponse = updateRequest
                    .setRefresh(true)
                    .execute();

            if (wait) {
                futureResponse.actionGet();
            }
        }
        catch(JsonProcessingException e) {
            throw new TechnicalException(e);
        }
    }

    /**
     *
     * @param currencyName
     * @param number the peer hash
     * @param json block as JSON
     */
    public EndpointService indexPeerFromJson(String currencyName, int number, byte[] json, boolean refresh, boolean wait) {
        Preconditions.checkNotNull(json);
        Preconditions.checkArgument(json.length > 0);

        // Preparing indexBlocksFromNode
        IndexRequestBuilder indexRequest = client.prepareIndex(currencyName, ENDPOINT_TYPE)
                .setId(String.valueOf(number))
                .setRefresh(refresh)
                .setSource(json);

        // Execute indexBlocksFromNode
        if (!wait) {
            indexRequest.execute();
        }
        else {
            indexRequest.execute().actionGet();
        }

        return this;
    }

    /**
     * Index the given block, as the last (current) block. This will check is a fork has occur, and apply a rollback so.
     */
    public void onNetworkChanged() {
        logger.info("ES network service -> peers changed: TODO: index new peers");
    }

    /**
     *
     * @param json block as json
     * @param refresh Enable ES update with 'refresh' tag ?
     * @param wait need to wait until processed ?
     */
    public EndpointService indexPeer(Peer peer, String json, boolean refresh, boolean wait) {
        Preconditions.checkNotNull(json);
        Preconditions.checkArgument(json.length() > 0);

        String currencyName = blockCurrencyParser.getValueAsString(json);
        String hash = blockHashParser.getValueAsString(json);

        logger.info(I18n.t("duniter4j.es.networkService.indexPeer", currencyName, peer));
        if (logger.isTraceEnabled()) {
            logger.trace(json);
        }


        // Preparing index
        IndexRequestBuilder indexRequest = client.prepareIndex(currencyName, ENDPOINT_TYPE)
                .setId(hash)
                .setRefresh(refresh)
                .setSource(json);

        // Execute indexBlocksFromNode
        if (!wait) {
            indexRequest.execute();
        }
        else {
            indexRequest.execute().actionGet();
        }

        return this;
    }

    public List<Peer> findPeersByHash(String currencyName, String query) {
        String[] queryParts = query.split("[\\t ]+");

        // Prepare request
        SearchRequestBuilder searchRequest = client
                .prepareSearch(currencyName)
                .setTypes(ENDPOINT_TYPE)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH);

        // If only one term, search as prefix
        if (queryParts.length == 1) {
            searchRequest.setQuery(QueryBuilders.prefixQuery("hash", query));
        }

        // If more than a word, search on terms match
        else {
            searchRequest.setQuery(QueryBuilders.matchQuery("hash", query));
        }

        // Sort as score/memberCount
        searchRequest.addSort("_score", SortOrder.DESC)
                .addSort("number", SortOrder.DESC);

        // Highlight matched words
        searchRequest.setHighlighterTagsSchema("styled")
                .addHighlightedField("hash")
                .addFields("hash")
                .addFields("*", "_source");

        // Execute query
        SearchResponse searchResponse = searchRequest.execute().actionGet();

        // Read query result
        return toBlocks(searchResponse, true);
    }

    /* -- Internal methods -- */

    public static XContentBuilder createEndpointTypeMapping() {
        try {
            XContentBuilder mapping = XContentFactory.jsonBuilder()
                    .startObject()
                    .startObject(ENDPOINT_TYPE)
                    .startObject("properties")

                    // currency
                    .startObject("currency")
                    .field("sortType", "string")
                    .endObject()

                    // pubkey
                    .startObject("pubkey")
                    .field("sortType", "string")
                    .field("index", "not_analyzed")
                    .endObject()

                    // api
                    .startObject("api")
                    .field("sortType", "string")
                    .field("index", "not_analyzed")
                    .endObject()

                    // uid
                    .startObject("uid")
                    .field("sortType", "string")
                    .endObject()

                    // dns
                    .startObject("dns")
                    .field("sortType", "string")
                    .endObject()

                    // ipv4
                    .startObject("ipv4")
                    .field("sortType", "string")
                    .endObject()

                    // ipv6
                    .startObject("ipv6")
                    .field("sortType", "string")
                    .endObject()

                    .endObject()
                    .endObject().endObject();

            return mapping;
        }
        catch(IOException ioe) {
            throw new TechnicalException("Error while getting mapping for peer index: " + ioe.getMessage(), ioe);
        }
    }

    public Peer getPeerByHash(String currencyName, String hash) {

        // Prepare request
        SearchRequestBuilder searchRequest = client
                .prepareSearch(currencyName)
                .setTypes(ENDPOINT_TYPE)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH);

        // If more than a word, search on terms match
        searchRequest.setQuery(QueryBuilders.matchQuery("_id", hash));

        // Execute query
        try {
            SearchResponse searchResponse = searchRequest.execute().actionGet();
            List<Peer> blocks = toBlocks(searchResponse, false);
            if (CollectionUtils.isEmpty(blocks)) {
                return null;
            }

            // Return the unique result
            return CollectionUtils.extractSingleton(blocks);
        }
        catch(JsonSyntaxException e) {
            throw new TechnicalException(String.format("Error while getting indexed peer #%s for [%s]", hash, currencyName), e);
        }

    }

    protected List<Peer> toBlocks(SearchResponse response, boolean withHighlight) {
        // Read query result
        List<Peer> result = Lists.newArrayList();
        response.getHits().forEach(searchHit -> {
            Peer peer;
            if (searchHit.source() != null) {
                String jsonString = new String(searchHit.source());
                try {
                    peer = objectMapper.readValue(jsonString, Peer.class);
                } catch(Exception e) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Error while parsing peer from JSON:\n" + jsonString);
                    }
                    throw new JsonSyntaxException("Error while read peer from JSON: " + e.getMessage(), e);
                }
            }
            else {
                peer = new Peer();
                SearchHitField field = searchHit.getFields().get("hash");
                peer.setHash(field.getValue());
            }
            result.add(peer);

            // If possible, use highlights
            if (withHighlight) {
                Map<String, HighlightField> fields = searchHit.getHighlightFields();
                for (HighlightField field : fields.values()) {
                    String blockNameHighLight = field.getFragments()[0].string();
                    peer.setHash(blockNameHighLight);
                }
            }
        });

        return result;
    }


    protected void reportIndexPeersProgress(ProgressionModel progressionModel, String currencyName, Peer peer, int offset, int total) {
        int pct = offset * 100 / total;
        progressionModel.setCurrent(pct);

        progressionModel.setMessage(I18n.t("duniter4j.es.networkService.indexPeers.progress", currencyName, peer, offset, pct));
        if (logger.isInfoEnabled()) {
            logger.info(I18n.t("duniter4j.es.networkService.indexPeers.progress", currencyName, peer, offset, pct));
        }

    }

}

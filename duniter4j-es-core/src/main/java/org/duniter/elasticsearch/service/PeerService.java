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


import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.duniter.core.client.dao.PeerDao;
import org.duniter.core.client.model.bma.BlockchainParameters;
import org.duniter.core.client.model.bma.EndpointApi;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.client.service.local.NetworkService;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.model.NullProgressionModel;
import org.duniter.core.model.ProgressionModel;
import org.duniter.core.service.CryptoService;
import org.duniter.core.util.CollectionUtils;
import org.duniter.core.util.Preconditions;
import org.duniter.core.util.concurrent.CompletableFutures;
import org.duniter.core.util.json.JsonSyntaxException;
import org.duniter.elasticsearch.PluginSettings;
import org.duniter.elasticsearch.client.Duniter4jClient;
import org.duniter.elasticsearch.exception.DuplicateIndexIdException;
import org.duniter.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.highlight.HighlightField;
import org.nuiton.i18n.I18n;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Created by Benoit on 30/03/2015.
 */
public class PeerService extends AbstractService {

    private final ProgressionModel nullProgressionModel = new NullProgressionModel();

    private org.duniter.core.client.service.bma.BlockchainRemoteService blockchainRemoteService;
    private org.duniter.core.client.service.local.NetworkService networkService;
    private ThreadPool threadPool;
    private PeerDao peerDao;

    @Inject
    public PeerService(Duniter4jClient client, PluginSettings settings, ThreadPool threadPool,
                       CryptoService cryptoService,
                       PeerDao peerDao,
                       final ServiceLocator serviceLocator){
        super("duniter.peers", client, settings, cryptoService);
        this.peerDao = peerDao;
        this.threadPool = threadPool;
        threadPool.scheduleOnStarted(() -> {
            this.blockchainRemoteService = serviceLocator.getBlockchainRemoteService();
            this.networkService = serviceLocator.getNetworkService();
        });
    }

    public PeerService indexAllPeers(Peer peer) {
        indexAllPeers(peer, nullProgressionModel);
        return this;
    }

    public PeerService indexAllPeers(Peer peer, ProgressionModel progressionModel) {

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
            logger.error("Error during indexAllPeers: " + e.getMessage(), e);
            progressionModel.setStatus(ProgressionModel.Status.FAILED);
        }

        return this;
    }


    public PeerService indexPeers(String currencyName, Peer firstPeer, ProgressionModel progressionModel) {
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
                                .map(peer -> savePeer(peer))
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

    public void listenAndIndexPeers(final Peer mainPeer) {
        // Get the blockchain name from node
        BlockchainParameters parameter = blockchainRemoteService.getParameters(mainPeer);
        if (parameter == null) {
            logger.error(I18n.t("duniter4j.es.networkService.indexPeers.remoteParametersError", mainPeer));
            return;
        }
        String currencyName = parameter.getCurrency();

        // Default filter
        NetworkService.Filter filterDef = new NetworkService.Filter();
        filterDef.filterType = null;
        filterDef.filterStatus = Peer.PeerStatus.UP;
        filterDef.filterEndpoints = ImmutableList.of(EndpointApi.BASIC_MERKLED_API.name(), EndpointApi.BMAS.name());

        // Default sort
        NetworkService.Sort sortDef = new NetworkService.Sort();
        sortDef.sortType = null;

        networkService.addPeersChangeListener(mainPeer, peers -> {
            if (CollectionUtils.isNotEmpty(peers)) {
                logger.info(String.format("[%s] Updating peers endpoints (%s endpoints found)", currencyName, peers.size()));
                peers.stream().forEach(peer -> savePeer(peer));
            }
        }, filterDef, sortDef, true /*autoreconnect*/, threadPool.scheduler());
    }

    /**
     * Create or update a peer, depending on its existence and hash
     * @param peer
     * @throws DuplicateIndexIdException
     */
    public Peer savePeer(final Peer peer) throws DuplicateIndexIdException {
        Preconditions.checkNotNull(peer, "peer could not be null") ;
        Preconditions.checkNotNull(peer.getCurrency(), "peer attribute 'currency' could not be null");
        Preconditions.checkNotNull(peer.getPubkey(), "peer attribute 'pubkey' could not be null");
        Preconditions.checkNotNull(peer.getHost(), "peer attribute 'host' could not be null");
        Preconditions.checkNotNull(peer.getApi(), "peer 'api' could not be null");

        String id = cryptoService.hash(peer.computeKey());
        peer.setId(id);

        boolean exists = peerDao.isExists(peer.getCurrency(), id);

        // Currency not exists, or has changed, so create it
        if (!exists) {
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("Insert new peer [%s]", peer));
            }

            // Index new peer
            peer.setId(id);
            peerDao.create(peer);
        }

        // Update existing peer
        else {
            logger.trace(String.format("Update peer [%s]", peer));
            peerDao.update(peer);
        }
        return peer;
    }


    /* -- protected methods -- */

    protected List<Peer> toPeers(SearchResponse response, boolean withHighlight) {
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


}

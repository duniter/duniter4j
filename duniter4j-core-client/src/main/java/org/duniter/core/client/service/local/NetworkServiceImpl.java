package org.duniter.core.client.service.local;

/*
 * #%L
 * Duniter4j :: Core Client API
 * %%
 * Copyright (C) 2014 - 2017 EIS
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
import com.google.common.collect.ImmutableList;
import org.duniter.core.client.model.bma.Constants;
import org.duniter.core.client.model.bma.EndpointApi;
import org.duniter.core.client.model.bma.NetworkPeering;
import org.duniter.core.client.model.bma.NetworkPeers;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.client.service.ServiceLocator;
import org.duniter.core.client.service.bma.BaseRemoteServiceImpl;
import org.duniter.core.client.service.bma.NetworkRemoteService;
import org.duniter.core.client.service.bma.WotRemoteService;
import org.duniter.core.client.service.exception.HttpConnectException;
import org.duniter.core.client.service.exception.HttpNotFoundException;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.service.CryptoService;
import org.duniter.core.util.CollectionUtils;
import org.duniter.core.util.Preconditions;
import org.duniter.core.util.StringUtils;
import org.duniter.core.util.concurrent.CompletableFutures;
import org.duniter.core.util.websocket.WebsocketClientEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by blavenie on 20/03/17.
 */
public class NetworkServiceImpl extends BaseRemoteServiceImpl implements NetworkService {

    private static final Logger log = LoggerFactory.getLogger(NetworkServiceImpl.class);

    private final static String BMA_URL_STATUS = "/node/summary";
    private final static String BMA_URL_BLOCKCHAIN_CURRENT = "/blockchain/current";
    private final static String BMA_URL_BLOCKCHAIN_HARDSHIP = "/blockchain/hardship/";

    private NetworkRemoteService networkRemoteService;
    private CryptoService cryptoService;
    private WotRemoteService wotRemoteService;

    public NetworkServiceImpl() {
    }

    public NetworkServiceImpl(NetworkRemoteService networkRemoteService,
                              WotRemoteService wotRemoteService,
                              CryptoService cryptoService) {
        this();
        this.networkRemoteService = networkRemoteService;
        this.wotRemoteService = wotRemoteService;
        this.cryptoService = cryptoService;
    }

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        this.networkRemoteService = ServiceLocator.instance().getNetworkRemoteService();
        this.wotRemoteService = ServiceLocator.instance().getWotRemoteService();
        this.cryptoService = ServiceLocator.instance().getCryptoService();
    }

    @Override
    public List<Peer> getPeers(Peer firstPeer) {

        // Default filter
        Filter filterDef = new Filter();
        filterDef.filterType = null;
        filterDef.filterStatus = Peer.PeerStatus.UP;
        filterDef.filterEndpoints = ImmutableList.of(EndpointApi.BASIC_MERKLED_API.name(), EndpointApi.BMAS.name());

        // Default sort
        Sort sortDef = new Sort();
        sortDef.sortType = null;

        return getPeers(firstPeer, filterDef, sortDef);
    }

    @Override
    public List<Peer> getPeers(Peer firstPeer, Filter filter, Sort sort) {

        try {
            return asyncGetPeers(firstPeer, null)
                .thenCompose(CompletableFutures::allOfToList)
                .thenApply(this::fillPeerStatsConsensus)
                .thenApply(peers -> peers.stream()
                        // filter, then sort
                        .filter(peerFilter(filter))
                        .sorted(peerComparator(sort))
                        .collect(Collectors.toList()))
                .thenApply(this::logPeers)
                .get();
        } catch (InterruptedException | ExecutionException e) {
            throw new TechnicalException("Error while loading peers: " + e.getMessage(), e);
        }
    }

    @Override
    public Predicate<Peer> peerFilter(final Filter filter) {
        return peer -> applyPeerFilter(peer, filter);
    }

    @Override
    public Comparator<Peer> peerComparator(final Sort sort) {
        return Comparator.comparing(peer -> computePeerStatsScore(peer, sort), (score1, score2) -> score2.compareTo(score1));
    }

    @Override
    public CompletableFuture<List<CompletableFuture<Peer>>> asyncGetPeers(Peer mainPeer, ExecutorService executor) throws ExecutionException, InterruptedException {
        Preconditions.checkNotNull(mainPeer);

        log.debug("Loading network peers...");

        final ExecutorService pool = (executor != null) ? executor : ForkJoinPool.commonPool();

        CompletableFuture<List<Peer>> peersFuture = CompletableFuture.supplyAsync(() -> loadPeerLeafs(mainPeer), pool);
        CompletableFuture<Map<String, String>> memberUidsFuture = CompletableFuture.supplyAsync(() -> wotRemoteService.getMembersUids(mainPeer), pool);

        return CompletableFuture.allOf(
                new CompletableFuture[] {peersFuture, memberUidsFuture})
                .thenApply(v -> {
                    final Map<String, String> memberUids = memberUidsFuture.join();
                    return peersFuture.join().stream().map(peer ->
                        CompletableFuture.supplyAsync(() -> getVersion(peer), pool)
                            .thenApply(this::getCurrentBlock)
                            .exceptionally(throwable -> {
                                peer.getStats().setStatus(Peer.PeerStatus.DOWN);
                                if(!(throwable instanceof HttpConnectException)) {
                                    Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
                                    peer.getStats().setError(cause.getMessage());
                                    if (log.isDebugEnabled()) {
                                        if (log.isTraceEnabled()) {
                                            log.debug(String.format("[%s] is DOWN: %s", peer, cause.getMessage()), cause);
                                        }
                                        else log.debug(String.format("[%s] is DOWN: %s", peer, cause.getMessage()));
                                    }
                                }
                                else if (log.isTraceEnabled()) log.debug(String.format("[%s] is DOWN", peer));
                                return peer;
                            })
                            .thenApply(apeer -> {
                                String uid = StringUtils.isNotBlank(peer.getPubkey()) ? memberUids.get(peer.getPubkey()) : null;
                                peer.getStats().setUid(uid);
                                if (peer.getStats().isReacheable() && StringUtils.isNotBlank(uid)) {
                                    getHardship(peer);
                                }
                                return apeer;
                            })
                            .exceptionally(throwable -> {
                                peer.getStats().setHardshipLevel(0);
                                return peer;
                            })
                        ).collect(Collectors.toList());
                });
    }

    public List<Peer> fillPeerStatsConsensus(final List<Peer> peers) {

        final Map<String,Long> peerCountByBuid = peers.stream()
                .filter(peer -> peer.getStats().getStatus() == Peer.PeerStatus.UP)
                .map(this::buid)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        // Compute main consensus buid
        Optional<Map.Entry<String, Long>> maxPeerCountEntry = peerCountByBuid.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getValue, (l1, l2) -> l2.compareTo(l1)))
                .findFirst();

        final String mainBuid = maxPeerCountEntry.isPresent() ? maxPeerCountEntry.get().getKey() : null;;

        // Compute total of UP peers
        final Long peersUpTotal = peerCountByBuid.values().stream().mapToLong(Long::longValue).sum();

        // Compute pct by buid
        final Map<String, Double> buidsPct = peerCountByBuid.keySet().stream()
                .collect(Collectors.toMap(
                        buid -> buid,
                        buid -> (peerCountByBuid.get(buid).doubleValue() * 100 / peersUpTotal)));

        // Set consensus stats
        peers.forEach(peer -> {
                    Peer.Stats stats = peer.getStats();
                    String buid = buid(stats);

                    // Set consensus stats on each peers
                    if (buid != null) {
                        boolean isMainConsensus = buid.equals(mainBuid);
                        stats.setMainConsensus(isMainConsensus);

                        boolean isForkConsensus = !isMainConsensus && peerCountByBuid.get(buid) > 1;
                        stats.setForkConsensus(isForkConsensus);

                        stats.setConsensusPct(isMainConsensus || isForkConsensus ? buidsPct.get(buid) : 0d);
                    }
                });

        return peers;
    }

    /* -- protected methods -- */

    protected List<Peer> loadPeerLeafs(Peer peer) {
        List<String> leaves = networkRemoteService.getPeersLeaves(peer);

        if (CollectionUtils.isEmpty(leaves)) return new ArrayList<>(); // should never occur

        // If less than 100 node, get it in ONE call
        if (leaves.size() < 100) {
            // TODO uncomment on prod
            //List<Peer> peers = networkService.getPeers(peer);
            //return ImmutableList.of(peers.get(0), peers.get(1), peers.get(2), peers.get(3));

            //return networkService.getPeers(peer);
        }

        // Get it by multiple call on /network/peering?leaf=
        List<Peer> result = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(leaves)) {
            int offset = 0;
            int count = Constants.Config.MAX_SAME_REQUEST_COUNT;
            while (offset < leaves.size()) {
                if (offset + count > leaves.size()) count = leaves.size() - offset;
                loadPeerLeafs(peer, result, leaves, offset, count);
                offset += count;
                try {
                    Thread.sleep(1000); // wait 1 s
                } catch (InterruptedException e) {
                    // stop
                    offset = leaves.size();
                }
            }
        }

        return result;
    }

    protected void loadPeerLeafs(Peer requestedPeer, List<Peer> result, List<String> leaves, int offset, int count) {

        for (int i = offset; i< offset + count; i++) {
            String leaf = leaves.get(i);
            try {
                NetworkPeers.Peer peer = networkRemoteService.getPeerLeaf(requestedPeer, leaf);

                if (CollectionUtils.isNotEmpty(peer.getEndpoints())) {
                    for (NetworkPeering.Endpoint ep: peer.getEndpoints()) {
                        if (ep != null && ep.getApi() != null) {
                            Peer peerEp = Peer.newBuilder()
                                    .setCurrency(peer.getCurrency())
                                    .setHash(leaf)
                                    .setPubkey(peer.getPubkey())
                                    .setEndpoint(ep)
                                    .build();
                            result.add(peerEp);
                        }
                    }
                }


            } catch(HttpNotFoundException e) {
                log.warn("Peer not found for leaf=" + leaf);
                // skip
            }
        }
    }


    protected boolean applyPeerFilter(Peer peer, Filter filter) {

        Peer.Stats stats = peer.getStats();

        // Filter member or mirror
        if (filter.filterType != null && (
                (filter.filterType == FilterType.MEMBER && StringUtils.isBlank(stats.getUid()))
                        || (filter.filterType == FilterType.MIRROR && StringUtils.isNotBlank(stats.getUid()))
        )) {
            return false;
        }

        // Filter on endpoints
        if (CollectionUtils.isNotEmpty(filter.filterEndpoints)
                && (StringUtils.isBlank(peer.getApi())
                    || !filter.filterEndpoints.contains(peer.getApi()))) {
            return false;
        }

        // Filter on status
        if (filter.filterStatus != null && filter.filterStatus != stats.getStatus()) {
            return false;
        }

        // Filter on SSL
        if (filter.filterSsl != null && filter.filterSsl.booleanValue() != peer.isUseSsl()) {
            return false;
        }

        return true;
    }

    protected Peer getVersion(final Peer peer) {
        JsonNode json = executeRequest(peer, BMA_URL_STATUS, JsonNode.class);
        // TODO update peer
        json = json.get("duniter");
        if (json.isMissingNode()) throw new TechnicalException(String.format("Invalid format of [%s] response", BMA_URL_STATUS));
        json = json.get("version");
        if (json.isMissingNode()) throw new TechnicalException(String.format("No version attribute found in [%s] response", BMA_URL_STATUS));
        String version = json.asText();
        peer.getStats().setVersion(version);
        return peer;
    }

    protected Peer getCurrentBlock(final Peer peer) {
        JsonNode json = executeRequest(peer, BMA_URL_BLOCKCHAIN_CURRENT , JsonNode.class);

        Integer number = json.has("number") ? json.get("number").asInt() : null;
        peer.getStats().setBlockNumber(number);

        String hash = json.has("hash") ? json.get("hash").asText() : null;
        peer.getStats().setBlockHash(hash);

        Long medianTime = json.has("medianTime") ? json.get("medianTime").asLong() : null;
        peer.getStats().setMedianTime(medianTime);

        if (log.isTraceEnabled()) {
            log.trace(String.format("[%s] current block [%s-%s]", peer.toString(), number, hash));
        }

        return peer;
    }

    protected Peer getHardship(final Peer peer) {
        if (StringUtils.isBlank(peer.getPubkey())) return peer;

        JsonNode json = executeRequest(peer, BMA_URL_BLOCKCHAIN_HARDSHIP + peer.getPubkey(), JsonNode.class);
        Integer level = json.has("level") ? json.get("level").asInt() : null;
        peer.getStats().setHardshipLevel(level);
        return peer;
    }

    protected String computeUniqueId(Peer peer) {
        return cryptoService.hash(
                new StringJoiner("|")
                .add(peer.getPubkey())
                .add(peer.getDns())
                .add(peer.getIpv4())
                .add(peer.getIpv6())
                .add(String.valueOf(peer.getPort()))
                .add(Boolean.toString(peer.isUseSsl()))
                .toString());
    }

    protected JsonNode get(final Peer peer, String path) {
        return executeRequest(peer, path, JsonNode.class);
    }

    /**
     * Log allOfToList peers found
     */
    protected List<Peer> logPeers(final List<Peer> peers) {
        if (!log.isDebugEnabled()) return peers;

        if (CollectionUtils.isEmpty(peers)) {
            log.debug("No peers found.");
        }
        else {
            log.debug(String.format("Found %s peers", peers.size()));
            if (log.isTraceEnabled()) {

                peers.forEach(peerFound -> {
                    if (peerFound.getStats().getStatus() == Peer.PeerStatus.DOWN) {
                        String error = peerFound.getStats().getError();
                        log.trace(String.format("Found peer [%s] [%s] %s",
                                peerFound.toString(),
                                peerFound.getStats().getStatus().name(),
                                error != null ? error : ""));
                    } else {
                        log.trace(String.format("Found peer [%s] [%s] [v%s] block [%s]", peerFound.toString(),
                                peerFound.getStats().getStatus().name(),
                                peerFound.getStats().getVersion(),
                                peerFound.getStats().getBlockNumber()
                        ));

                    }
                });
            }
        }
        return peers;
    }

    protected double computePeerStatsScore(Peer peer, Sort sort) {
        double score = 0;
        Peer.Stats stats = peer.getStats();
        if (sort != null && sort.sortType != null) {
            long specScore = 0;
            specScore += (sort.sortType == SortType.UID ? computeScoreAlphaValue(stats.getUid(), 3, sort.sortAsc) : 0);
            specScore += (sort.sortType == SortType.PUBKEY ? computeScoreAlphaValue(peer.getPubkey(), 3, sort.sortAsc) : 0);
            specScore += (sort.sortType == SortType.API ?
                    (peer.isUseSsl() ? (sort.sortAsc ? 1 : -1) :
                            (hasEndPointAPI(peer, EndpointApi.ES_USER_API) ? (sort.sortAsc ? 0.5 : -0.5) : 0)) : 0);
            specScore += (sort.sortType == SortType.HARDSHIP ? (stats.getHardshipLevel() != null ? (sort.sortAsc ? (10000-stats.getHardshipLevel()) : stats.getHardshipLevel()): 0) : 0);
            specScore += (sort.sortType == SortType.BLOCK_NUMBER ? (stats.getBlockNumber() != null ? (sort.sortAsc ? (1000000000 - stats.getBlockNumber()) : stats.getBlockNumber()) : 0) : 0);
            score += (10000000000l * specScore);
        }
        score += (1000000000 * (stats.getStatus() == Peer.PeerStatus.UP ? 1 : 0));
        score += (100000000  * (stats.isMainConsensus() ? 1 : 0));
        score += (1000000    * (stats.isForkConsensus() ? stats.getConsensusPct() : 0));

        score += (100     * (stats.getHardshipLevel() != null ? (10000-stats.getHardshipLevel()) : 0));
        score += /* 1     */(peer.getPubkey() != null ? computeScoreAlphaValue(peer.getPubkey(), 2, true) : 0);

        return score;
    }

    protected int computeScoreAlphaValue(String value, int nbChars, boolean asc) {
        if (StringUtils.isBlank(value)) return 0;
        int score = 0;
        value = value.toLowerCase();
        if (nbChars > value.length()) {
            nbChars = value.length();
        }
        score += (int)value.charAt(0);
        for (int i=1; i < nbChars; i++) {
            score += Math.pow(0.001, i) * value.charAt(i);
        }
        return asc ? (1000 - score) : score;
    }

    protected boolean hasEndPointAPI(Peer peer, EndpointApi api) {
        return peer.getApi() != null && peer.getApi().equalsIgnoreCase(api.name());
    }

    protected String buid(Peer peer) {
        return buid(peer.getStats());
    }

    protected String buid(Peer.Stats stats) {
        return stats.getStatus() == Peer.PeerStatus.UP
                ? stats.getBlockNumber() + "-" + stats.getBlockHash()
                : null;
    }
}

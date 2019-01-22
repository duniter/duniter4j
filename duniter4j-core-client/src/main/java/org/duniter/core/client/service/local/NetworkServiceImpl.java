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
import com.google.common.collect.Lists;
import org.duniter.core.client.config.Configuration;
import org.duniter.core.client.model.bma.*;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.client.model.local.Peers;
import org.duniter.core.client.service.ServiceLocator;
import org.duniter.core.client.service.bma.BaseRemoteServiceImpl;
import org.duniter.core.client.service.bma.BlockchainRemoteService;
import org.duniter.core.client.service.bma.NetworkRemoteService;
import org.duniter.core.client.service.bma.WotRemoteService;
import org.duniter.core.client.service.exception.HttpConnectException;
import org.duniter.core.client.service.exception.HttpNotFoundException;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.util.*;
import org.duniter.core.util.CollectionUtils;
import org.duniter.core.util.concurrent.CompletableFutures;
import org.duniter.core.util.http.InetAddressUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by blavenie on 20/03/17.
 */
public class NetworkServiceImpl extends BaseRemoteServiceImpl implements NetworkService {

    private static final Logger log = LoggerFactory.getLogger(NetworkServiceImpl.class);
    private static final String PEERS_UPDATE_LOCK_NAME = "Peers update";

    private final static String BMA_URL_STATUS = "/node/summary";
    private final static String BMA_URL_BLOCKCHAIN_CURRENT = "/blockchain/current";
    private final static String BMA_URL_BLOCKCHAIN_HARDSHIP = "/blockchain/hardship/";
    private final static String ES_URL_BLOCKCHAIN_CURRENT = "/blockchain/current";

    private NetworkRemoteService networkRemoteService;
    private WotRemoteService wotRemoteService;
    private BlockchainRemoteService blockchainRemoteService;
    private Configuration config;
    private final LockManager lockManager = new LockManager(4, 10);

    private PeerService peerService;

    public NetworkServiceImpl() {
    }

    public NetworkServiceImpl(NetworkRemoteService networkRemoteService,
                              WotRemoteService wotRemoteService,
                              BlockchainRemoteService blockchainRemoteService,
                              PeerService peerService) {
        this();
        this.networkRemoteService = networkRemoteService;
        this.wotRemoteService = wotRemoteService;
        this.blockchainRemoteService = blockchainRemoteService;
        this.peerService = peerService;
    }

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        this.networkRemoteService = ServiceLocator.instance().getNetworkRemoteService();
        this.wotRemoteService = ServiceLocator.instance().getWotRemoteService();
        this.blockchainRemoteService = ServiceLocator.instance().getBlockchainRemoteService();
        this.config = Configuration.instance();
        this.peerService = ServiceLocator.instance().getPeerService();
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
    public List<Peer> getPeers(final Peer mainPeer, Filter filter, Sort sort) {
        return getPeers(mainPeer, filter, sort, null);
    }

    @Override
    public List<Peer> getPeers(final Peer mainPeer, Filter filter, Sort sort, ExecutorService executor) {

        try {
            return asyncGetPeers(mainPeer, (filter != null ? filter.filterEndpoints : null), executor)
                .thenCompose(CompletableFutures::allOfToList)
                .thenApply(this::fillPeerStatsConsensus)
                .thenApply(peers -> peers.stream()
                        // Filter on currency
                        .filter(peer -> mainPeer.getCurrency() == null || ObjectUtils.equals(mainPeer.getCurrency(), peer.getCurrency()))
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
        return Comparator.comparing(peer -> computePeerStatsScore(peer, sort), Comparator.reverseOrder());
    }

    @Override
    public CompletableFuture<List<CompletableFuture<Peer>>> asyncGetPeers(final Peer mainPeer, List<String> filterEndpoints, ExecutorService executor) throws ExecutionException, InterruptedException {
        Preconditions.checkNotNull(mainPeer);

        log.debug("Loading network peers...");
        final ExecutorService pool = (executor != null) ? executor : ForkJoinPool.commonPool();
        CompletableFuture<List<Peer>> peersFuture = CompletableFuture.supplyAsync(() -> loadPeerLeafs(mainPeer, filterEndpoints), pool);
        CompletableFuture<Map<String, String>> memberUidsFuture = CompletableFuture.supplyAsync(() -> wotRemoteService.getMembersUids(mainPeer), pool);

        return CompletableFuture.allOf(
                new CompletableFuture[] {peersFuture, memberUidsFuture})
                .thenApply(v -> {
                    final Map<String, String> memberUids = memberUidsFuture.join();
                    return peersFuture.join().stream()
                            .map(peer -> {
                                // For if same as main peer,
                                if (mainPeer.getUrl().equals(peer.getUrl())) {
                                    // Update properties
                                    mainPeer.setPubkey(peer.getPubkey());
                                    mainPeer.setHash(peer.getHash());
                                    mainPeer.setCurrency(peer.getCurrency());
                                    // reuse instance
                                    peer = mainPeer;
                                }

                                // Exclude peer with only a local IPv4 address
                                else if (InetAddressUtils.isLocalIPv4Address(peer.getHost())) {
                                  return null;
                                }

                                // Exclude localhost address
                                else if ("localhost".equalsIgnoreCase(peer.getHost())) {
                                    return null;
                                }

                                return asyncRefreshPeer(peer, memberUids, pool);
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                });
    }


    public CompletableFuture<Peer> asyncRefreshPeer(final Peer peer, final Map<String, String> memberUids, final ExecutorService pool) {
        return CompletableFuture.supplyAsync(() -> fillVersion(peer), pool)
                .thenApply(p -> fillCurrentBlock(p))
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
                .thenApply(p -> {
                    String uid = StringUtils.isNotBlank(p.getPubkey()) ? memberUids.get(p.getPubkey()) : null;
                    p.getStats().setUid(uid);
                    if (p.getStats().isReacheable() && Peers.hasBmaEndpoint(p)) {

                        // Hardship
                        if (StringUtils.isNotBlank(uid)) {
                            fillHardship(p);
                        }
                    }
                    return p;
                })
                .exceptionally(throwable -> {
                    peer.getStats().setHardshipLevel(0);
                    return peer;
                });
    }


    public CompletableFuture<List<Peer>> asyncRefreshPeers(final Peer mainPeer, final List<Peer> peers, final ExecutorService pool) {
        return CompletableFuture.supplyAsync(() -> wotRemoteService.getMembersUids(mainPeer), pool)
                // Refresh all endpoints
                .thenApply(memberUids ->
                        peers.stream().map(peer ->
                                asyncRefreshPeer(peer, memberUids, pool))
                                .collect(Collectors.toList())
                )
                .thenCompose(CompletableFutures::allOfToList);
    }

    public List<Peer> fillPeerStatsConsensus(final List<Peer> peers) {
        if (CollectionUtils.isEmpty(peers)) return peers;

        final Map<String,Long> peerCountByBuid = peers.stream()
                .filter(peer -> Peers.isReacheable(peer) && Peers.hasDuniterEndpoint(peer))
                .map(Peers::buid)
                .filter(b -> b != null)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        // Compute main consensus buid
        Optional<Map.Entry<String, Long>> maxPeerCountEntry = peerCountByBuid.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getValue, Comparator.reverseOrder()))
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
                    String buid = Peers.buid(stats);

                    // Set consensus stats on each peers
                    if (buid != null && Peers.hasDuniterEndpoint(peer)) {
                        boolean isMainConsensus = buid.equals(mainBuid);
                        stats.setMainConsensus(isMainConsensus);

                        boolean isForkConsensus = !isMainConsensus && peerCountByBuid.containsKey(buid) && peerCountByBuid.get(buid) > 1;
                        stats.setForkConsensus(isForkConsensus);

                        stats.setConsensusPct(isMainConsensus || isForkConsensus ? buidsPct.get(buid) : 0d);
                    }
                });

        return peers;
    }

    public void addPeersChangeListener(final Peer mainPeer, final PeersChangeListener listener) {

        BlockchainParameters parameters = blockchainRemoteService.getParameters(mainPeer);

        // Default filter
        Filter filterDef = new Filter();
        filterDef.filterType = null;
        filterDef.filterStatus = Peer.PeerStatus.UP;
        filterDef.filterEndpoints = ImmutableList.of(EndpointApi.BASIC_MERKLED_API.name(), EndpointApi.BMAS.name(), EndpointApi.WS2P.name());
        filterDef.currency = parameters.getCurrency();

        // Default sort
        Sort sortDef = new Sort();
        sortDef.sortType = null;

        addPeersChangeListener(mainPeer, listener, filterDef, sortDef, true, null);

    }

    public void addPeersChangeListener(final Peer mainPeer, final PeersChangeListener listener,
                                       final Filter filter, final Sort sort, final boolean autoreconnect,
                                       final ExecutorService executor) {

        final String currency = filter != null && filter.currency != null ? filter.currency :
                blockchainRemoteService.getParameters(mainPeer).getCurrency();

        final List<String> knownBlocks = new ArrayList<>();
        final Predicate<Peer> peerFilter = peerFilter(filter);
        final Comparator<Peer> peerComparator = peerComparator(sort);
        final ExecutorService pool = (executor != null) ? executor : ForkJoinPool.commonPool();

        // Refreshing one peer (e.g. received from WS)
        Consumer<List<Peer>> updateKnownBlocks = (updatedPeers) ->
            updatedPeers.forEach(peer -> {
                String buid = Peers.buid(peer);
                if (!knownBlocks.contains(buid)) {
                    knownBlocks.add(buid);
                }
            });

        // Load all peers
        Runnable loadAllPeers = () -> {
            try {
                if (lockManager.tryLock(PEERS_UPDATE_LOCK_NAME, 1, TimeUnit.MINUTES)) {
                    try {
                        List<Peer> result = getPeers(mainPeer, filter, sort, pool);

                        knownBlocks.clear();
                        updateKnownBlocks.accept(result);

                        // Save update peers
                        peerService.save(currency, result, false/*not the full UP list*/);

                        // Send full list listener
                        listener.onChanges(result);
                    } catch (Exception e) {
                        log.error("Error while loading all peers: " + e.getMessage(), e);
                    } finally {
                        lockManager.unlock(PEERS_UPDATE_LOCK_NAME);
                    }
                }
            } catch (InterruptedException e) {
                log.warn("Could not acquire lock for reloading all peers. Skipping.");
            }
        };

        // Refreshing one peer (e.g. received from WS)
        Consumer<NetworkPeers.Peer> refreshPeerConsumer = (bmaPeer) -> {
            if (lockManager.tryLock(PEERS_UPDATE_LOCK_NAME)) {
                try {
                    final List<Peer> newPeers = new ArrayList<>();
                    addEndpointsAsPeers(bmaPeer, newPeers, null, filter.filterEndpoints);

                    CompletableFuture<List<CompletableFuture<Peer>>> jobs =
                            CompletableFuture.supplyAsync(() -> wotRemoteService.getMembersUids(mainPeer), pool)

                                    // Refresh all endpoints
                                    .thenApply(memberUids ->
                                            newPeers.stream().map(peer ->
                                                    asyncRefreshPeer(peer, memberUids, pool))
                                                    .collect(Collectors.toList())
                                    );

                    jobs.thenCompose(CompletableFutures::allOfToList)
                        .thenAccept(refreshedPeers -> {
                            if (CollectionUtils.isEmpty(refreshedPeers)) return;

                            // Get the full list
                            final Map<String, Peer> knownPeers = peerService.getPeersByCurrencyId(currency)
                                    .stream()
                                    .filter(peerFilter)
                                    .collect(Collectors.toMap(Peer::toString, Function.identity()));

                            // filter, to keep only existing peer, or expected by filter
                            List<Peer> changedPeers = refreshedPeers.stream()
                                    .filter(refreshedPeer -> {
                                String peerId = refreshedPeer.toString();
                                boolean exists = knownPeers.containsKey(peerId);
                                if (exists){
                                    knownPeers.remove(peerId);
                                }
                                // If include, add it to full list
                                boolean include = peerFilter.test(refreshedPeer);
                                if (include) {
                                    knownPeers.put(peerId, refreshedPeer);
                                }
                                return include;
                            }).collect(Collectors.toList());

                            // If something changes
                            if (CollectionUtils.isNotEmpty(changedPeers)) {
                                List<Peer> result = Lists.newArrayList(knownPeers.values());
                                fillPeerStatsConsensus(result);
                                result.sort(peerComparator);

                                updateKnownBlocks.accept(changedPeers);

                                // Save update peers
                                peerService.save(currency, changedPeers, false/*not the full UP list*/);

                                listener.onChanges(result);
                            }
                        });
                } catch (Exception e) {
                    log.error("Error while refreshing a peer: " + e.getMessage(), e);
                } finally {
                    lockManager.unlock(PEERS_UPDATE_LOCK_NAME);
                }
            }
        };

        // Manage new block event
        blockchainRemoteService.addBlockListener(mainPeer, json -> {
            log.debug("Received new block event");
            try {
                BlockchainBlock block = readValue(json, BlockchainBlock.class);
                String blockBuid = BlockchainBlocks.buid(block);
                boolean isNewBlock = (blockBuid != null && !knownBlocks.contains(blockBuid));

                // If new block + wait 3s for network propagation
                if (isNewBlock) {
                    schedule(loadAllPeers, pool, 3000/*waiting block propagation*/);
                }

            } catch(IOException e) {
                log.error("Could not parse peer received by WS: " + e.getMessage(), e);
            }
        }, autoreconnect);

        // Manage new peer event
        networkRemoteService.addPeerListener(mainPeer, json -> {

            log.debug("Received new peer event");
            try {
                final NetworkPeers.Peer bmaPeer = readValue(json, NetworkPeers.Peer.class);
                if (!lockManager.isLocked(PEERS_UPDATE_LOCK_NAME)) {
                    pool.submit(() -> refreshPeerConsumer.accept(bmaPeer));
                }
            } catch(IOException e) {
                log.error("Could not parse peer received by WS: " + e.getMessage(), e);
            }
        }, autoreconnect);

        // Default action: Load all peers
        pool.submit(loadAllPeers);

    }

    public String getVersion(final Peer peer) {
        return getVersion(getNodeSummary(peer));
    }

    public JsonNode getNodeSummary(final Peer peer) {
        return get(peer, BMA_URL_STATUS);
    }

    public String getVersion(JsonNode json) {
        json = json.get("duniter");
        if (json.isMissingNode()) throw new TechnicalException(String.format("Invalid format of [%s] response", BMA_URL_STATUS));
        json = json.get("version");
        if (json.isMissingNode()) throw new TechnicalException(String.format("No version attribute found in [%s] response", BMA_URL_STATUS));
        return json.asText();
    }

    public String getSoftware(JsonNode json) {
        json = json.get("duniter");
        if (json.isMissingNode()) throw new TechnicalException(String.format("Invalid format of [%s] response", BMA_URL_STATUS));
        json = json.get("software");
        if (json.isMissingNode()) throw new TechnicalException(String.format("No software attribute found in [%s] response", BMA_URL_STATUS));
        return json.asText();
    }

    /* -- protected methods -- */

    protected List<Peer> loadPeerLeafs(Peer peer, List<String> filterEndpoints) {
        List<String> leaves = networkRemoteService.getPeersLeaves(peer);

        if (CollectionUtils.isEmpty(leaves)) return new ArrayList<>(); // should never occur

        List<Peer> result = new ArrayList<>();

        // If less than 100 node, get it in ONE call
        if (leaves.size() <= 2000) {
            List<Peer> peers = networkRemoteService.getPeers(peer);

            if (CollectionUtils.isNotEmpty(peers)) {
                for (Peer peerEp : peers) {
                    // Filter on endpoints - fix #18
                    if (CollectionUtils.isEmpty(filterEndpoints)
                            || StringUtils.isBlank(peerEp.getApi())
                            || filterEndpoints.contains(peerEp.getApi())) {
                        String hash = ServiceLocator.instance().getCryptoService().hash(peerEp.computeKey()); // compute the hash
                        peerEp.setHash(hash);
                        result.add(peerEp);
                    }
                }
            }
        }

        // Get it by multiple call on /network/peering?leaf=
        else {
            int offset = 0;
            int count = Constants.Config.MAX_SAME_REQUEST_COUNT;
            while (offset < leaves.size()) {
                if (offset + count > leaves.size()) count = leaves.size() - offset;
                loadPeerLeafs(peer, result, leaves, offset, count, filterEndpoints);
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

    protected void loadPeerLeafs(Peer requestedPeer, List<Peer> result, List<String> leaves, int offset, int count, List<String> filterEndpoints) {

        for (int i = offset; i< offset + count; i++) {
            String leaf = leaves.get(i);
            try {
                NetworkPeers.Peer peer = networkRemoteService.getPeerLeaf(requestedPeer, leaf);
                addEndpointsAsPeers(peer, result, leaf, filterEndpoints);

            } catch(HttpNotFoundException hnfe) {
                log.debug("Peer not found for leaf=" + leaf);
                // skip
            } catch(TechnicalException e) {
                log.warn("Error while getting peer leaf=" + leaf, e.getMessage());
                // skip
            }
        }
    }

    protected void addEndpointsAsPeers(NetworkPeers.Peer peer, List<Peer> result, String hash, List<String> filterEndpoints) {
        if (CollectionUtils.isNotEmpty(peer.getEndpoints())) {
            for (NetworkPeering.Endpoint ep: peer.getEndpoints()) {
                if (ep != null && ep.getApi() != null) {
                    Peer peerEp = Peer.newBuilder()
                            .setCurrency(peer.getCurrency())
                            .setHash(hash)
                            .setPubkey(peer.getPubkey())
                            .setEndpoint(ep)
                            .build();
                    // Filter on endpoints - fix #18
                    if (CollectionUtils.isEmpty(filterEndpoints)
                            || StringUtils.isBlank(peerEp.getApi())
                            || filterEndpoints.contains(peerEp.getApi())) {
                        result.add(peerEp);
                    }
                }
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
        if (filter.filterSsl != null && filter.filterSsl != peer.isUseSsl()) {
            return false;
        }

        return true;
    }

    protected Peer fillVersion(final Peer peer) {
        if (!Peers.hasBmaEndpoint(peer) && !Peers.hasEsCoreEndpoint(peer)) return peer;
        JsonNode summary = getNodeSummary(peer);
        peer.getStats().setVersion(getVersion(summary));
        peer.getStats().setSoftware(getSoftware(summary));
        return peer;
    }

    protected Peer fillCurrentBlock(final Peer peer) {
        if (Peers.hasBmaEndpoint(peer) || Peers.hasEsCoreEndpoint(peer)) {
            JsonNode json = get(peer, BMA_URL_BLOCKCHAIN_CURRENT);

            String currency = json.has("currency") ? json.get("currency").asText() : null;
            peer.setCurrency(currency);

            Integer number = json.has("number") ? json.get("number").asInt() : null;
            peer.getStats().setBlockNumber(number);

            String hash = json.has("hash") ? json.get("hash").asText() : null;
            peer.getStats().setBlockHash(hash);

            Long medianTime = json.has("medianTime") ? json.get("medianTime").asLong() : null;
            peer.getStats().setMedianTime(medianTime);

            if (log.isTraceEnabled()) {
                log.trace(String.format("[%s] current block [%s-%s]", peer.toString(), number, hash));
            }
        }

        return peer;
    }

    protected Peer fillHardship(final Peer peer) {
        if (StringUtils.isBlank(peer.getPubkey())) return peer;

        JsonNode json = get(peer, BMA_URL_BLOCKCHAIN_HARDSHIP + peer.getPubkey());
        Integer level = json.has("level") ? json.get("level").asInt() : null;
        peer.getStats().setHardshipLevel(level);
        return peer;
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
                        log.trace(String.format(" peer [%s] [%s] %s",
                                peerFound.toString(),
                                peerFound.getStats().getStatus().name(),
                                error != null ? error : ""));
                    } else {
                        log.trace(String.format(" peer [%s] [%s] [v%s] block [%s]", peerFound.toString(),
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
                            (Peers.hasEndPointAPI(peer, EndpointApi.ES_USER_API) ? (sort.sortAsc ? 0.5 : -0.5) : 0)) : 0);
            specScore += (sort.sortType == SortType.HARDSHIP ? (stats.getHardshipLevel() != null ? (sort.sortAsc ? (10000-stats.getHardshipLevel()) : stats.getHardshipLevel()): 0) : 0);
            specScore += (sort.sortType == SortType.BLOCK_NUMBER ? (stats.getBlockNumber() != null ? (sort.sortAsc ? (1000000000 - stats.getBlockNumber()) : stats.getBlockNumber()) : 0) : 0);
            score += (10000000000L * specScore);
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



    protected void schedule(Runnable command, ExecutorService pool, long delayInMs) {
        if (pool instanceof ScheduledExecutorService) {
            ((ScheduledExecutorService)pool).schedule(command, delayInMs, TimeUnit.MILLISECONDS);
        }
        else if (delayInMs <= 0) {
            pool.submit(command);
        }
        else {
            pool.submit(() -> {
                try {
                    Thread.sleep(delayInMs);
                    command.run();
                } catch (InterruptedException e) {
                }
            });
        }
    }
}

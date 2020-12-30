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
import com.google.common.collect.Sets;
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
import org.duniter.core.service.CryptoService;
import org.duniter.core.util.*;
import org.duniter.core.util.CollectionUtils;
import org.duniter.core.util.concurrent.CompletableFutures;
import org.duniter.core.util.http.InetAddressUtils;
import org.duniter.core.util.websocket.WebsocketClientEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by blavenie on 20/03/17.
 */
public class NetworkServiceImpl extends BaseRemoteServiceImpl implements NetworkService {

    private static final Logger log = LoggerFactory.getLogger(NetworkServiceImpl.class);
    private static final String PEERS_UPDATE_LOCK_NAME = "Peers update";

    private final static String BMA_URL_STATUS = "/node/summary";
    private final static String BMA_URL_BLOCKCHAIN_CURRENT = "/blockchain/current";
    private final static String BMA_URL_BLOCKCHAIN_HARDSHIP = "/blockchain/hardship/";

    private NetworkRemoteService networkRemoteService;
    private WotRemoteService wotRemoteService;
    private BlockchainRemoteService blockchainRemoteService;
    private Configuration config;
    private final LockManager lockManager = new LockManager(4, 10);

    private PeerService peerService;
    private List<RefreshPeerListener> refreshPeerListeners = Lists.newArrayList();

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

        BlockchainBlock current = blockchainRemoteService.getCurrentBlock(firstPeer);

        // Default filter
        Filter filterDef = new Filter();
        filterDef.filterType = null;
        filterDef.filterStatus = Peer.PeerStatus.UP;
        filterDef.filterEndpoints = ImmutableList.of(EndpointApi.BASIC_MERKLED_API.label(), EndpointApi.BMAS.label(), EndpointApi.WS2P.label());
        filterDef.minBlockNumber = current.getNumber() - 100;

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
            return getPeersAsync(mainPeer, (filter != null ? filter.filterEndpoints : null), executor)
                .thenApplyAsync(this::fillPeerStatsConsensus)
                .thenApplyAsync(peers -> peers.stream()
                        // Filter on currency
                        .filter(peer -> mainPeer.getCurrency() == null || ObjectUtils.equals(mainPeer.getCurrency(), peer.getCurrency()))
                        // filter, then sort
                        .filter(peerFilter(filter))
                        .sorted(peerComparator(sort))
                        .collect(Collectors.toList()))
                .thenApplyAsync(this::logPeers)
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
    public CompletableFuture<List<Peer>> getPeersAsync(final Peer mainPeer, List<String> filterEndpoints, ExecutorService executor) throws ExecutionException, InterruptedException {
        Preconditions.checkNotNull(mainPeer);

        log.debug("Loading network peers...");
        final ExecutorService pool = (executor != null) ? executor : ForkJoinPool.commonPool();

        return CompletableFuture.supplyAsync(() -> loadPeerLeafs(mainPeer, filterEndpoints), pool)
                .thenApply(peers -> peers.stream()
                    // Replace by main peer, if same URL
                    .map(peer -> {
                        if (mainPeer.getUrl().equals(peer.getUrl())) {
                            // Update properties
                            mainPeer.setPubkey(peer.getPubkey());
                            mainPeer.setHash(peer.getHash());
                            mainPeer.setCurrency(peer.getCurrency());
                            mainPeer.setPeering(peer.getPeering());
                            // reuse instance
                            return mainPeer;
                        }

                        return peer;
                    })
                    // Exclude peer with only a local address
                    .filter(peer -> InetAddressUtils.isNotLocalAddress(peer.getHost()))
                    .collect(Collectors.toList())
        )
         .thenCompose(peers -> this.refreshPeersAsync(mainPeer, peers, pool));
    }


    public CompletableFuture<Peer> refreshPeerAsync(final Peer peer,
                                                    final Map<String, String> memberUids,
                                                    final List<Ws2pHead> ws2pHeads,
                                                    final BlockchainDifficulties difficulties,
                                                    final ExecutorService pool) {
        if (log.isDebugEnabled()) log.debug(String.format("[%s] Refreshing peer status", peer.toString()));

        CompletableFuture<Peer> result;
        // WS2P: refresh using heads
        if (Peers.hasWs2pEndpoint(peer)) {
            result = CompletableFuture.supplyAsync(() -> fillWs2pPeer(peer, memberUids, ws2pHeads, difficulties), pool);
        }

        // BMA or ES_CORE
        else if (Peers.hasBmaEndpoint(peer) || Peers.hasEsCoreEndpoint(peer)) {

            result = CompletableFuture.allOf(
                    CompletableFuture.supplyAsync(() -> fillNodeSummary(peer), pool),
                    CompletableFuture.supplyAsync(() -> fillCurrentBlock(peer), pool)
            )
                .thenApply((v) -> peer)
                .exceptionally(throwable -> {
                    peer.getStats().setStatus(Peer.PeerStatus.DOWN);
                    if(!(throwable instanceof HttpConnectException)) {
                        Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
                        peer.getStats().setError(cause.getMessage());
                        if (log.isDebugEnabled()) {
                            if (log.isTraceEnabled()) log.debug(String.format("[%s] is DOWN: %s", peer, cause.getMessage()), cause);
                            else log.debug(String.format("[%s] is DOWN: %s", peer, cause.getMessage()));
                        }
                    }
                    else if (log.isTraceEnabled()) log.debug(String.format("[%s] is DOWN", peer));
                    return peer;
                })
                .thenApplyAsync(p -> {
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

        // Unknown API: just return the peer
        else {
            result = CompletableFuture.completedFuture(peer);
        }

        // No listeners: return result
        if (CollectionUtils.isEmpty(refreshPeerListeners)) {
            return result;
        }

        // Executing listeners
        return result.thenApplyAsync(p -> CompletableFuture.allOf(
                refreshPeerListeners.stream()
                    .map(l -> CompletableFuture.runAsync(() -> l.onRefresh(peer), pool))
                    .toArray(CompletableFuture[]::new)
                )
                .exceptionally(e -> {
                    if (log.isDebugEnabled()) log.error(String.format("[%s] Refresh peer listeners error: %s", peer, e.getMessage()), e);
                    else log.error(String.format("[%s] Refresh peer listeners error: %s", peer, e.getMessage()));
                    return null;
                }))
            // Return the peer, as result
            .thenApply(v -> peer);
    }

    public Peer fillWs2pPeer(final Peer peer,
                             final Map<String, String> memberUids,
                             final List<Ws2pHead> ws2pHeads,
                             final BlockchainDifficulties difficulties) {
        if (log.isDebugEnabled()) log.debug(String.format("[%s] Refreshing WS2P peer status", peer.toString()));

        if (StringUtils.isBlank(peer.getPubkey()) || StringUtils.isBlank(peer.getEpId())) return peer;

        Ws2pHead ws2pHead = ws2pHeads.stream().filter(head ->
                peer.getPubkey().equals(head.getPubkey())
                && peer.getEpId().equals(head.getWs2pid()
                )
        ).findFirst().orElse(null);

        Peer.Stats stats = peer.getStats();

        if (ws2pHead != null) {
            if (ws2pHead.getBlock() != null) {
                String[] blockParts = ws2pHead.getBlock().split("-");
                if (blockParts.length == 2) {
                    stats.setBlockNumber(Integer.parseInt(blockParts[0]));
                    stats.setBlockHash(blockParts[1]);
                }
            }
            stats.setSoftware(ws2pHead.getSoftware());
            stats.setVersion(ws2pHead.getSoftwareVersion());
        }
        else {
            stats.setStatus(Peer.PeerStatus.DOWN);
        }

        // Set uid
        String uid = memberUids.get(peer.getPubkey());
        stats.setUid(uid);

        if (uid != null) {
            Integer difficulty = 0;
            if (stats.getBlockNumber() == null || (stats.getBlockNumber().intValue()+1 == difficulties.getBlock().intValue())) {
                difficulty = Stream.of(difficulties.getLevels())
                        .filter(d -> uid.equals(d.getUid()))
                        .map(d -> d.getLevel())
                        .filter(Objects::nonNull)
                        .findFirst()
                        // Could not known hardship, so fill 0 if member (=can compute)
                        .orElse(new Integer(0));
            }
            stats.setHardshipLevel(difficulty);
        }
        else {
            stats.setHardshipLevel(null);
        }

        return peer;
    }

    public CompletableFuture<List<Peer>> refreshPeersAsync(final Peer mainPeer,final  List<Peer> peers, final ExecutorService pool) {

        if (CollectionUtils.isEmpty(peers)) return CompletableFuture.completedFuture(ImmutableList.of());

        CompletableFuture<Map<String, String>> memberUidsFuture = CompletableFuture.supplyAsync(() -> wotRemoteService.getMembersUids(mainPeer), pool);
        CompletableFuture<List<Ws2pHead>> ws2pHeadsFuture = CompletableFuture.supplyAsync(() -> networkRemoteService.getWs2pHeads(mainPeer), pool);
        CompletableFuture<BlockchainDifficulties> difficultiesFuture = CompletableFuture.supplyAsync(() -> blockchainRemoteService.getDifficulties(mainPeer), pool);

        return CompletableFuture.allOf(memberUidsFuture, ws2pHeadsFuture, difficultiesFuture)

                // Refresh all endpoints
                .thenApply(v -> {
                    final Map<String, String> memberUids = memberUidsFuture.join();
                    final List<Ws2pHead> ws2pHeads = ws2pHeadsFuture.join();
                    final BlockchainDifficulties difficulties = difficultiesFuture.join();
                    return peers.stream().map(peer ->
                            refreshPeerAsync(peer, memberUids, ws2pHeads, difficulties, pool))
                            .collect(Collectors.toList());
                })
                .thenCompose(CompletableFutures::allOfToList);

    }

    public List<Peer> fillPeerStatsConsensus(final List<Peer> peers) {
        if (CollectionUtils.isEmpty(peers)) return peers;

        final Map<String,Long> peerCountByBuid = peers.stream()
                .filter(peer -> Peers.isReacheable(peer) && Peers.hasDuniterEndpoint(peer))
                .map(Peers::buid)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        // Compute main consensus buid
        Optional<Map.Entry<String, Long>> maxPeerCountEntry = peerCountByBuid.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getValue, Comparator.reverseOrder()))
                .findFirst();

        final String mainBuid = maxPeerCountEntry.isPresent() ? maxPeerCountEntry.get().getKey() : null;

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

    public Closeable addPeersChangeListener(final Peer mainPeer, final PeersChangeListener listener) {

        BlockchainParameters parameters = blockchainRemoteService.getParameters(mainPeer);
        fillCurrentBlock(mainPeer);

        // Default filter
        Filter filterDef = new Filter();
        filterDef.filterType = null;
        filterDef.filterStatus = Peer.PeerStatus.UP;
        filterDef.filterEndpoints = ImmutableList.of(EndpointApi.BASIC_MERKLED_API.label(), EndpointApi.BMAS.label(), EndpointApi.WS2P.label());
        filterDef.currency = parameters.getCurrency();

        // Skip node on an old fork
        if (mainPeer.getStats().getBlockNumber() != null) {
            filterDef.minBlockNumber = mainPeer.getStats().getBlockNumber() - 100;
        }

        // Default sort
        Sort sortDef = new Sort();
        sortDef.sortType = null;

        return addPeersChangeListener(mainPeer, listener, filterDef, sortDef, true, null);

    }

    public Closeable addPeersChangeListener(final Peer mainPeer, final PeersChangeListener listener,
                                            final Filter filter, final Sort sort, final boolean autoreconnect,
                                            final ExecutorService executor) {

        final String currency = filter != null && filter.currency != null ? filter.currency :
                blockchainRemoteService.getParameters(mainPeer).getCurrency();

        final Set<String> knownBlocks = Sets.newHashSet();
        final Predicate<Peer> peerFilter = peerFilter(filter);
        final Comparator<Peer> peerComparator = peerComparator(sort);
        final ExecutorService pool = (executor != null) ? executor : ForkJoinPool.commonPool();
        final int peerUpMaxAgeInMs = config.getPeerUpMaxAge();

        // Refreshing one peer (e.g. received from WS)
        Consumer<List<Peer>> updateKnownBlocks = (updatedPeers) ->
            knownBlocks.addAll(updatedPeers.stream().map(Peers::buid).collect(Collectors.toSet()))
        ;

        // Load all peers
        Runnable loadAllPeers = () -> {
            try {
                if (lockManager.tryLock(PEERS_UPDATE_LOCK_NAME, 1, TimeUnit.MINUTES)) {
                    try {
                        long now = System.currentTimeMillis();
                        List<Peer> result = getPeers(mainPeer, filter, sort, pool);

                        // Mark old peers as DOWN
                        long minUpTimeInMs = (System.currentTimeMillis() - peerUpMaxAgeInMs);

                        knownBlocks.clear();
                        updateKnownBlocks.accept(result);

                        // Save update peers
                        peerService.save(currency, result);

                        // Set old peers as DOWN (with a delay)
                        peerService.updatePeersAsDown(currency, minUpTimeInMs, filter.filterEndpoints);

                        long duration = System.currentTimeMillis() - now;

                        // If took more than 2 min => warning
                        if (duration /1000/60 > 2) {
                            log.warn(String.format("Refreshing peers took %s seconds", Math.round(duration/1000)));
                        }

                        // Send full list listener
                        listener.onChanges(result);
                    } catch (Exception e) {
                        log.error("Error while loading all peers: " + e.getMessage(), e);
                    } finally {
                        lockManager.unlock(PEERS_UPDATE_LOCK_NAME);
                    }
                }
                else {
                    log.debug("Could not acquire lock for reloading all peers. Skipping.");
                }
            } catch (InterruptedException e) {
                log.warn("Stopping reloading all peers: " + e.getMessage());
            }
        };

        // Refreshing one peer (e.g. received from WS)
        Consumer<NetworkPeers.Peer> refreshPeerConsumer = (bmaPeer) -> {
            if (lockManager.tryLock(PEERS_UPDATE_LOCK_NAME)) {
                try {
                    final List<Peer> newPeers = new ArrayList<>();
                    addEndpointsAsPeers(bmaPeer, newPeers, null, filter.filterEndpoints);

                    refreshPeersAsync(mainPeer, newPeers, executor)
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

                                // Save updated peers
                                peerService.save(currency, changedPeers);

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
        WebsocketClientEndpoint.MessageListener blockListener = json -> {
            log.debug("Received new block event");
            try {
                BlockchainBlock block = readValue(json, BlockchainBlock.class);
                String blockBuid = BlockchainBlocks.buid(block);
                boolean isNewBlock = (blockBuid != null && !knownBlocks.contains(blockBuid));

                // If new block + wait 3s for network propagation
                if (isNewBlock) {
                    schedule(loadAllPeers, pool, 3000/*waiting 3s, for block propagation*/);
                }

            } catch(IOException e) {
                log.error("Could not parse peer received by WS: " + e.getMessage(), e);
            }
        };
        WebsocketClientEndpoint wsBlockEndpoint = blockchainRemoteService.addBlockListener(mainPeer, blockListener, autoreconnect);

        // Manage new peer event
        WebsocketClientEndpoint.MessageListener peerListsner = json -> {

            log.debug("Received new peer event");
            try {
                final NetworkPeers.Peer bmaPeer = readValue(json, NetworkPeers.Peer.class);
                if (!lockManager.isLocked(PEERS_UPDATE_LOCK_NAME)) {
                    pool.submit(() -> refreshPeerConsumer.accept(bmaPeer));
                }
            } catch(IOException e) {
                log.error("Could not parse peer received by WS: " + e.getMessage(), e);
            }
        };
        WebsocketClientEndpoint wsPeerEndpoint = networkRemoteService.addPeerListener(mainPeer, peerListsner, autoreconnect);

        // Default action: Load all peers
        pool.submit(loadAllPeers);

        // Return the tear down logic
        return () -> {
            wsBlockEndpoint.unregisterListener(blockListener);
            wsPeerEndpoint.unregisterListener(peerListsner);
        };
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

    public NetworkService addRefreshPeerListener(RefreshPeerListener listener) {
        refreshPeerListeners.add(listener);
        return this;
    }

    public NetworkService removeRefreshPeerListener(RefreshPeerListener listener) {
        refreshPeerListeners.remove(listener);
        return this;
    }

    /* -- protected methods -- */

    protected List<Peer> loadPeerLeafs(Peer peer, List<String> filterEndpoints) {
        List<String> leaves = networkRemoteService.getPeersLeaves(peer);

        if (CollectionUtils.isEmpty(leaves)) return ImmutableList.of();

        CryptoService cryptoService = ServiceLocator.instance().getCryptoService();

        // If less than 100 node, get it in ONE call
        if (leaves.size() <= 2000) {
            List<Peer> peers = networkRemoteService.getPeers(peer);
            if (CollectionUtils.isEmpty(peers)) return ImmutableList.of();
            return peers.stream()
                // Filter on endpoints - fix #18
                .filter(peerEp -> CollectionUtils.isEmpty(filterEndpoints)
                        || StringUtils.isBlank(peerEp.getApi())
                        || filterEndpoints.contains(peerEp.getApi()))

                // Compute the hash
                .map(peerEp -> {
                    String hash = cryptoService.hash(peerEp.computeKey());
                    peerEp.setHash(hash);
                    return peerEp;
                }).collect(Collectors.toList());
        }

        // Get it by multiple call on /network/peering?leaf=
        List<Peer> result = Lists.newArrayList();
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
        if (filter == null) return true;

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

        // Filter block number
        if (filter.minBlockNumber != null && (stats.getBlockNumber() == null || stats.getBlockNumber().intValue() < filter.minBlockNumber.intValue())) {
            return false;
        }

        return true;
    }

    protected Peer fillNodeSummary(final Peer peer) {
        // Skip if no BMA, BMAS or ES_CORE_API
        if (!Peers.hasBmaEndpoint(peer) && !Peers.hasEsCoreEndpoint(peer)) return peer;

        JsonNode summary = getNodeSummary(peer);
        peer.getStats().setVersion(getVersion(summary));
        peer.getStats().setSoftware(getSoftware(summary));

        return peer;
    }

    protected Peer fillCurrentBlock(final Peer peer) {
        // Skip if no BMA, BMAS or ES_CORE_API
        if (!Peers.hasBmaEndpoint(peer) && !Peers.hasEsCoreEndpoint(peer)) return peer;

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
                        log.trace(String.format(" [%s] status is %s %s",
                                peerFound.toString(),
                                Peer.PeerStatus.DOWN.name(),
                                error != null ? (":" + error) : ""));
                    } else {
                        log.trace(String.format(" [%s] status %s: [v%s] block [%s]", peerFound.toString(),
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

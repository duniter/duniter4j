package org.duniter.core.client.model.local;

/*-
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

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.duniter.core.client.model.bma.*;
import org.duniter.core.util.CollectionUtils;
import org.duniter.core.util.Preconditions;
import org.duniter.core.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by blavenie on 12/09/17.
 */
public final class Peers {

    private static final Logger log = LoggerFactory.getLogger(Peers.class);

    private Peers() {
        // helper class
    }

    public static boolean hasEndPointAPI(Peer peer, EndpointApi api) {
        return peer.getApi() != null && peer.getApi().equalsIgnoreCase(api.name());
    }

    public static String buid(Peer peer) {
        return buid(peer.getStats());
    }

    public static String buid(Peer.Stats stats) {
        return stats.getStatus() == Peer.PeerStatus.UP && stats.getBlockNumber() != null
                ? stats.getBlockNumber() + "-" + stats.getBlockHash()
                : null;
    }

    public static boolean hasBmaEndpoint(Peer peer) {
        return hasEndPointAPI(peer, EndpointApi.BASIC_MERKLED_API) ||
               hasEndPointAPI(peer, EndpointApi.BMAS);
    }

    public static boolean hasWs2pEndpoint(Peer peer) {
        return hasEndPointAPI(peer, EndpointApi.WS2P);
    }

    public static boolean hasDuniterEndpoint(Peer peer) {
        return hasBmaEndpoint(peer) ||
                hasWs2pEndpoint(peer);
    }

    public static boolean hasEsCoreEndpoint(Peer peer) {
        return hasEndPointAPI(peer, EndpointApi.ES_CORE_API);
    }

    public static boolean isReacheable(Peer peer) {
        return peer.getStats() != null && peer.getStats().isReacheable();
    }

    public static NetworkPeers.Peer toBmaPeer(Peer endpointAsPeer) {
        NetworkPeers.Peer result = new NetworkPeers.Peer();

        try {
            // Fill BMA peer, using the raw document
            NetworkPeerings.parse(endpointAsPeer.getPeering().getRaw(), result);
            // Override the status, last_try and first_down, using stats
            Peer.PeerStatus status = getStatus(endpointAsPeer).orElse(Peer.PeerStatus.DOWN);
            result.setStatus(status.name());
            if (status == Peer.PeerStatus.UP) {
                result.setLastTry(getLastUpTime(endpointAsPeer).get());
            } else {
                result.setFirstDown(getFirstDownTime(endpointAsPeer).get());
            }
            return result;

        } catch (IOException e) {
            log.error("Unable to parse peering raw document found in: " + e.getMessage());
            // Continue to next endpoint
        }
        return null;
    }

    public static List<NetworkPeers.Peer> toBmaPeers(List<Peer> endpointAsPeers) {
        if (CollectionUtils.isEmpty(endpointAsPeers)) return null;

        // Group by peering document
        Multimap<String, Peer> groupByPeering = ArrayListMultimap.create();
        endpointAsPeers.stream()
                .filter(endpointAsPeer ->
                        endpointAsPeer.getPeering() != null
                        && endpointAsPeer.getPubkey() != null
                        && endpointAsPeer.getPeering().getSignature() != null
                        && endpointAsPeer.getPeering().getRaw() != null
                )
                .forEach(endpointAsPeer ->  {
                    String peeringKey = String.format("%s:%s:%s",
                                    endpointAsPeer.getPubkey(),
                                    endpointAsPeer.getPeering().getBlockNumber(),
                                    endpointAsPeer.getPeering().getSignature());
                    groupByPeering.put(peeringKey, endpointAsPeer);
                });

        // Sort keys, to select only first peering doc, by pubkey (by block number)
        Set<String> processedPubkeys = Sets.newHashSet();
        return groupByPeering.keySet().stream()
                .sorted(Comparator.naturalOrder())
                .map(peeringKey -> {
                    String pubkey = peeringKey.substring(0, peeringKey.indexOf(':'));
                    // Skip if already processed
                    if (processedPubkeys.contains(pubkey)) return null;
                    // Remember the pubkey, to skip it next time
                    processedPubkeys.add(pubkey);

                    // Get the first endpoint found for this pubkey
                    return groupByPeering.get(peeringKey).stream().map(Peers::toBmaPeer)
                            .filter(Objects::nonNull)
                            .findFirst().orElse(null);
                })
                // Remove skipped items
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static NetworkWs2pHeads.Head toWs2pHead(Peer peer) {
        NetworkWs2pHeads.Head result = new NetworkWs2pHeads.Head();

        // TODO : add implementation

        return result;
    }

    public static Optional<Peer.PeerStatus> getStatus(final Peer peer) {
        return peer.getStats() != null ?
                Optional.ofNullable(peer.getStats().getStatus()) :
                Optional.empty();
    }

    public static Optional<Long> getLastUpTime(final Peer peer) {
        return peer.getStats() != null ?
                Optional.ofNullable(peer.getStats().getLastUpTime()) :
                Optional.empty();
    }

    public static Optional<Long> getFirstDownTime(final Peer peer) {
        return peer.getStats() != null ?
                Optional.ofNullable(peer.getStats().getFirstDownTime()) :
                Optional.empty();
    }

    public static String getPeeringBlockStamp(final Peer peer) {
        return peer.getPeering() != null &&
                peer.getPeering().getBlockNumber() != null &&
                peer.getPeering().getBlockHash() != null
                ? (peer.getPeering().getBlockNumber() + "-" + peer.getPeering().getBlockHash()) : null;
    }

    public static String getStatsBlockStamp(final Peer peer) {
        return peer.getStats() != null &&
                peer.getStats().getBlockNumber() != null &&
                peer.getStats().getBlockHash() != null
                ? (peer.getStats().getBlockNumber() + "-" + peer.getStats().getBlockHash()) : null;
    }

    public static NetworkPeering.Endpoint toBmaEndpoint(Peer ep) {
        NetworkPeering.Endpoint bmaEp = new NetworkPeering.Endpoint();
        bmaEp.setApi(ep.getApi());
        bmaEp.setId(ep.getEpId());
        bmaEp.setDns(ep.getDns());
        bmaEp.setPort(ep.getPort());
        bmaEp.setIpv4(ep.getIpv4());
        bmaEp.setIpv6(ep.getIpv6());
        bmaEp.setPath(ep.getPath());
        return bmaEp;
    }

    public static NetworkPeers.Peer toBmaPeer(NetworkPeering peeringDocument) {
        NetworkPeers.Peer result = new NetworkPeers.Peer();

        result.setCurrency(peeringDocument.getCurrency());
        result.setPubkey(peeringDocument.getPubkey());
        result.setBlock(peeringDocument.getBlock());
        result.setSignature(peeringDocument.getSignature());
        result.setVersion(peeringDocument.getVersion());
        result.setEndpoints(peeringDocument.getEndpoints());
        result.setStatus(peeringDocument.getStatus());

        result.setRaw(peeringDocument.getRaw());

        return result;
    }

    public static Peer setPeering(Peer peer, NetworkPeering peeringDocument)  {
        Preconditions.checkNotNull(peer);
        Preconditions.checkNotNull(peeringDocument);

        Peer.Peering peering = (peer.getPeering() != null) ? peer.getPeering() : new Peer.Peering();

        // Copy some fields
        peer.setPubkey(peeringDocument.getPubkey());
        peer.setCurrency(peeringDocument.getCurrency());

        peering.setVersion(peeringDocument.getVersion());
        peering.setSignature(peeringDocument.getSignature());

        // Copy block infos
        if (StringUtils.isNotBlank(peeringDocument.getBlock())) {
            String[] blockParts = peeringDocument.getBlock().split("-");
            if (blockParts.length == 2) {
                peering.setBlockNumber(Integer.parseInt(blockParts[0]));
                peering.setBlockHash(blockParts[1]);
            }
        }

        return peer;
    }

    public static Peer setStats(Peer peer, NetworkPeering peeringDocument)  {
        Preconditions.checkNotNull(peer);
        Preconditions.checkNotNull(peeringDocument);

        Peer.Stats stats = peer.getStats() != null ? peer.getStats() : new Peer.Stats();

        // Copy block infos
        if (StringUtils.isNotBlank(peeringDocument.getBlock())) {
            String[] blockParts = peeringDocument.getBlock().split("-");
            if (blockParts.length == 2) {
                stats.setBlockNumber(Integer.parseInt(blockParts[0]));
                stats.setBlockHash(blockParts[1]);
            }
        }

        // Update peer status UP/DOWN
        if ("UP".equalsIgnoreCase(peeringDocument.getStatus())) {
            stats.setStatus(Peer.PeerStatus.UP);

            // FIXME: Duniter 1.7 return lastUpTime in ms. Check if this a bug or not
            stats.setLastUpTime((long)Math.round(System.currentTimeMillis() / 1000));
        }
        else {
            stats.setStatus(Peer.PeerStatus.DOWN);
        }

        return peer;
    }
}

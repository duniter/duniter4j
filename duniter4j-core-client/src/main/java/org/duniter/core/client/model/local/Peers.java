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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.duniter.core.client.model.bma.*;
import org.duniter.core.util.CollectionUtils;
import org.duniter.core.util.Preconditions;
import org.duniter.core.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by blavenie on 12/09/17.
 */
public final class Peers {

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

    public static List<NetworkPeers.Peer> toBmaPeers(List<Peer> peers) {
        if (CollectionUtils.isEmpty(peers)) return null;

        // Group endpoint by pubkey
        Map<String, List<Peer>> epByPubkeys = Maps.newHashMap();
        peers.stream().forEach(peer -> {
            String pubkey = peer.getPubkey();
            if (StringUtils.isNotBlank(pubkey)) {
                List<Peer> endpoints = epByPubkeys.get(pubkey);
                if (endpoints == null) {
                    endpoints = Lists.newArrayList();
                    epByPubkeys.put(pubkey, endpoints);
                }
                endpoints.add(peer);
            }
        });

        return epByPubkeys.values().stream().map(endpoints -> {
            Peer firstEp = endpoints.get(0);
            NetworkPeers.Peer result = new NetworkPeers.Peer();
            result.setCurrency(firstEp.getCurrency());
            result.setPubkey(firstEp.getPubkey());

            if (firstEp.getPeering() != null) {
                result.setBlock(getPeeringBlockStamp(firstEp));
                result.setSignature(firstEp.getPeering().getSignature());
                result.setVersion(firstEp.getPeering().getVersion());
            }
            else {
                result.setVersion(Protocol.VERSION);
                result.setBlock(getStatsBlockStamp(firstEp));
                result.setSignature(null);
            }

            // Compute status (=UP is at least one endpoint is UP)
            String status = endpoints.stream()
                    .map(Peers::getStatus)
                    .filter(s -> s == Peer.PeerStatus.UP)
                    .findAny()
                    .orElse(Peer.PeerStatus.DOWN).name();
            result.setStatus(status);

            // Compute endpoints list
            List<NetworkPeering.Endpoint> bmaEps = endpoints.stream()
                    .map(Peers::toBmaEndpoint)
                    .collect(Collectors.toList());
            result.setEndpoints(bmaEps.toArray(new NetworkPeering.Endpoint[bmaEps.size()]));

            // Compute last try
            Long lastUpTime =  endpoints.stream()
                    .map(Peers::getLastUpTime)
                    .filter(Objects::nonNull)
                    .max(Long::compare)
                    .orElse(null);

            // Compute last try
            result.setLastTry(lastUpTime);

            return result;
        }).collect(Collectors.toList());
    }

    public static NetworkWs2pHeads.Head toWs2pHead(Peer peer) {
        NetworkWs2pHeads.Head result = new NetworkWs2pHeads.Head();

        // TODO : add implementation

        return result;
    }

    public static  Peer.PeerStatus getStatus(final Peer peer) {
        return peer.getStats() != null &&
                peer.getStats().getStatus() != null ?
                peer.getStats().getStatus() : null;
    }

    public static Long getLastUpTime(final Peer peer) {
        return peer.getStats() != null &&
                peer.getStats().getLastUpTime() != null ?
                peer.getStats().getLastUpTime() : null;
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
        bmaEp.setApi(EndpointApi.valueOf(ep.getApi()));
        bmaEp.setId(ep.getEpId());
        bmaEp.setDns(ep.getDns());
        bmaEp.setPort(ep.getPort());
        bmaEp.setIpv4(ep.getIpv4());
        bmaEp.setIpv6(ep.getIpv6());
        bmaEp.setPath(ep.getPath());
        return bmaEp;
    }

    public static Peer setPeeringAndStats(Peer peer, NetworkPeering peeringDocument)  {
        Preconditions.checkNotNull(peer);
        Preconditions.checkNotNull(peeringDocument);

        Peer.Stats stats = peer.getStats() != null ? peer.getStats() : new Peer.Stats();
        Peer.Peering peering = (peer.getPeering() != null) ? peer.getPeering() : new Peer.Peering();

        // Copy some fields
        peer.setPubkey(peeringDocument.getPubkey());
        peer.setCurrency(peeringDocument.getCurrency());

        peering.setVersion(peeringDocument.getVersion());
        peering.setSignature(peeringDocument.getSignature());

        // Copy block infos
        String blockstamp = peeringDocument.getBlock();
        if (StringUtils.isNotBlank(blockstamp)) {
            String[] blockParts = blockstamp.split("-");
            if (blockParts.length == 2) {
                int blockNumber = Integer.parseInt(blockParts[0]);
                String blockHash = blockParts[1];

                // Fill peering block
                peering.setBlockNumber(blockNumber);
                peering.setBlockHash(blockHash);

                // use peering block as default stats (if empty)
                if (stats.getBlockNumber() == null) {
                    stats.setBlockNumber(blockNumber);
                    stats.setBlockHash(blockHash);
                }
            }
        }

        // Update peer status UP/DOWN
        if ("UP".equalsIgnoreCase(peeringDocument.getStatus())) {
            stats.setStatus(Peer.PeerStatus.UP);
            stats.setLastUpTime((long)Math.round(System.currentTimeMillis() / 1000));
        }
        else {
            stats.setStatus(Peer.PeerStatus.DOWN);
        }

        return peer;
    }
}

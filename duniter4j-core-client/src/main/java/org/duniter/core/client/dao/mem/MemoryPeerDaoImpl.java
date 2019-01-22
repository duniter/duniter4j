package org.duniter.core.client.dao.mem;

/*
 * #%L
 * UCoin Java :: Core Client API
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

import com.google.common.collect.ImmutableList;
import org.duniter.core.client.dao.PeerDao;
import org.duniter.core.client.model.bma.EndpointApi;
import org.duniter.core.client.model.bma.NetworkPeers;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.client.model.local.Peers;
import org.duniter.core.util.Preconditions;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by blavenie on 29/12/15.
 */
public class MemoryPeerDaoImpl implements PeerDao {

    private Map<String, Peer> peersByCurrencyId = new HashMap<>();

    public MemoryPeerDaoImpl() {
        super();
    }

    @Override
    public Peer create(Peer entity) {
        entity.setId(entity.computeKey());

        peersByCurrencyId.put(entity.getId(), entity);

        return entity;
    }

    @Override
    public Peer update(Peer entity) {
        peersByCurrencyId.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public Peer getById(String id) {
        return peersByCurrencyId.get(id);
    }

    @Override
    public void remove(Peer entity) {
        peersByCurrencyId.remove(entity.getId());
    }

    @Override
    public List<Peer> getPeersByCurrencyId(final String currencyId) {
        Preconditions.checkNotNull(currencyId);
        return peersByCurrencyId.values().stream()
            .filter(peer -> currencyId.equals(peer.getCurrency()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Peer> getPeersByCurrencyIdAndApi(final String currencyId, final String endpointApi) {
        Preconditions.checkNotNull(currencyId);
        Preconditions.checkNotNull(endpointApi);
        return peersByCurrencyId.values().stream()
                .filter(peer ->
                        // Filter on currency
                        currencyId.equals(peer.getCurrency()) &&
                        // Filter on API
                        peer.getApi() != null &&
                        endpointApi.equals(peer.getApi()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Peer> getPeersByCurrencyIdAndApiAndPubkeys(String currencyId, String endpointApi, String[] pubkeys) {
        Preconditions.checkNotNull(currencyId);
        Preconditions.checkNotNull(endpointApi);
        List pubkeysAsList = pubkeys != null ? ImmutableList.copyOf(pubkeys) : null;

        return peersByCurrencyId.values().stream()
                .filter(peer ->
                        // Filter on currency
                        currencyId.equals(peer.getCurrency()) &&
                        // Filter on API
                        (endpointApi == null || (
                                peer.getApi() != null &&
                                endpointApi.equals(peer.getApi()))
                        ) &&
                        // Filter on pubkeys
                        (pubkeysAsList == null || (
                                peer.getPubkey() != null &&
                                pubkeysAsList.contains(peer.getPubkey()))
                        ))
                .collect(Collectors.toList());
    }

    @Override
    public List<NetworkPeers.Peer> getBmaPeersByCurrencyId(String currencyId, String[] pubkeys) {
        Preconditions.checkNotNull(currencyId);

        return Peers.toBmaPeers(getPeersByCurrencyIdAndApiAndPubkeys(currencyId, null, pubkeys));
    }


    @Override
    public boolean isExists(final String currencyId, final  String peerId) {
        Preconditions.checkNotNull(currencyId);
        return peersByCurrencyId.values().stream()
                .anyMatch(peer -> currencyId.equals(peer.getCurrency()) && peerId.equals(peer.getId()));
    }

    @Override
    public Long getMaxLastUpTime(String currencyId) {
        Preconditions.checkNotNull(currencyId);
        OptionalLong max = getPeersByCurrencyId(currencyId).stream()
                .mapToLong(peer -> peer.getStats() != null ? peer.getStats().getLastUpTime() : -1)
                .max();

        if (!max.isPresent()) {
            return null;
        }
        return max.getAsLong();
    }

    @Override
    public void updatePeersAsDown(String currencyId, long upTimeLimitInSec) {

        getPeersByCurrencyId(currencyId).stream()
                .filter(peer -> peer.getStats() != null && peer.getStats().getLastUpTime() <= upTimeLimitInSec)
                .forEach(peer -> peer.getStats().setStatus(Peer.PeerStatus.DOWN));

    }

    @Override
    public boolean hasPeersUpWithApi(String currencyId, Set<EndpointApi> api) {
        return getPeersByCurrencyId(currencyId)
                .stream()
                .anyMatch(p ->
                    api.contains(EndpointApi.valueOf(p.getApi())) &&
                            p.getStats() != null &&
                            Peer.PeerStatus.UP.equals(p.getStats().getStatus())
                );
    }

    /* -- protected methods -- */

}

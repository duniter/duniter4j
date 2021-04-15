package org.duniter.core.client.repositories.mem;

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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.duniter.core.beans.InitializingBean;
import org.duniter.core.client.model.local.Currency;
import org.duniter.core.client.repositories.PeerRepository;
import org.duniter.core.client.model.bma.NetworkWs2pHeads;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.client.model.local.Peers;
import org.duniter.core.client.service.ServiceLocator;
import org.duniter.core.service.CryptoService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by blavenie on 29/12/15.
 */
public class MemoryPeerRepositoryImpl implements PeerRepository, InitializingBean, MemoryCrudRepository<String, Peer> {

    private Map<String, Peer> peersById = new HashMap<>();
    private CryptoService cryptoService;

    public MemoryPeerRepositoryImpl() {
        super();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        cryptoService = ServiceLocator.instance().getCryptoService();
    }

    @Override
    public <S extends Peer> S save(S entity) {
        String id = entity.getId();
        if (id == null) {
            id = Peers.computeHash(entity, cryptoService);
            entity.setId(id);
        }
        peersById.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public Optional<Peer> findById(String id) {
        return Optional.ofNullable(peersById.get(id));
    }

    @Override
    public Iterable<Peer> findAll() {
        return ImmutableList.copyOf(peersById.values());
    }

    @Override
    public long count() {
        return peersById.size();
    }

    @Override
    public void delete(Peer entity) {
        Preconditions.checkNotNull(entity);
        deleteById(entity.getId());
    }

    @Override
    public void deleteById(String id) {
        peersById.remove(id);
    }

    @Override
    public List<Peer> getPeersByCurrencyId(final String currency) {
        Preconditions.checkNotNull(currency);
        return peersById.values().stream()
            .filter(peer -> currency.equals(peer.getCurrency()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Peer> getPeersByCurrencyIdAndApi(final String currency, final String endpointApi) {
        Preconditions.checkNotNull(currency);
        Preconditions.checkNotNull(endpointApi);
        return peersById.values().stream()
                .filter(peer ->
                        // Filter on currency
                        currency.equals(peer.getCurrency()) &&
                        // Filter on API
                        peer.getApi() != null &&
                        endpointApi.equals(peer.getApi()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Peer> getPeersByCurrencyIdAndApiAndPubkeys(String currencyId, String endpointApi, String... pubkeys) {
        Preconditions.checkNotNull(currencyId);
        Preconditions.checkNotNull(endpointApi);
        List pubkeysAsList = pubkeys != null ? Arrays.asList(pubkeys) : null;

        return peersById.values().stream()
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
    public List<Peer> getUpPeersByCurrencyId(String currencyId, String... pubkeys) {
        Preconditions.checkNotNull(currencyId);

        return getPeersByCurrencyIdAndApiAndPubkeys(currencyId, null, pubkeys)
                .stream()
                .filter(Peers::isReacheable)
                .collect(Collectors.toList());
    }

    @Override
    public List<NetworkWs2pHeads.Head> getWs2pPeersByCurrencyId(String currencyId, String... pubkeys) {
        Preconditions.checkNotNull(currencyId);

        return getPeersByCurrencyIdAndApiAndPubkeys(currencyId, null, pubkeys)
                .stream()
                .map(Peers::toWs2pHead)
                // Skip if no message
                .filter(head -> head.getMessage() != null)
                .collect(Collectors.toList());
    }


    @Override
    public boolean existsById(final String id) {
        Preconditions.checkNotNull(id);
        return peersById.containsKey(id);
    }

    @Override
    public boolean existsByCurrencyAndId(final String currency, final  String id) {
        Preconditions.checkNotNull(currency);
        return peersById.values().stream()
                .anyMatch(peer -> currency.equals(peer.getCurrency()) && id.equals(peer.getId()));
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
    public void updatePeersAsDown(String currencyId, long minUpTimeInMs, Collection<String> endpointApis) {

        long minUpTimeInSec = minUpTimeInMs / 1000L;
        long firstDownTime = System.currentTimeMillis() / 1000L;

        getPeersByCurrencyId(currencyId).stream()
                .filter(peer ->
                        peer.getStats() != null
                        && peer.getStats().isReacheable()
                        && (
                                peer.getStats().getLastUpTime() == null
                                || peer.getStats().getLastUpTime() < minUpTimeInSec
                        )
                        && (endpointApis == null || endpointApis.contains(peer.getApi()))
                )
                .forEach(peer -> {
                    peer.getStats().setStatus(Peer.PeerStatus.DOWN);
                    peer.getStats().setFirstDownTime(firstDownTime);
                });
    }

    @Override
    public boolean hasPeersUpWithApi(String currencyId, Set<String> api) {
        return getPeersByCurrencyId(currencyId)
                .stream()
                .anyMatch(p ->
                    api.contains(p.getApi()) &&
                            p.getStats() != null &&
                            Peer.PeerStatus.UP.equals(p.getStats().getStatus())
                );
    }

    /* -- protected methods -- */

}

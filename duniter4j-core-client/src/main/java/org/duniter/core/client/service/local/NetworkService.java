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

import org.duniter.core.beans.Service;
import org.duniter.core.client.model.local.Peer;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;

/**
 * Created by blavenie on 20/03/17.
 */
public interface NetworkService extends Service {

    interface PeersChangeListener {
        void onChanges(Collection<Peer> peers);

    }

    class Sort {
        public SortType sortType;
        public boolean sortAsc;
    }

    class Filter {
        public FilterType filterType;
        public Peer.PeerStatus filterStatus;
        public Boolean filterSsl;
        public List<String> filterEndpoints;
        public String currency;
    }


    enum SortType {
        UID,
        PUBKEY,
        API,
        HARDSHIP,
        BLOCK_NUMBER
    }

    enum FilterType {
        MEMBER, // Only members peers
        MIRROR // Only mirror peers
    }

    List<Peer> getPeers(Peer mainPeer);

    List<Peer> getPeers(Peer mainPeer, Filter filter, Sort sort);

    List<Peer> getPeers(Peer mainPeer, Filter filter, Sort sort, ExecutorService pool);

    CompletableFuture<List<Peer>> getPeersAsync(Peer mainPeer, List<String> filterEndpoints, ExecutorService pool) throws ExecutionException, InterruptedException;

    List<Peer> fillPeerStatsConsensus(final List<Peer> peers);

    Predicate<Peer> peerFilter(Filter filter);

    Comparator<Peer> peerComparator(Sort sort);

    void addPeersChangeListener(final Peer mainPeer, final PeersChangeListener listener);

    void addPeersChangeListener(final Peer mainPeer, final PeersChangeListener listener,
                                final Filter filter, final Sort sort, final boolean autoreconnect,
                                final ExecutorService executor);

    CompletableFuture<List<Peer>> refreshPeersAsync(final Peer mainPeer, final List<Peer> peers, final ExecutorService pool);

    String getVersion(final Peer peer);
}

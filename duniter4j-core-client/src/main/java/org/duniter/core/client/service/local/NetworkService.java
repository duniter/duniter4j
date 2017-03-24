package org.duniter.core.client.service.local;

import org.duniter.core.beans.Service;
import org.duniter.core.client.model.local.Peer;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;

/**
 * Created by blavenie on 20/03/17.
 */
public interface NetworkService extends Service {

    class Sort {
        public SortType sortType;
        public boolean sortAsc;
    }

    class Filter {
        public FilterType filterType;
        public Peer.PeerStatus filterStatus;
        public Boolean filterSsl;
        public List<String> filterEndpoints;
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

    CompletableFuture<List<CompletableFuture<Peer>>> asyncGetPeers(Peer mainPeer, ExecutorService pool) throws ExecutionException, InterruptedException;

    List<Peer> fillPeerStatsConsensus(final List<Peer> peers);

    Predicate<Peer> peerFilter(Filter filter);

    Comparator<Peer> peerComparator(Sort sort);


}

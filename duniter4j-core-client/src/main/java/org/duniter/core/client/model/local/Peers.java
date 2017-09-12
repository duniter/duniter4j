package org.duniter.core.client.model.local;

import org.duniter.core.client.model.bma.EndpointApi;

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
        return stats.getStatus() == Peer.PeerStatus.UP
                ? stats.getBlockNumber() + "-" + stats.getBlockHash()
                : null;
    }
}

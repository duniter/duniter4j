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
        return stats.getStatus() == Peer.PeerStatus.UP && stats.getBlockNumber() != null
                ? stats.getBlockNumber() + "-" + stats.getBlockHash()
                : null;
    }

    public static boolean hasBmaEndpoint(Peer peer) {
        return hasEndPointAPI(peer, EndpointApi.BASIC_MERKLED_API) ||
               hasEndPointAPI(peer, EndpointApi.BMAS);
    }

    public static boolean hasEsCoreEndpoint(Peer peer) {
        return hasEndPointAPI(peer, EndpointApi.ES_CORE_API);
    }
}

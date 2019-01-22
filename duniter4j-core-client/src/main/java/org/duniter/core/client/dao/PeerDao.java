package org.duniter.core.client.dao;

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

import org.duniter.core.client.model.bma.EndpointApi;
import org.duniter.core.client.model.bma.NetworkPeers;
import org.duniter.core.client.model.local.Peer;

import java.util.List;
import java.util.Set;

/**
 * Created by blavenie on 29/12/15.
 */
public interface PeerDao extends EntityDao<String, Peer> {

    List<Peer> getPeersByCurrencyId(String currencyId);

    List<Peer> getPeersByCurrencyIdAndApi(String currencyId, String endpointApi);

    /**
     *
     * @param currencyId
     * @param endpointApi
     * @param pubkeys
     * @return
     */
    List<Peer> getPeersByCurrencyIdAndApiAndPubkeys(String currencyId, String endpointApi, String[] pubkeys);

    /**
     * Get peers as BMA /network/peers format
     * @param currencyId
     * @param pubkeys use to filter on specific pubkeys. If null, not filtering
     * @return
     */
    List<NetworkPeers.Peer> getBmaPeersByCurrencyId(String currencyId, String[] pubkeys);

    boolean isExists(String currencyId, String peerId);

    Long getMaxLastUpTime(String currencyId);

    void updatePeersAsDown(String currencyId, long upTimeLimitInSec);

    boolean hasPeersUpWithApi(String currencyId, Set<EndpointApi> api);
}

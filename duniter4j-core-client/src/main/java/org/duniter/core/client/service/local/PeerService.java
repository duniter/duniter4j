package org.duniter.core.client.service.local;

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

import org.duniter.core.beans.Service;
import org.duniter.core.client.model.local.Peer;

import java.util.Collection;
import java.util.List;

/**
 * Created by eis on 07/02/15.
 */
public interface PeerService extends Service {

    Peer save(final Peer peer);

    /**
     * Return a (cached) active peer, by currency id
     * @param currencyId
     * @return
     */
    Peer getActivePeerByCurrencyId(String currencyId);

    /**
     * Save the active (default) peer, for a given currency id
     * @param currencyId
     * @param peer
     */
    void setCurrencyMainPeer(String currency, Peer peer);

    /**
     * Return a (cached) peer list, by currency id
     * @param currencyId
     * @return
     */
    List<Peer> getPeersByCurrencyId(String currencyId);

    void save(String currencyId, List<Peer> peers);

    void updatePeersAsDown(String currencyId, Collection<String> filterApis);

    void updatePeersAsDown(String currencyId, long maxUpTimeInSec, Collection<String> filterApis);

    boolean isExists(String currencyId, String peerId);
}

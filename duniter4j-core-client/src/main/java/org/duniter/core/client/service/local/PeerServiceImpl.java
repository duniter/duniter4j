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

import org.duniter.core.beans.InitializingBean;
import org.duniter.core.client.dao.PeerDao;
import org.duniter.core.client.model.local.Currency;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.client.service.ServiceLocator;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.util.CollectionUtils;
import org.duniter.core.util.ObjectUtils;
import org.duniter.core.util.Preconditions;
import org.duniter.core.util.StringUtils;
import org.duniter.core.util.cache.Cache;
import org.duniter.core.util.cache.SimpleCache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by eis on 07/02/15.
 */
public class PeerServiceImpl implements PeerService, InitializingBean {

    private Cache<Long, List<Peer>> peersByCurrencyIdCache;
    private Cache<Long, Peer> activePeerByCurrencyIdCache;

    private CurrencyService currencyService;
    private PeerDao peerDao;

    public PeerServiceImpl() {
        super();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        currencyService = ServiceLocator.instance().getCurrencyService();
        peerDao = ServiceLocator.instance().getBean(PeerDao.class);
    }

    @Override
    public void close() throws IOException {
        currencyService = null;
        peerDao = null;
        peersByCurrencyIdCache = null;
        activePeerByCurrencyIdCache = null;
    }

    public Peer save(final Peer peer) {
        Preconditions.checkNotNull(peer);
        Preconditions.checkNotNull(peer.getCurrencyId());
        Preconditions.checkArgument(StringUtils.isNotBlank(peer.getHost()));
        Preconditions.checkArgument(peer.getPort() >= 0);

        Peer result;
        // Create
        if (peer.getId() == null) {
            result = peerDao.create(peer);
        }

        // or update
        else {
            peerDao.update(peer);
            result = peer;
        }

        // update cache (if already loaded)
        if (peersByCurrencyIdCache != null) {
            List<Peer> peers = peersByCurrencyIdCache.get(peer.getCurrencyId());
            if (peers == null) {
                peers = new ArrayList<Peer>();
                peersByCurrencyIdCache.put(peer.getCurrencyId(), peers);
                peers.add(peer);
            }
            else if (!peers.contains(peer)) {
                peers.add(peer);
            }
        }

        return result;
    }


    public Peer getPeerById(long peerId) {
        return peerDao.getById(peerId);
    }

    /**
     * Return a (cached) active peer, by currency id
     * @param currencyId
     * @return
     */
    public Peer getActivePeerByCurrencyId(long currencyId) {
        // Check if cache as been loaded
        if (activePeerByCurrencyIdCache == null) {

            activePeerByCurrencyIdCache = new SimpleCache<Long, Peer>() {
                @Override
                public Peer load(Long currencyId) {
                    List<Peer> peers = peerDao.getPeersByCurrencyId(currencyId);
                    if (CollectionUtils.isEmpty(peers)) {
                        String currencyName = currencyService.getCurrencyNameById(currencyId);
                        throw new TechnicalException(String.format(
                                "No peers configure for currency [%s]",
                                currencyName != null ? currencyName : currencyId));
                    }

                    return peers.get(0);
                }
            };
        }

        return activePeerByCurrencyIdCache.get(currencyId);
    }

    /**
     * Return a (cached) peer list, by currency id
     * @param currencyId
     * @return
     */
    public List<Peer> getPeersByCurrencyId(long currencyId) {
        // Check if cache as been loaded
        if (peersByCurrencyIdCache == null) {
            throw new TechnicalException("Cache not initialize. Please call loadCache() before getPeersByCurrencyId().");
        }
        // Get it from cache
        return peersByCurrencyIdCache.get(currencyId);
    }

    /**
     * Fill all cache need for currencies
     * @param accountId
     */
    public void loadCache(long accountId) {
        if (peersByCurrencyIdCache != null) {
            return;
        }

        peersByCurrencyIdCache = new SimpleCache<Long, List<Peer>>() {
            @Override
            public List<Peer> load(Long currencyId) {
                return peerDao.getPeersByCurrencyId(currencyId);
            }
        };

        List<Currency> currencies = ServiceLocator.instance().getCurrencyService().getCurrencies(accountId);

        for (Currency currency: currencies) {
            // Get peers from DB
            List<Peer> peers = getPeersByCurrencyId(currency.getId());

            // Then fill the cache
            if (CollectionUtils.isNotEmpty(peers)) {
                peersByCurrencyIdCache.put(currency.getId(), peers);
            }
        }
    }

}

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
import org.duniter.core.client.config.Configuration;
import org.duniter.core.client.dao.PeerDao;
import org.duniter.core.client.model.local.Currency;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.client.service.ServiceLocator;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.service.CryptoService;
import org.duniter.core.util.CollectionUtils;
import org.duniter.core.util.Preconditions;
import org.duniter.core.util.StringUtils;
import org.duniter.core.util.cache.Cache;
import org.duniter.core.util.cache.SimpleCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Created by eis on 07/02/15.
 */
public class PeerServiceImpl implements PeerService, InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(PeerServiceImpl.class);
    private Cache<String, List<Peer>> peersByCurrencyIdCache;
    private Cache<String, Peer> activePeerByCurrencyIdCache;

    private CurrencyService currencyService;
    private CryptoService cryptoService;
    private PeerDao peerDao;
    private Configuration config;

    public PeerServiceImpl() {
        super();

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.currencyService = ServiceLocator.instance().getCurrencyService();
        this.peerDao = ServiceLocator.instance().getBean(PeerDao.class);
        this.config = Configuration.instance();
        this.cryptoService = ServiceLocator.instance().getCryptoService();
        this.activePeerByCurrencyIdCache = new SimpleCache<String, Peer>() {
            @Override
            public Peer load(String currencyId) {
                return loadDefaultPeer(currencyId);
            }
        };
    }

    @Override
    public void close() throws IOException {
        currencyService = null;
        peerDao = null;
        peersByCurrencyIdCache = null;
        activePeerByCurrencyIdCache = null;
        cryptoService = null;
    }


    public Peer save(final Peer peer) {
        Preconditions.checkNotNull(peer);
        Preconditions.checkNotNull(peer.getCurrency());
        Preconditions.checkArgument(StringUtils.isNotBlank(peer.getHost()));
        Preconditions.checkArgument(peer.getPort() >= 0);


        String peerId = cryptoService.hash(peer.computeKey());
        boolean exists = isExists(peer.getCurrency(), peerId);
        peer.setId(peerId);

        Peer result;
        // Create
        if (!exists) {
            result = peerDao.create(peer);
        }

        // or update
        else {
            peerDao.update(peer);
            result = peer;
        }

        // update cache (if already loaded)
        if (peersByCurrencyIdCache != null) {
            List<Peer> peers = peersByCurrencyIdCache.get(peer.getCurrency());
            if (peers == null) {
                peers = new ArrayList<>();
                peersByCurrencyIdCache.put(peer.getCurrency(), peers);
                peers.add(peer);
            } else if (!peers.contains(peer)) {
                peers.add(peer);
            }
        }

        return result;
    }

    /**
     * Return a (cached) active peer, by currency id
     *
     * @param currencyId
     * @return
     */
    public Peer getActivePeerByCurrencyId(String currencyId) {
        return activePeerByCurrencyIdCache.get(currencyId);
    }

    @Override
    public void setCurrencyMainPeer(String currencyId, Peer peer) {
        activePeerByCurrencyIdCache.put(currencyId, peer);
    }

    /**
     * Return a (cached) peer list, by currency id
     *
     * @param currencyId
     * @return
     */
    public List<Peer> getPeersByCurrencyId(String currencyId) {
        // Check if cache as been loaded
        if (peersByCurrencyIdCache == null) {
            throw new TechnicalException("Cache not initialize. Please call loadCache() before getPeersByCurrencyId().");
        }
        // Get it from cache
        return peersByCurrencyIdCache.get(currencyId);
    }

    /**
     * Fill allOfToList cache need for currencies
     *
     * @param accountId
     */
    public void loadCache(long accountId) {
        if (peersByCurrencyIdCache != null) {
            return;
        }

        peersByCurrencyIdCache = new SimpleCache<String, List<Peer>>() {
            @Override
            public List<Peer> load(String currencyId) {
                return peerDao.getPeersByCurrencyId(currencyId);
            }
        };

        List<Currency> currencies = ServiceLocator.instance().getCurrencyService().getAllByAccount(accountId);

        for (Currency currency: currencies) {
            // Get peers from DB
            List<Peer> peers = getPeersByCurrencyId(currency.getId());

            // Then fill the cache
            if (CollectionUtils.isNotEmpty(peers)) {
                peersByCurrencyIdCache.put(currency.getId(), peers);
            }
        }
    }

    @Override
    public void save(String currencyId, List<Peer> peers) {


        if (CollectionUtils.isNotEmpty(peers)) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("[%s] Updating peers (%s endpoints found)", currencyId, peers.size()));
            }

            // FIXME: Duniter 1.7 return lastUpTime in ms. Check if this a bug or not
            final long upTime = System.currentTimeMillis() / 1000;

            peers.forEach(peer -> {
                // On each UP peers: set last UP time
                if (peer.getStats() != null && peer.getStats().isReacheable()) {
                    peer.getStats().setLastUpTime(upTime);
                    peer.getStats().setFirstDownTime(null);
                }
                // Save
                save(peer);
            });
        }
    }

    @Override
    public boolean isExists(String currencyId, String peerId) {
        return peerDao.isExists(currencyId, peerId);
    }

    @Override
    public void updatePeersAsDown(String currencyId, Collection<String> filterApis) {
        int peerDownTimeoutMs = config.getPeerUpMaxAge();
        // Mark old peers as DOWN
        if (peerDownTimeoutMs >0) {
            long maxUpTimeInMs = System.currentTimeMillis() - peerDownTimeoutMs;
            updatePeersAsDown(currencyId, maxUpTimeInMs, filterApis);
        }
    }

    @Override
    public void updatePeersAsDown(String currencyId, long minUpTimeInMs, Collection<String> filterApis) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("[%s] %s Setting peers as DOWN, if older than [%s]...", currencyId, filterApis, new Date(minUpTimeInMs *1000)));
        }
        peerDao.updatePeersAsDown(currencyId, minUpTimeInMs, filterApis);
    }

    protected Peer loadDefaultPeer(String currencyId) {
        List<Peer> peers = peerDao.getPeersByCurrencyId(currencyId);
        if (CollectionUtils.isEmpty(peers)) {
            throw new TechnicalException(String.format(
                    "No peers configure for currency [%s]",
                    currencyId));
        }

        Peer defaultPeer = peers.stream()
                .filter(peer -> peer.getStats() == null || peer.getStats().getStatus() == null || peer.getStats().getStatus() == Peer.PeerStatus.UP)
                .findFirst().orElse(null);
        if (defaultPeer != null) {
            // Make sure currency is filled
            defaultPeer.setCurrency(currencyId);
        }
        else {
            log.warn(String.format("[%s] No default peer found. Unable to send remote request.", currencyId));
        }

        return defaultPeer;
    }

}

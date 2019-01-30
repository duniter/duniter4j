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
import org.duniter.core.client.dao.CurrencyDao;
import org.duniter.core.client.model.local.Currency;
import org.duniter.core.client.service.ServiceLocator;
import org.duniter.core.client.service.bma.BlockchainRemoteService;
import org.duniter.core.util.CollectionUtils;
import org.duniter.core.util.ObjectUtils;
import org.duniter.core.util.Preconditions;
import org.duniter.core.util.StringUtils;
import org.duniter.core.util.cache.Cache;
import org.duniter.core.util.cache.SimpleCache;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Created by eis on 07/02/15.
 */
public class CurrencyServiceImpl implements CurrencyService, InitializingBean {


    private static final long UD_CACHE_TIME_MILLIS = 5 * 60 * 1000; // = 5 min

    private Cache<String, Currency> mCurrencyCache;
    private Cache<String, Long> mUDCache;

    private BlockchainRemoteService blockchainRemoteService;
    private CurrencyDao currencyDao;

    public CurrencyServiceImpl() {
        super();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        blockchainRemoteService = ServiceLocator.instance().getBlockchainRemoteService();
        currencyDao = ServiceLocator.instance().getBean(CurrencyDao.class);

        // Load cache
        initCaches();
    }

    @Override
    public void close() throws IOException {
        currencyDao = null;
        blockchainRemoteService = null;
    }

    public Currency save(final Currency currency) {
        Preconditions.checkNotNull(currency);
        Preconditions.checkArgument(StringUtils.isNotBlank(currency.getId()));
        Preconditions.checkArgument(StringUtils.isNotBlank(currency.getFirstBlockSignature()));
        Preconditions.checkNotNull(currency.getMembersCount());
        Preconditions.checkArgument(currency.getMembersCount().intValue() >= 0);
        Preconditions.checkNotNull(currency.getLastUD());
        Preconditions.checkArgument(currency.getLastUD().longValue() > 0);

        Currency result;

        // Create
        if (currency.getId() == null) {
            result = currencyDao.create(currency);

            // Update the cache (if already initialized)
            if (mCurrencyCache != null) {
                mCurrencyCache.put(currency.getId(), currency);
            }
        }

        // or update
        else {
            currencyDao.update(currency);

            result = currency;
        }

        return result;
    }

    public List<Currency> getAllByAccount(long accountId) {
        return currencyDao.getAllByAccount(accountId);
    }

    public List<Currency> getAll() {
        Set<String> ids = currencyDao.getAllIds();
        return ids.stream()
                .map(id -> getById(id))
                .collect(Collectors.toList());
    }

    public Currency getById(String currencyId) {
        return mCurrencyCache.get(currencyId);
    }

    /**
     * Return a (cached) currency name, by id
     * @param currencyId
     * @return
     */
    public String getNameById(String currencyId) {
        Currency currency = mCurrencyCache != null ? mCurrencyCache.getIfPresent(currencyId) : null;
        if (currency == null) {
            return null;
        }
        return currency.getId();
    }

    /**
     * Return a currency id, by name
     * @param currencyName
     * @return
     */
    public String getIdByName(String currencyName) {
        Preconditions.checkArgument(StringUtils.isNotBlank(currencyName));

        // Search from currencies
        for (Map.Entry<String, Currency> entry : mCurrencyCache.entrySet()) {
            Currency currency = entry.getValue();
            if (ObjectUtils.equals(currencyName, currency.getId())) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Return a (cached) list of currency ids
     * @return
     */
    public Set<String> getAllIds() {
        Set<String> ids = mCurrencyCache.keySet();
        if (CollectionUtils.isEmpty(ids)) {
            ids = currencyDao.getAllIds();
        }
        return ids;
    }

    /**
     * Return a (cached) number of registered currencies
     * @return
     */
    public int count() {
        return mCurrencyCache.entrySet().size();
    }

    /**
     * Fill allOfToList cache need for currencies
     */
    public void initCaches() {
        if (mCurrencyCache != null && mUDCache != null) return;

        // Create and fill the currency cache
        if (mCurrencyCache == null) {

            mCurrencyCache = new SimpleCache<String, Currency>() {
                @Override
                public Currency load(String currencyId) {
                    return currencyDao.getById(currencyId);
                }
            };

            // Fill cache for the configured account
            long accountId = ServiceLocator.instance().getDataContext().getAccountId();
            List<Currency> currencies = (accountId != -1) ? getAllByAccount(accountId) : getAll();
            for (Currency currency : currencies) {
                mCurrencyCache.put(currency.getId(), currency);
            }
        }

        // Create the UD cache
        if (mUDCache == null) {

            mUDCache = new SimpleCache<String, Long>(UD_CACHE_TIME_MILLIS) {
                @Override
                public Long load(final String currencyId) {
                    // Retrieve the last UD from the blockchain
                    final Long lastUD = blockchainRemoteService.getLastUD(currencyId);

                    // Update currency
                    Currency currency = getById(currencyId);
                    if (!ObjectUtils.equals(currency.getLastUD(), lastUD)) {
                        currency.setLastUD(lastUD);
                        currencyDao.update(currency);
                    }

                    return lastUD;
                }
            };
        }
    }

    /**
     * Return the value of the last universal dividend
     * @param currencyId
     * @return
     */
    public long getLastUD(String currencyId) {
        return mUDCache.get(currencyId);
    }

    /**
     * Return a map of UD (key=blockNumber, value=amount)
     * @return
     */
    public Map<Integer, Long> refreshAndGetUD(String currencyId, long lastSyncBlockNumber) {

        // Retrieve new UDs from blockchain
        Map<Integer, Long> newUDs = blockchainRemoteService.getUDs(currencyId, lastSyncBlockNumber + 1);

        // If any, insert new into DB
        if (newUDs != null && newUDs.size() > 0) {
            currencyDao.insertUDs(currencyId, newUDs);
        }

        return getAllUD(currencyId);
    }

    /**
     * Return a map of UD (key=blockNumber, value=amount)
     * @return
     */
    public Map<Integer, Long> getAllUD(String currencyId) {
        return currencyDao.getAllUD(currencyId);
    }

}

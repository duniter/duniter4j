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
import org.duniter.core.client.model.local.Currency;
import org.duniter.core.client.repositories.CurrencyRepository;
import org.duniter.core.client.service.ServiceLocator;
import org.duniter.core.util.CollectionUtils;
import org.duniter.core.util.Preconditions;
import org.duniter.core.util.StringUtils;
import org.duniter.core.util.cache.Cache;
import org.duniter.core.util.cache.SimpleCache;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;


/**
 * Created by eis on 07/02/15.
 */
public class CurrencyServiceImpl implements CurrencyService, InitializingBean {

    private Cache<String, Optional<Currency>> mCurrencyCache;

    private CurrencyRepository<Currency> currencyRepository;

    public CurrencyServiceImpl() {
        super();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        currencyRepository = ServiceLocator.instance().getBean(CurrencyRepository.class);

        // Load cache
        initCaches();
    }

    @Override
    public void close() throws IOException {
        currencyRepository = null;
    }

    public Currency save(final Currency currency) {
        Preconditions.checkNotNull(currency);
        Preconditions.checkArgument(StringUtils.isNotBlank(currency.getId()));
        Preconditions.checkArgument(StringUtils.isNotBlank(currency.getFirstBlockSignature()));
        Preconditions.checkNotNull(currency.getMembersCount());
        Preconditions.checkArgument(currency.getMembersCount() >= 0);
        Preconditions.checkNotNull(currency.getDividend());
        Preconditions.checkArgument(currency.getDividend() > 0);

        Currency result = currencyRepository.save(currency);

        // Update the cache (if already initialized)
        if (mCurrencyCache != null) {
            mCurrencyCache.put(currency.getId(), Optional.of(currency));
        }

        return result;
    }

    public Iterable<Currency> findAll() {
        return currencyRepository.findAll();
    }

    public Optional<Currency> findById(String id) {
        return mCurrencyCache.get(id);
    }

    /**
     * Return a (cached) list of currency ids
     * @return
     */
    public Iterable<String> findAllIds() {
        Set<String> ids = mCurrencyCache.keySet();
        if (CollectionUtils.isNotEmpty(ids)) return ids;

        return currencyRepository.findAllIds();
    }

    /**
     * Return a (cached) number of registered currencies
     * @return
     */
    public long count() {
        return currencyRepository.count();
    }

    @Override
    public boolean existsById(String id) {
        return currencyRepository.existsById(id);
    }


    /**
     * Fill allOfToList cache need for currencies
     */
    protected void initCaches() {
        // Create and fill the currency cache
        if (mCurrencyCache == null) {

            mCurrencyCache = new SimpleCache<String, Optional<Currency>>() {
                @Override
                public Optional<Currency> load(String id) {
                    return currencyRepository.findById(id);
                }
            };

            // Load cache
            for (Currency currency: findAll()) {
                mCurrencyCache.put(currency.getId(), Optional.of(currency));
            }
        }
    }

}

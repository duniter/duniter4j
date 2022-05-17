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

import org.apache.commons.collections4.MapUtils;
import org.duniter.core.beans.InitializingBean;
import org.duniter.core.client.model.local.Currency;
import org.duniter.core.client.model.local.Dividend;
import org.duniter.core.client.repositories.CurrencyRepository;
import org.duniter.core.client.repositories.DividendRepository;
import org.duniter.core.client.service.ServiceLocator;
import org.duniter.core.client.service.bma.BlockchainRemoteService;
import org.duniter.core.util.Beans;
import org.duniter.core.util.ObjectUtils;
import org.duniter.core.util.cache.Cache;
import org.duniter.core.util.cache.SimpleCache;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * Created by eis on 14/04/21.
 */
public class DividendServiceImpl implements DividendService, InitializingBean {


    private static final long UD_CACHE_TIME_MILLIS = 5 * 60 * 1000; // = 5 min

    private Cache<String, Long> lastUdByCurrencyCache;

    private BlockchainRemoteService blockchainRemoteService;
    private DividendRepository dividendRepository;
    private CurrencyRepository<Currency> currencyRepository;

    public DividendServiceImpl() {
        super();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        blockchainRemoteService = ServiceLocator.instance().getBlockchainRemoteService();
        currencyRepository = ServiceLocator.instance().getBean(CurrencyRepository.class);
        dividendRepository = ServiceLocator.instance().getBean(DividendRepository.class);

        // Load cache
        initCaches();
    }

    @Override
    public void close() throws IOException {
        blockchainRemoteService = null;
        currencyRepository = null;
        dividendRepository = null;
    }

    /**
     * Fill allOfToList cache need for currencies
     */
    public void initCaches() {
        // Create the UD cache
        if (lastUdByCurrencyCache == null) {

            lastUdByCurrencyCache = new SimpleCache<String, Long>(UD_CACHE_TIME_MILLIS) {
                @Override
                public Long load(final String currency) {
                    // Retrieve the last UD from the blockchain
                    final Long lastUD = blockchainRemoteService.getLastDividend(currency);

                    // Update currency
                    if (lastUD != null) {
                        currencyRepository.findById(currency)
                                .filter(currencyEntity -> !ObjectUtils.equals(currencyEntity.getDividend(), lastUD))
                                .ifPresent(currencyEntity -> {
                                    currencyEntity.setDividend(lastUD);
                                    currencyRepository.save(currencyEntity);
                                });
                    }

                    return lastUD;
                }
            };
        }
    }

    /**
     * Return the value of the last universal dividend
     * @param currency
     * @return
     */
    public Optional<Long> findLastDividendByCurrency(String currency) {
        return Optional.ofNullable(lastUdByCurrencyCache.get(currency));
    }

    /**
     * Return a map of UD (key=blockNumber, value=amount)
     * @return
     */
    public Map<Integer, Long> refreshAndGetDividends(String currency, long lastSyncBlockNumber) {

        // Retrieve new UDs from blockchain
        Map<Integer, Long> newUDs = blockchainRemoteService.getUDs(currency, lastSyncBlockNumber + 1);

        // If any, insert new into DB
        if (MapUtils.isNotEmpty(newUDs)) {
            List<Dividend> dividends = newUDs.entrySet().stream()
                    .map(e -> Dividend.builder()
                            .number(e.getKey())
                            .dividend(e.getValue())
                    .build()).collect(Collectors.toList());
            dividendRepository.saveAll(dividends);

            // Update currency's last UD
            Long lastDividend = dividends.stream().max(Comparator.comparing(Dividend::getNumber)).get().getDividend();
            updateLastDividendByCurrency(currency, lastDividend);
        }

        // Return the full list
        return findAllDividendsByCurrency(currency);
    }

    /**
     * Return a map of UD (key=blockNumber, value=amount)
     * @return
     */
    public Map<Integer, Long> findAllDividendsByCurrency(String currency) {
        return Beans.getStream(dividendRepository.findAllByCurrency(currency))
                .collect(Collectors.toMap(Dividend::getNumber, Dividend::getDividend));
    }

    @Override
    public void updateLastDividendByCurrency(String currency, Long dividend) {
        currencyRepository.findById(currency)
                .filter(currencyEntity -> !ObjectUtils.equals(currencyEntity.getDividend(), dividend))
                .ifPresent(currencyEntity -> {
                    currencyEntity.setDividend(dividend);
                    currencyRepository.save(currencyEntity);
                });

        // Update UD cache
        lastUdByCurrencyCache.put(currency, dividend);
    }
}

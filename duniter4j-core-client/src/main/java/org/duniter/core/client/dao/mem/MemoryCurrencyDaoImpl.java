package org.duniter.core.client.dao.mem;

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

import org.duniter.core.client.dao.CurrencyDao;

import java.util.*;

/**
 * Created by blavenie on 29/12/15.
 */
public class MemoryCurrencyDaoImpl implements CurrencyDao {


    private Map<Long, org.duniter.core.client.model.local.Currency> currencies = new HashMap<>();

    private Map<Long, Map<Integer, Long>> currencyUDsByBlock = new HashMap<>();

    @Override
    public org.duniter.core.client.model.local.Currency create(final org.duniter.core.client.model.local.Currency entity) {

        long id = getMaxId() + 1;
        entity.setId(id);

        currencies.put(id, entity);

        return entity;
    }

    @Override
    public org.duniter.core.client.model.local.Currency update(final org.duniter.core.client.model.local.Currency currency) {
        currencies.put(currency.getId(), currency);
        return currency;
    }

    @Override
    public void remove(final org.duniter.core.client.model.local.Currency currency) {
        currencies.remove(currency.getId());
    }

    @Override
    public List<org.duniter.core.client.model.local.Currency> getCurrencies(long accountId) {
        List<org.duniter.core.client.model.local.Currency> result = new ArrayList<>();
        result.addAll(currencies.values());
        return result;
    }

    @Override
    public org.duniter.core.client.model.local.Currency getById(long currencyId) {
        return currencies.get(currencyId);
    }

    @Override
    public String getCurrencyNameById(long currencyId) {
        org.duniter.core.client.model.local.Currency currency = getById(currencyId);
        if (currency == null) {
            return null;
        }
        return currency.getCurrencyName();
    }

    @Override
    public Long getCurrencyIdByName(String currencyName) {
        for(org.duniter.core.client.model.local.Currency currency: currencies.values()) {
            if (currencyName.equalsIgnoreCase(currency.getCurrencyName())) {
                return currency.getId();
            }
        }
        return null;
    }

    @Override
    public Set<Long> getCurrencyIds() {
        return currencies.keySet();
    }

    @Override
    public int getCurrencyCount() {
        return currencies.size();
    }

    @Override
    public long getLastUD(long currencyId) {
        org.duniter.core.client.model.local.Currency currency = getById(currencyId);
        if (currency == null) {
            return -1;
        }
        return currency.getLastUD();
    }

    @Override
    public Map<Integer, Long> getAllUD(long currencyId) {

        return currencyUDsByBlock.get(currencyId);
    }

    @Override
    public void insertUDs(Long currencyId,  Map<Integer, Long> newUDs) {
        Map<Integer, Long> udsByBlock = currencyUDsByBlock.get(currencyId);
        if (udsByBlock == null) {
            udsByBlock = new HashMap<>();
            currencyUDsByBlock.put(currencyId, udsByBlock);
        }
        udsByBlock.putAll(newUDs);
    }

    /* -- internal methods -- */

    protected long getMaxId() {
        long currencyId = -1;
        for (Long anId : currencies.keySet()) {
            if (anId > currencyId) {
                currencyId = anId;
            }
        }

        return currencyId;
    }
}

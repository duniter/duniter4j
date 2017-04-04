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


    private Map<String, org.duniter.core.client.model.local.Currency> currencies = new HashMap<>();

    private Map<String, Map<Integer, Long>> currencyUDsByBlock = new HashMap<>();

    public MemoryCurrencyDaoImpl() {
        super();
    }

    @Override
    public org.duniter.core.client.model.local.Currency create(final org.duniter.core.client.model.local.Currency entity) {

        currencies.put(entity.getId(), entity);

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
    public org.duniter.core.client.model.local.Currency getById(String id) {
        return currencies.get(id);
    }

    @Override
    public long getLastUD(String id) {
        org.duniter.core.client.model.local.Currency currency = getById(id);
        if (currency == null) {
            return -1;
        }
        return currency.getLastUD();
    }

    @Override
    public Map<Integer, Long> getAllUD(String id) {

        return currencyUDsByBlock.get(id);
    }

    @Override
    public void insertUDs(String id,  Map<Integer, Long> newUDs) {
        Map<Integer, Long> udsByBlock = currencyUDsByBlock.get(id);
        if (udsByBlock == null) {
            udsByBlock = new HashMap<>();
            currencyUDsByBlock.put(id, udsByBlock);
        }
        udsByBlock.putAll(newUDs);
    }

    @Override
    public boolean isExists(String currencyId) {
        return currencies.get(currencyId) != null;
    }
}

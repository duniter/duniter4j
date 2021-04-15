package org.duniter.core.client.repositories.mem;

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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.duniter.core.client.model.local.Currency;
import org.duniter.core.client.model.local.Dividend;
import org.duniter.core.client.repositories.CurrencyRepository;
import org.duniter.core.client.model.local.ICurrency;
import org.duniter.core.util.Preconditions;

import java.util.*;

/**
 * Created by blavenie on 29/12/15.
 */
public class MemoryCurrencyRepositoryImpl implements CurrencyRepository<Currency>, MemoryCrudRepository<String, Currency> {

    private Map<String, Currency> currencies = new HashMap<>();

    public MemoryCurrencyRepositoryImpl() {
        super();
    }

    @Override
    public <S extends Currency> S save(S entity) {
        Preconditions.checkNotNull(entity);
        Preconditions.checkNotNull(entity.getId());
        if (!existsById(entity.getId())) {
            currencies.put(entity.getId(), entity);
        }
        else {
            currencies.put(entity.getId(), entity);
        }

        return entity;
    }

    @Override
    public void deleteById(String id) {
        currencies.remove(id);
    }

    @Override
    public long count() {
        return currencies.size();
    }

    @Override
    public Iterable<String> findAllIds() {
        return ImmutableSet.copyOf(currencies.keySet());
    }

    @Override
    public Iterable<Currency> findAll() {
        return ImmutableList.copyOf(currencies.values());
    }

    @Override
    public Optional<Currency> findById(String id) {
        return Optional.ofNullable(currencies.get(id));
    }

    @Override
    public boolean existsById(String id) {
        return currencies.containsKey(id);
    }
}

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
import org.duniter.core.client.model.local.Dividend;
import org.duniter.core.client.repositories.DividendRepository;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by blavenie on 29/12/15.
 */
public class MemoryDividendRepositoryImpl implements DividendRepository, MemoryCrudRepository<String, Dividend> {

    private Map<String, Dividend> dividends = new HashMap<>();

    public MemoryDividendRepositoryImpl() {
        super();
    }

    @Override
    public <S extends Dividend> S save(S entity) {
        String id = entity.getId();
        if (id == null) {
            id = Dividend.computeId(entity);
            entity.setId(id);
        }
        dividends.put(id, entity);

        return entity;
    }

    @Override
    public void deleteById(String id) {
        dividends.remove(id);
    }

    @Override
    public long count() {
        return dividends.size();
    }

    @Override
    public Iterable<Dividend> findAll() {
        return ImmutableList.copyOf(dividends.values());
    }

    @Override
    public Optional<Dividend> findById(String id) {
        return Optional.ofNullable(dividends.get(id));
    }

    @Override
    public Iterable<Dividend> findAllByCurrency(String currency) {
        return dividends.values().stream()
                .filter(entity -> currency.equals(entity.getCurrency()))
                .sorted(Comparator.comparing(Dividend::getNumber))
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsById(String id) {
        return dividends.get(id) != null;
    }
}

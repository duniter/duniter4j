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
import org.duniter.core.client.model.local.Currency;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by eis on 07/02/15.
 */
public interface CurrencyService extends Service {

    Currency save(final Currency currency);

    List<Currency> getAll();

    List<Currency> getAllByAccount(long accountId);

    Currency getById(String currencyId);

    /**
     * Return a (cached) currency name, by id
     * @param currencyId
     * @return
     */
    String getNameById(String currencyId);

    /**
     * Return a currency id, by name
     * @param currencyName
     * @return
     */
    String getIdByName(String currencyName);

    /**
     * Return a (cached) list of currency ids
     * @return
     */
    Set<String> getAllIds();

    /**
     * Return a (cached) number of registered currencies
     * @return
     */
    int count();

    /**
     * Return the value of the last universal dividend
     * @param currencyId
     * @return
     */
    long getLastUD(String currencyId);

    /**
     * Return a map of UD (key=blockNumber, value=amount)
     * @return
     */
    Map<Integer, Long> refreshAndGetUD(String currencyId, long lastSyncBlockNumber);

    /**
     * Return a map of UD (key=blockNumber, value=amount)
     * @return
     */
     Map<Integer, Long> getAllUD(String currencyId);
}

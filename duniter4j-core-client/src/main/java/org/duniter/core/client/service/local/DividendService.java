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

import java.util.Map;
import java.util.Optional;

/**
 * Created by eis on 07/02/15.
 */
public interface DividendService extends Service {

    /**
     * Return the value of the last universal dividend
     * @param currencyId
     * @return
     */
    Optional<Long> findLastDividendByCurrency(String currency);

    /**
     * Return a map of UD (key=blockNumber, value=amount)
     * @return
     */
    Map<Integer, Long> refreshAndGetDividends(String currency, long lastSyncBlockNumber);

    /**
     * Return a map of UD (key=blockNumber, value=amount)
     * @return
     */
    Map<Integer, Long> findAllDividendsByCurrency(String currency);

    /**
     * Update the last currency dividend
     * @param currency
     * @param dividend
     */
    void updateLastDividendByCurrency(String currency, Long dividend);
}

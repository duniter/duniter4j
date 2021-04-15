package org.duniter.core.client.service.elasticsearch;

/*
 * #%L
 * Duniter4j :: ElasticSearch Indexer
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
import org.duniter.core.client.model.local.Peer;

import java.util.List;

/**
 * Created by Benoit on 06/05/2015.
 */
public interface CurrencyPodRemoteService extends Service {

    /**
     * Test if elasticsearch node defined in config is alive
     * @return
     */
    boolean isNodeAlive();

    /**
     * Test if elasticsearch node from the given endpoint is alive
     * @return
     */
    boolean isNodeAlive(Peer peer);

    List<String> getAllCurrencyNames();
}

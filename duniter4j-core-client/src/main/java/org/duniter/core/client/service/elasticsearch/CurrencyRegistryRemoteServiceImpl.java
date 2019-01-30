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

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.duniter.core.beans.InitializingBean;
import org.duniter.core.client.config.Configuration;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.client.service.bma.BaseRemoteServiceImpl;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.util.json.JsonAttributeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Created by Benoit on 06/05/2015.
 */
public class CurrencyRegistryRemoteServiceImpl extends BaseRemoteServiceImpl implements CurrencyRegistryRemoteService, InitializingBean, Closeable{
    private static final Logger log = LoggerFactory.getLogger(CurrencyRegistryRemoteServiceImpl.class);

    private final static String URL_STATUS = "/node/summary";
    private final static String URL_ALL_CURRENCY_NAMES = "/currency/record/_search?_source=currencyName";

    private Configuration config;
    private Peer peer;

    public CurrencyRegistryRemoteServiceImpl() {
        super();
    }

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        config = Configuration.instance();
        peer = Peer.newBuilder().setHost(config.getNodeElasticSearchHost()).setPort(config.getNodeElasticSearchPort()).build();
    }

    @Override
    public void close() throws IOException {
        super.close();
        config = null;
        peer = null;
    }

    @Override
    public boolean isNodeAlive() {
        return isNodeAlive(peer);
    }

    @Override
    public boolean isNodeAlive(Peer peer) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Checking if elasticsearch node [%s:%s] is alive...", peer.getHost(), peer.getPort()));
        }

        // get currency
        String jsonResponse;
        try {
            jsonResponse = executeRequest(peer, URL_STATUS, String.class);
            Integer statusCode = new JsonAttributeParser<>("status", Integer.class).getValue(jsonResponse);
            return statusCode != null && statusCode == HttpStatus.SC_OK;
        }
        catch(TechnicalException e) {
            if (log.isDebugEnabled()) {
                log.debug("Failed to get node status: " + e.getMessage(), e);
            }
            return false;
        }
    }

    @Override
    public List<String> getAllCurrencyNames() {
        if (log.isDebugEnabled()) {
            log.debug("Getting allOfToList currency names...");
        }

        // get currency
        String path = httpService.getPath(peer, URL_ALL_CURRENCY_NAMES);
        String jsonResponse = httpService.executeRequest(new HttpGet(path), String.class);

        List<String> currencyNames = new JsonAttributeParser<>("currencyName", String.class).getValues(jsonResponse);

        // Sort into alphabetical order
        Collections.sort(currencyNames);

        return currencyNames;
    }

    /* -- protected methods -- */


}

package org.duniter.core.client.service.elasticsearch;

/*
 * #%L
 * UCoin Java Client :: ElasticSearch Indexer
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
import org.apache.http.client.utils.URIBuilder;
import org.duniter.core.beans.InitializingBean;
import org.duniter.core.client.config.Configuration;
import org.duniter.core.client.model.bma.gson.GsonUtils;
import org.duniter.core.client.model.bma.jackson.JacksonUtils;
import org.duniter.core.client.model.elasticsearch.Currency;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.client.model.local.Wallet;
import org.duniter.core.client.service.ServiceLocator;
import org.duniter.core.client.service.bma.BaseRemoteServiceImpl;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.service.CryptoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

/**
 * Created by Benoit on 06/05/2015.
 */
public class CurrencyRegistryRemoteServiceImpl extends BaseRemoteServiceImpl implements CurrencyRegistryRemoteService, InitializingBean, Closeable{
    private static final Logger log = LoggerFactory.getLogger(CurrencyRegistryRemoteServiceImpl.class);

    private final static String URL_STATUS = "/";
    private final static String URL_ALL_CURRENCY_NAMES = "/currency/simple/_search?_source=currencyName";
    private final static String URL_ADD_CURRENCY = "/rest/currency/add";

    private Configuration config;
    private Peer peer;

    public CurrencyRegistryRemoteServiceImpl() {
        super();
    }

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        config = Configuration.instance();
        peer = new Peer(config.getNodeElasticSearchHost(), config.getNodeElasticSearchPort());
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
            int statusCode = JacksonUtils.getValueFromJSONAsInt(jsonResponse, "status");
            return statusCode == HttpStatus.SC_OK;
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
            log.debug("Getting all currency names...");
        }

        // get currency
        String path = getPath(peer, URL_ALL_CURRENCY_NAMES);
        String jsonResponse = executeRequest(new HttpGet(path), String.class);

        List<String> currencyNames = JacksonUtils.getValuesFromJSONAsString(jsonResponse, "currencyName");

        // Sort into alphabetical order
        Collections.sort(currencyNames);

        return currencyNames;
    }

    @Override
    public void registerNewCurrency(Wallet wallet, Currency currency) {
        if (log.isDebugEnabled()) {
            log.debug("Registering a new currency...");
        }

        String currencyJson = GsonUtils.newBuilder().create().toJson(currency);
        CryptoService cryptoService = ServiceLocator.instance().getCryptoService();
        String signature = cryptoService.sign(currencyJson, wallet.getSecKey());

        registerNewCurrency(
                wallet.getPubKeyHash(),
                currencyJson,
                signature);

        // get currency
        //HttpGet httpGet = new HttpGet(getAppendedPath("/currency/simple/_search?_source=currencyName"));
        //String jsonString = executeRequest(httpGet, String.class);

    }

    @Override
    public void registerNewCurrency(String pubkey, String jsonCurrency, String signature) {
        if (log.isDebugEnabled()) {
            log.debug("Registering a new currency...");
        }

        URIBuilder builder = getURIBuilder(config.getNodeElasticSearchUrl(), URL_ADD_CURRENCY);
        builder.addParameter("pubkey", pubkey);
        builder.addParameter("currency", jsonCurrency);
        builder.addParameter("sig", signature);

        HttpGet httpGet;
        try {
            httpGet = new HttpGet(builder.build());
        }
        catch(URISyntaxException e) {
            throw new TechnicalException(e);
        }

        String result = executeRequest(httpGet, String.class);

        if (log.isDebugEnabled()) {
            log.debug("Server response, after currency registration: " + result);
        }
    }

    /* -- protected methods -- */


}

package org.duniter.elasticsearch.service;

/*
 * #%L
 * UCoin Java Client :: Core API
 * %%
 * Copyright (C) 2014 - 2015 EIS
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


import com.fasterxml.jackson.core.JsonProcessingException;
import org.duniter.core.client.dao.CurrencyDao;
import org.duniter.core.client.dao.PeerDao;
import org.duniter.core.client.model.bma.BlockchainBlock;
import org.duniter.core.client.model.bma.BlockchainParameters;
import org.duniter.core.client.model.bma.jackson.JacksonUtils;
import org.duniter.core.client.model.elasticsearch.Currency;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.client.service.bma.BlockchainRemoteService;
import org.duniter.core.client.service.exception.HttpConnectException;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.service.CryptoService;
import org.duniter.core.util.Preconditions;
import org.duniter.elasticsearch.PluginSettings;
import org.duniter.elasticsearch.client.Duniter4jClient;
import org.duniter.elasticsearch.dao.*;
import org.duniter.elasticsearch.exception.AccessDeniedException;
import org.duniter.elasticsearch.exception.DuplicateIndexIdException;
import org.duniter.elasticsearch.exception.InvalidSignatureException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Created by Benoit on 30/03/2015.
 */
public class CurrencyService extends AbstractService {

    public static final String INDEX = CurrencyExtendDao.INDEX;
    public static final String RECORD_TYPE = CurrencyExtendDao.RECORD_TYPE;

    private BlockchainRemoteService blockchainRemoteService;
    private CurrencyExtendDao currencyDao;
    private Map<String, IndexDao<?>> currencyDataDaos = new HashMap<>();
    private Injector injector;

    @Inject
    public CurrencyService(Duniter4jClient client,
                           PluginSettings settings,
                           CryptoService cryptoService,
                           CurrencyDao currencyDao,
                           BlockchainRemoteService blockchainRemoteService,
                           Injector injector) {
        super("duniter." + INDEX, client, settings, cryptoService);
        this.blockchainRemoteService = blockchainRemoteService;
        this.currencyDao = (CurrencyExtendDao)currencyDao;
        this.injector = injector;
    }

    public CurrencyService createIndexIfNotExists() {
        currencyDao.createIndexIfNotExists();
        return this;
    }

    public CurrencyService deleteIndex() {
        currencyDao.deleteIndex();
        return this;
    }

    public boolean isCurrencyExists(String currencyName) {
        return currencyDao.isExists(currencyName);
    }

    /**
     * Retrieve the blockchain data, from peer
     *
     * @param peer
     * @param autoReconnect
     * @return the created blockchain
     */
    public Currency indexCurrencyFromPeer(Peer peer, boolean autoReconnect) {
        if (!autoReconnect) return indexCurrencyFromPeer(peer);

        while(true) {
            try {
                return indexCurrencyFromPeer(peer);
            } catch (HttpConnectException e) {
                // log then retry
                logger.warn(String.format("[%s] Unable to connect. Retrying in 10s...", peer.toString()));
            }

            try {
                Thread.sleep(10 * 1000); // wait 20s
            } catch(Exception e) {
                throw new TechnicalException(e);
            }
        }
    }

    /**
     * Retrieve the blockchain data, from peer
     *
     * @param peer
     * @return the created blockchain
     */
    public Currency indexCurrencyFromPeer(Peer peer) {
        BlockchainParameters parameters = blockchainRemoteService.getParameters(peer);
        BlockchainBlock firstBlock = blockchainRemoteService.getBlock(peer, 0l);
        BlockchainBlock currentBlock = blockchainRemoteService.getCurrentBlock(peer);
        Long lastUD = blockchainRemoteService.getLastUD(peer);


        Currency result = new Currency();
        result.setCurrencyName(parameters.getCurrency());
        result.setFirstBlockSignature(firstBlock.getSignature());
        result.setMembersCount(currentBlock.getMembersCount());
        result.setLastUD(lastUD);
        result.setParameters(parameters);

        // Save it
        saveCurrency(result, pluginSettings.getKeyringPublicKey());

        return result;
    }

    /**
     * Save a blockchain (update or create) into the blockchain index.
     * @param currency
     * @param issuer
     * @throws DuplicateIndexIdException
     * @throws AccessDeniedException if exists and user if not the original blockchain sender
     */
    public void saveCurrency(Currency currency, String issuer) throws DuplicateIndexIdException {
        Preconditions.checkNotNull(currency, "currency could not be null") ;
        Preconditions.checkNotNull(currency.getId(), "currency attribute 'currency' could not be null");

        String previousIssuer = getSenderPubkeyByCurrencyId(currency.getId());

        // Currency not exists, so create it
        if (previousIssuer == null) {
            // make sure to fill the sender
            currency.setIssuer(issuer);

            // Save it
            currencyDao.create(currency);

            // Create data index (delete first if exists)
            getCurrencyDataDao(currency.getId())
                .deleteIndex()
                .createIndexIfNotExists();

        }

        // Exists, so check the owner signature
        else {
            if (issuer != null && !Objects.equals(issuer, previousIssuer)) {
                throw new AccessDeniedException("Could not change the currency, because it has been registered by another public key.");
            }

            // Make sure the sender is not changed
            if (issuer != null) {
                currency.setIssuer(previousIssuer);
            }

            // Save changes
            currencyDao.update(currency);

            // Create data index (if need)
            getCurrencyDataDao(currency.getId())
                    .createIndexIfNotExists();
        }
    }

    /**
     * Registrer a new blockchain.
     * @param pubkey the sender pubkey
     * @param jsonCurrency the blockchain, as JSON
     * @param signature the signature of sender.
     * @throws InvalidSignatureException if signature not correspond to sender pubkey
     */
    public void insertCurrency(String pubkey, String jsonCurrency, String signature) {
        Preconditions.checkNotNull(pubkey);
        Preconditions.checkNotNull(jsonCurrency);
        Preconditions.checkNotNull(signature);

        if (!cryptoService.verify(jsonCurrency, signature, pubkey)) {
            String currencyName = JacksonUtils.getValueFromJSONAsString(jsonCurrency, "currencyName");
            logger.warn(String.format("Currency not added, because bad signature. blockchain [%s]", currencyName));
            throw new InvalidSignatureException("Bad signature");
        }

        Currency currency;
        try {
            currency = objectMapper.readValue(jsonCurrency, Currency.class);
            Preconditions.checkNotNull(currency);
            Preconditions.checkNotNull(currency.getCurrencyName());
        } catch(Throwable t) {
            logger.error("Error while reading blockchain JSON: " + jsonCurrency);
            throw new TechnicalException("Error while reading blockchain JSON: " + jsonCurrency, t);
        }

        saveCurrency(currency, pubkey);
    }



    /* -- Internal methods -- */



    /**
     * Retrieve a blockchain from its name
     * @param currencyId
     * @return
     */
    protected String getSenderPubkeyByCurrencyId(String currencyId) {

        if (!isCurrencyExists(currencyId)) {
            return null;
        }

        // Prepare request

        SearchRequestBuilder searchRequest = client
                .prepareSearch(INDEX)
                .setTypes(RECORD_TYPE)
                .setSearchType(SearchType.QUERY_AND_FETCH);

        // If more than a word, search on terms match
        searchRequest.setQuery(new IdsQueryBuilder().addIds(currencyId));

        // Execute query
        try {
            SearchResponse response = searchRequest.execute().actionGet();

            // Read query result
            SearchHit[] searchHits = response.getHits().getHits();
            for (SearchHit searchHit : searchHits) {
                if (searchHit.source() != null) {
                    Currency currency = objectMapper.readValue(new String(searchHit.source(), "UTF-8"), Currency.class);
                    return currency.getIssuer();
                }
                else {
                    SearchHitField field = searchHit.getFields().get("issuer");
                    return field.getValue().toString();
                }
            }
        }
        catch(SearchPhaseExecutionException | IOException e) {
            // Failed or no item on index
        }

        return null;
    }

    protected IndexDao<?> getCurrencyDataDao(final String currencyId) {
        // Create data
        IndexDao<?> dataDao = currencyDataDaos.get(currencyId);
        if (dataDao == null) {
            dataDao = new AbstractIndexDao(currencyId) {
                @Override
                protected void createIndex() throws JsonProcessingException {
                    logger.info(String.format("Creating index [%s]", currencyId));

                    CreateIndexRequestBuilder createIndexRequestBuilder = client.admin().indices().prepareCreate(currencyId);
                    org.elasticsearch.common.settings.Settings indexSettings = org.elasticsearch.common.settings.Settings.settingsBuilder()
                            .put("number_of_shards", 3)
                            .put("number_of_replicas", 1)
                            //.put("analyzer", createDefaultAnalyzer())
                            .build();
                    createIndexRequestBuilder.setSettings(indexSettings);

                    // Add peer type
                    TypeDao<?> peerDao = (TypeDao<?>)ServiceLocator.instance().getBean(PeerDao.class);
                    createIndexRequestBuilder.addMapping(peerDao.getType(), peerDao.createTypeMapping());

                    // Add block type
                    BlockDao blockDao = ServiceLocator.instance().getBean(BlockDao.class);
                    createIndexRequestBuilder.addMapping(blockDao.getType(), blockDao.createTypeMapping());

                    // Add blockStat type
                    BlockStatDao blockStatDao = injector.getInstance(BlockStatDao.class);
                    createIndexRequestBuilder.addMapping(blockStatDao.getType(), blockStatDao.createTypeMapping());

                    createIndexRequestBuilder.execute().actionGet();
                }
            };
            injector.injectMembers(dataDao);
            currencyDataDaos.put(currencyId, dataDao);
        }

        return dataDao;


    }
}

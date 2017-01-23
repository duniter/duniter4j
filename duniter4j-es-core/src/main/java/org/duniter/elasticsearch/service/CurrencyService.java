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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.ArrayUtils;
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
import org.duniter.core.util.StringUtils;
import org.duniter.elasticsearch.PluginSettings;
import org.duniter.elasticsearch.exception.AccessDeniedException;
import org.duniter.elasticsearch.exception.DuplicateIndexIdException;
import org.duniter.elasticsearch.exception.InvalidSignatureException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Created by Benoit on 30/03/2015.
 */
public class CurrencyService extends AbstractService {

    protected static final String REGEX_WORD_SEPARATOR = "[-\\t@# _]+";

    public static final String INDEX = "currency";
    public static final String RECORD_TYPE = "record";
    public static final String PEER_TYPE = "peer";

    private final ObjectMapper objectMapper;
    private BlockchainRemoteService blockchainRemoteService;

    @Inject
    public CurrencyService(Client client,
                           PluginSettings settings,
                           CryptoService cryptoService,
                           BlockchainRemoteService blockchainRemoteService) {
        super("duniter." + INDEX, client, settings, cryptoService);
        this.objectMapper = JacksonUtils.newObjectMapper();
        this.blockchainRemoteService = blockchainRemoteService;
    }

    /**
     * Create index need for blockchain registry, if need
     */
    public CurrencyService createIndexIfNotExists() {
        try {
            if (!existsIndex(INDEX)) {
                createIndex();
            }
        }
        catch(JsonProcessingException e) {
            throw new TechnicalException(String.format("Error while creating index [%s]", INDEX));
        }
        return this;
    }

    /**
     * Create index for registry
     * @throws JsonProcessingException
     */
    public CurrencyService createIndex() throws JsonProcessingException {
        logger.info(String.format("Creating index [%s]", INDEX));

        CreateIndexRequestBuilder createIndexRequestBuilder = client.admin().indices().prepareCreate(INDEX);
        org.elasticsearch.common.settings.Settings indexSettings = org.elasticsearch.common.settings.Settings.settingsBuilder()
                .put("number_of_shards", 3)
                .put("number_of_replicas", 1)
                //.put("analyzer", createDefaultAnalyzer())
                .build();
        createIndexRequestBuilder.setSettings(indexSettings);
        createIndexRequestBuilder.addMapping(RECORD_TYPE, createRecordTypeMapping());
        createIndexRequestBuilder.execute().actionGet();

        return this;
    }

    public CurrencyService deleteIndex() {
        deleteIndexIfExists(INDEX);
        return this;
    }

    public boolean existsIndex() {
        return super.existsIndex(INDEX);
    }

    public boolean isCurrencyExists(String currencyName) {
        String pubkey = getSenderPubkeyByCurrencyId(currencyName);
        return !StringUtils.isEmpty(pubkey);
    }


    /**
     * Add a new currency
     * TODO :
     *  - add security, to allow only request from admin (check signature against settings keyring)
     *
     * @param json
     * @return
     */
    public String indexCurrencyFromJson(String json) {
        throw new TechnicalException("Not implemented yet. Received JSON: " + json);
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
        result.setCurrency(parameters.getCurrency());
        result.setFirstBlockSignature(firstBlock.getSignature());
        result.setMembersCount(currentBlock.getMembersCount());
        result.setLastUD(lastUD);
        result.setParameters(parameters);

        indexCurrency(result);

        indexPeer(parameters.getCurrency(), peer);

        return result;
    }

    /**
     * Index a blockchain
     * @param currency
     */
    public void indexCurrency(Currency currency) {
        try {
            Preconditions.checkNotNull(currency.getCurrency());

            // Fill tags
            if (ArrayUtils.isEmpty(currency.getTags())) {
                String currencyName = currency.getCurrency();
                String[] tags = currencyName.split(REGEX_WORD_SEPARATOR);
                List<String> tagsList = Lists.newArrayList(tags);

                // Convert as a sentence (replace seprator with a space)
                String sentence = currencyName.replaceAll(REGEX_WORD_SEPARATOR, " ");
                if (!tagsList.contains(sentence)) {
                    tagsList.add(sentence);
                }

                currency.setTags(tagsList.toArray(new String[tagsList.size()]));
            }

            // Serialize into JSON
            byte[] json = objectMapper.writeValueAsBytes(currency);

            // Preparing indexBlocksFromNode
            IndexRequestBuilder indexRequest = client.prepareIndex(INDEX, RECORD_TYPE)
                    .setId(currency.getCurrency())
                    .setSource(json);

            // Execute indexBlocksFromNode
            indexRequest
                    .setRefresh(true)
                    .execute().actionGet();

        } catch(JsonProcessingException e) {
            throw new TechnicalException(e);
        }
    }

    public String indexPeer(String currency, Peer peer) {
        Preconditions.checkNotNull(currency);
        Preconditions.checkNotNull(peer);
        try {
            // Serialize into JSON
            byte[] json = objectMapper.writeValueAsBytes(peer);

            // Preparing index
            IndexRequestBuilder indexRequest = client.prepareIndex(currency, PEER_TYPE)
                    .setSource(json);

            // Execute index
            return indexRequest
                    .setRefresh(true)
                    .execute().actionGet().getId();

        } catch(JsonProcessingException e) {
            throw new TechnicalException(e);
        }
    }

    /**
     * Get suggestions from a string query. Useful for web autocomplete field (e.g. text full search)
     * @param query
     * @return
     */
   /* public List<String> getSuggestions(String query) {
        CompletionSuggestionBuilder suggestionBuilder = new CompletionSuggestionBuilder(INDEX_TYPE)
                .text(query)
                .size(10) // limit to 10 results
                .field("tags");

        // Prepare request
        SuggestRequestBuilder suggestRequest = client
                .prepareSuggest(INDEX_NAME)
                .addSuggestion(suggestionBuilder);

        // Execute query
        SuggestResponse response = suggestRequest.execute().actionGet();

        // Read query result
        return toSuggestions(response, RECORD_CATEGORY_TYPE, query);
    }*/

    /**
     * Save a blockchain (update or create) into the blockchain index.
     * @param currency
     * @param issuer
     * @throws DuplicateIndexIdException
     * @throws AccessDeniedException if exists and user if not the original blockchain sender
     */
    public void saveCurrency(Currency currency, String issuer) throws DuplicateIndexIdException {
        Preconditions.checkNotNull(currency, "currency could not be null") ;
        Preconditions.checkNotNull(currency.getCurrency(), "currency attribute 'currency' could not be null");

        String previousIssuer = getSenderPubkeyByCurrencyId(currency.getCurrency());

        // Currency not exists, so create it
        if (previousIssuer == null) {
            // make sure to fill the sender
            currency.setIssuer(issuer);

            // Save it
            indexCurrency(currency);
        }

        // Exists, so check the owner signature
        else {
            if (!Objects.equals(issuer, previousIssuer)) {
                throw new AccessDeniedException("Could not change the currency, because it has been registered by another public key.");
            }

            // Make sure the sender is not changed
            currency.setIssuer(previousIssuer);

            // Save changes
            indexCurrency(currency);
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
            Preconditions.checkNotNull(currency.getCurrency());
        } catch(Throwable t) {
            logger.error("Error while reading blockchain JSON: " + jsonCurrency);
            throw new TechnicalException("Error while reading blockchain JSON: " + jsonCurrency, t);
        }

        saveCurrency(currency, pubkey);
    }

    /* -- Internal methods -- */

    public XContentBuilder createRecordTypeMapping() {
        try {
            XContentBuilder mapping = XContentFactory.jsonBuilder().startObject().startObject(RECORD_TYPE)
                    .startObject("properties")

                    // currency
                    .startObject("currency")
                    .field("type", "string")
                    .endObject()

                    // firstBlockSignature
                    .startObject("firstBlockSignature")
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()

                    // member count
                    .startObject("membersCount")
                    .field("type", "long")
                    .endObject()

                    // lastUD
                    .startObject("lastUD")
                    .field("type", "long")
                    .endObject()

                    // unitbase
                    .startObject("unitbase")
                    .field("type", "integer")
                    .endObject()

                    // issuer
                    .startObject("issuer")
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()

                    // tags
                    .startObject("tags")
                    .field("type", "completion")
                    .field("search_analyzer", "simple")
                    .field("analyzer", "simple")
                    .field("preserve_separators", "false")

                    .endObject()
                    .endObject()
                    .endObject().endObject();

            return mapping;
        }
        catch(IOException ioe) {
            throw new TechnicalException(String.format("Error while getting mapping for index [%s/%s]: %s", INDEX, RECORD_TYPE, ioe.getMessage()), ioe);
        }
    }

    /**
     * Retrieve a blockchain from its name
     * @param currencyId
     * @return
     */
    protected String getSenderPubkeyByCurrencyId(String currencyId) {

        if (!existsIndex(currencyId)) {
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
}

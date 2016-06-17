package org.duniter.elasticsearch.service.registry;

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
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.duniter.core.client.model.bma.BlockchainBlock;
import org.duniter.core.client.model.bma.BlockchainParameters;
import org.duniter.core.client.model.bma.gson.GsonUtils;
import org.duniter.core.client.model.elasticsearch.Currency;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.client.service.ServiceLocator;
import org.duniter.core.client.service.bma.BlockchainRemoteService;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.service.CryptoService;
import org.duniter.core.util.ObjectUtils;
import org.duniter.elasticsearch.PluginSettings;
import org.duniter.elasticsearch.model.SearchResult;
import org.duniter.elasticsearch.service.AbstractService;
import org.duniter.elasticsearch.service.exception.AccessDeniedException;
import org.duniter.elasticsearch.service.exception.DuplicateIndexIdException;
import org.duniter.elasticsearch.service.exception.InvalidSignatureException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.suggest.SuggestRequestBuilder;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by Benoit on 30/03/2015.
 */
public class CurrencyRegistryService extends AbstractService<CurrencyRegistryService> {

    private static final ESLogger log = ESLoggerFactory.getLogger(CurrencyRegistryService.class.getName());

    public static final String INDEX_NAME = "registry";

    public static final String INDEX_TYPE = "blockchain";

    public static final String REGEX_WORD_SEPARATOR = "[-\\t@# ]+";
    public static final String REGEX_SPACE = "[\\t\\n\\r ]+";

    private CryptoService cryptoService;
    private Gson gson;

    @Inject
    public CurrencyRegistryService(Client client, PluginSettings settings) {
        super(client, settings);
        this.gson = GsonUtils.newBuilder().create();
    }

    @Override
    public CurrencyRegistryService start() {
        this.cryptoService = ServiceLocator.instance().getCryptoService();
        return super.start();
    }

    @Override
    public void close() {
        this.cryptoService = null;
        this.gson = null;
        super.close();
    }

    /**
     * Delete blockchain index, and all data
     * @throws JsonProcessingException
     */
    public void deleteIndex() throws JsonProcessingException {
        deleteIndexIfExists(INDEX_NAME);
    }

    /**
     * Create index need for blockchain registry, if need
     */
    public void createIndexIfNotExists() {
        try {
            if (!existsIndex(INDEX_NAME)) {
                    createIndex();
            }
        }
        catch(JsonProcessingException e) {
            throw new TechnicalException(String.format("Error while creating index [%s]", INDEX_NAME));
        }
    }

    /**
     * Create index need for blockchain registry
     * @throws JsonProcessingException
     */
    public void createIndex() throws JsonProcessingException {
        log.info(String.format("Creating index [%s/]", INDEX_NAME, INDEX_TYPE));

        CreateIndexRequestBuilder createIndexRequestBuilder = getClient().admin().indices().prepareCreate(INDEX_NAME);
        Settings indexSettings = Settings.settingsBuilder()
                .put("number_of_shards", 1)
                .put("number_of_replicas", 1)
                //.put("analyzer", createDefaultAnalyzer())
                .build();
        createIndexRequestBuilder.setSettings(indexSettings);
        createIndexRequestBuilder.addMapping(INDEX_TYPE, createIndexMapping());
        createIndexRequestBuilder.execute().actionGet();
    }

    /**
     * Retrieve the blockchain data, from peer
     *
     * @param peer
     * @return the created blockchain
     */
    public Currency indexCurrencyFromPeer(Peer peer) {
        BlockchainRemoteService blockchainRemoteService = ServiceLocator.instance().getBlockchainRemoteService();
        BlockchainParameters parameters = blockchainRemoteService.getParameters(peer);
        BlockchainBlock firstBlock = blockchainRemoteService.getBlock(peer, 0);
        BlockchainBlock currentBlock = blockchainRemoteService.getCurrentBlock(peer);
        long lastUD = blockchainRemoteService.getLastUD(peer);

        Currency result = new Currency();
        result.setCurrencyName(parameters.getCurrency());
        result.setFirstBlockSignature(firstBlock.getSignature());
        result.setMembersCount(currentBlock.getMembersCount());
        result.setLastUD(lastUD);
        result.setParameters(parameters);
        result.setPeers(new Peer[]{peer});

        indexCurrency(result);

        // Index the first block
        // FIXME : attention au dependence circulaire : cela devrait plutot etre fait à l'exetrieure e registry
        //         par exemple dans l'action REST
        //blockBlockchainService.createIndexIfNotExists(parameters.getCurrency());
        //blockBlockchainService.indexBlock(firstBlock, false);
        //blockBlockchainService.indexCurrentBlock(firstBlock, true);

        return result;
    }

    /**
     * Index a blockchain
     * @param currency
     */
    public void indexCurrency(Currency currency) {
        try {
            ObjectUtils.checkNotNull(currency.getCurrencyName());

            // Fill tags
            if (ArrayUtils.isEmpty(currency.getTags())) {
                String currencyName = currency.getCurrencyName();
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
            byte[] json = getObjectMapper().writeValueAsBytes(currency);

            // Preparing indexBlocksFromNode
            IndexRequestBuilder indexRequest = getClient().prepareIndex(INDEX_NAME, INDEX_TYPE)
                    .setId(currency.getCurrencyName())
                    .setSource(json);

            // Execute indexBlocksFromNode
            indexRequest
                    .setRefresh(true)
                    .execute().actionGet();

        } catch(JsonProcessingException e) {
            throw new TechnicalException(e);
        }
    }

    /**
     * Get suggestions from a string query. Useful for web autocomplete field (e.g. text full search)
     * @param query
     * @return
     */
    public List<String> getSuggestions(String query) {
        CompletionSuggestionBuilder suggestionBuilder = new CompletionSuggestionBuilder(INDEX_TYPE)
            .text(query)
            .size(10) // limit to 10 results
            .field("tags");

        // Prepare request
        SuggestRequestBuilder suggestRequest = getClient()
                .prepareSuggest(INDEX_NAME)
                .addSuggestion(suggestionBuilder);

        // Execute query
        SuggestResponse response = suggestRequest.execute().actionGet();

        // Read query result
        return toSuggestions(response, INDEX_TYPE, query);
    }

    /**
     * Find blockchain that match the givent string query (Full text search)
     * @param query
     * @return
     */
    public List<Currency> searchCurrencies(String query) {
        String[] queryParts = query.split(REGEX_SPACE);

        // Prepare request
        SearchRequestBuilder searchRequest = getClient()
                .prepareSearch(INDEX_NAME)
                .setTypes(INDEX_TYPE)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH);

        // If only one term, search as prefix
        if (queryParts.length == 1) {
            searchRequest.setQuery(QueryBuilders.prefixQuery("currencyName", query));
        }

        // If more than a word, search on terms match
        else {
            searchRequest.setQuery(QueryBuilders.matchQuery("currencyName", query));
        }

        // Sort as score/membersCount
        searchRequest.addSort("_score", SortOrder.DESC)
                .addSort("membersCount", SortOrder.DESC);

        // Highlight matched words
        searchRequest.setHighlighterTagsSchema("styled")
            .addHighlightedField("currencyName")
            .addFields("currencyName")
            .addFields("currencyName", "_source");

        // Execute query
        SearchResponse searchResponse = searchRequest.execute().actionGet();

        // Read query result
        return toCurrencies(searchResponse, true);
    }

    public void deleteAllCurrencies() {
        if (!existsIndex(INDEX_NAME)) {
            return;
        }

        log.info(String.format("Deleting all blockchain indexes"));

        // Prepare request
        SearchRequestBuilder request = getClient()
                .prepareSearch(INDEX_NAME)
                .setTypes(INDEX_TYPE);

        // Execute query
        SearchResponse response = request.execute().actionGet();

        // Delete every currencies
        List<Currency> currencies = toCurrencies(response);
        for (Currency currency: currencies){
            log.info(String.format("Deleting blockchain [%s]...", currency.getCurrencyName()));
            deleteIndexIfExists(currency.getCurrencyName());
        }

        deleteIndexIfExists(INDEX_NAME);

        log.info("All blockchain successfully deleted");
    }

    /**
     * find blockchain that match string query (full text search), and return a generic VO for search result.
     * @param query
     * @return a list of generic search result
     */
    public List<SearchResult> searchCurrenciesAsVO(String query) {
        String[] queryParts = query.split(REGEX_SPACE);

        // Prepare request
        SearchRequestBuilder searchRequest = getClient()
                .prepareSearch(INDEX_NAME)
                .setTypes(INDEX_TYPE)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH);

        // If only one term, search as prefix
        if (queryParts.length == 1) {
            searchRequest.setQuery(QueryBuilders.prefixQuery("currencyName", query));
        }

        // If more than a word, search on terms match
        else {
            searchRequest.setQuery(QueryBuilders.matchQuery("currencyName", query));
        }

        // Sort as score/membersCount
        searchRequest.addSort("_score", SortOrder.DESC)
                .addSort("membersCount", SortOrder.DESC);

        // Highlight matched words
        searchRequest.setHighlighterTagsSchema("styled")
                .addHighlightedField("currencyName")
                .addFields("currencyName")
                .addFields("membersCount");

        // Execute query
        SearchResponse searchResponse = searchRequest.execute().actionGet();

        // Read query result
        return toSearchResults(searchResponse, true);
    }

    /**
     * Retrieve a blockchain from its name
     * @param currencyId
     * @return
     */
    public Currency getCurrencyById(String currencyId) {

        if (!existsIndex(currencyId)) {
            return null;
        }

        // Prepare request
        SearchRequestBuilder searchRequest = getClient()
                .prepareSearch(INDEX_NAME)
                .setTypes(INDEX_TYPE)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH);

        // If more than a word, search on terms match
        searchRequest.setQuery(QueryBuilders.matchQuery("_id", currencyId));

        // Execute query
        List<Currency> currencies = null;
        try {
            SearchResponse searchResponse = searchRequest.execute().actionGet();
            currencies = toCurrencies(searchResponse);
        }
        catch(SearchPhaseExecutionException e) {
            // Failed or no item on index
        }

        // No result : return null
        if (CollectionUtils.isEmpty(currencies)) {
            return null;
        }

        // Return the unique result
        return CollectionUtils.extractSingleton(currencies);
    }

    /**
     * Save a blockchain (update or create) into the blockchain index.
     * @param currency
     * @param senderPubkey
     * @throws DuplicateIndexIdException
     * @throws AccessDeniedException if exists and user if not the original blockchain sender
     */
    public void saveCurrency(Currency currency, String senderPubkey) throws DuplicateIndexIdException {
        ObjectUtils.checkNotNull(currency, "blockchain could not be null") ;
        ObjectUtils.checkNotNull(currency.getCurrencyName(), "blockchain attribute 'currencyName' could not be null");

        Currency existingCurrency = getCurrencyById(currency.getCurrencyName());

        // Currency not exists, so create it
        if (existingCurrency == null || currency.getSenderPubkey() == null) {
            // make sure to fill the sender
            currency.setSenderPubkey(senderPubkey);

            // Save it
            indexCurrency(currency);
        }

        // Exists, so check the owner signature
        else {
            if (!Objects.equals(currency.getSenderPubkey(), senderPubkey)) {
                throw new AccessDeniedException("Could not change blockchain, because it has been registered by another public key.");
            }

            // Make sure the sender is not changed
            currency.setSenderPubkey(senderPubkey);

            // Save changes
            indexCurrency(currency);
        }
    }

    /**
     * Get the full list of currencies names, from blockchain index
     * @return
     */
    public List<String> getAllCurrencyNames() {
        // Prepare request
        SearchRequestBuilder searchRequest = getClient()
                .prepareSearch(INDEX_NAME)
                .setTypes(INDEX_TYPE);

        // Sort as score/membersCount
        searchRequest.addSort("currencyName", SortOrder.ASC)
            .addField("_id");

        // Execute query
        SearchResponse searchResponse = searchRequest.execute().actionGet();

        // Read query result
        return toCurrencyNames(searchResponse, true);
    }

    /**
     * Registrer a new blockchain.
     * @param pubkey the sender pubkey
     * @param jsonCurrency the blockchain, as JSON
     * @param signature the signature of sender.
     * @throws InvalidSignatureException if signature not correspond to sender pubkey
     */
    public void registerCurrency(String pubkey, String jsonCurrency, String signature) {
        Preconditions.checkNotNull(pubkey);
        Preconditions.checkNotNull(jsonCurrency);
        Preconditions.checkNotNull(signature);

        if (!cryptoService.verify(jsonCurrency, signature, pubkey)) {
            String currencyName = GsonUtils.getValueFromJSONAsString(jsonCurrency, "currencyName");
            log.warn(String.format("Currency not added, because bad signature. blockchain [%s]", currencyName));
            throw new InvalidSignatureException("Bad signature");
        }

        Currency currency = null;
        try {
            currency = gson.fromJson(jsonCurrency, Currency.class);
            Preconditions.checkNotNull(currency);
            Preconditions.checkNotNull(currency.getCurrencyName());
        } catch(Throwable t) {
            log.error("Error while reading blockchain JSON: " + jsonCurrency);
            throw new TechnicalException("Error while reading blockchain JSON: " + jsonCurrency, t);
        }

        saveCurrency(currency, pubkey);
    }

    /* -- Internal methods -- */

    public XContentBuilder createIndexMapping() {
        try {
            XContentBuilder mapping = XContentFactory.jsonBuilder().startObject().startObject(INDEX_TYPE)
                .startObject("properties")

                // blockchain name
                .startObject("currencyName")
                .field("type", "string")
                .endObject()

                // member count
                .startObject("membersCount")
                .field("type", "long")
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
            throw new TechnicalException(String.format("Error while getting mapping for index [%s/%s]: %s", INDEX_NAME, INDEX_TYPE, ioe.getMessage()), ioe);
        }
    }

    protected void createCurrency(Currency currency) throws DuplicateIndexIdException, JsonProcessingException {
        ObjectUtils.checkNotNull(currency, "blockchain could not be null") ;
        ObjectUtils.checkNotNull(currency.getCurrencyName(), "blockchain attribute 'currencyName' could not be null");

        Currency existingCurrency = getCurrencyById(currency.getCurrencyName());
        if (existingCurrency != null) {
            throw new DuplicateIndexIdException(String.format("Currency with name [%s] already exists.", currency.getCurrencyName()));
        }

        // register to blockchain
        indexCurrency(currency);

        // Create sub indexes
        // FIXME : circular reference
        // may be use an EVentBus (guava)
        //blockBlockchainService.createIndex(currency.getCurrencyName());
    }

    protected List<Currency> toCurrencies(SearchResponse response) {
        return toCurrencies(response, false);
    }

    protected List<Currency> toCurrencies(SearchResponse response, boolean withHighlight) {
        try {
            // Read query result
            SearchHit[] searchHits = response.getHits().getHits();
            List<Currency> result = Lists.newArrayListWithCapacity(searchHits.length);
            for (SearchHit searchHit : searchHits) {
                Currency currency = null;
                if (searchHit.source() != null) {
                    currency = gson.fromJson(new String(searchHit.source(), "UTF-8"), Currency.class);
                }
                else {
                    currency = new Currency();
                    SearchHitField field = searchHit.getFields().get("currencyName");
                    currency.setCurrencyName((String)field.getValue());
                }
                result.add(currency);

                // If possible, use highlights
                if (withHighlight) {
                    Map<String, HighlightField> fields = searchHit.getHighlightFields();
                    for (HighlightField field : fields.values()) {
                        String currencyNameHighLight = field.getFragments()[0].string();
                        currency.setCurrencyName(currencyNameHighLight);
                    }
                }
            }

            return result;
        } catch(IOException e) {
            throw new TechnicalException("Error while reading blockchain search result: " + e.getMessage(), e);
        }
    }

    protected List<SearchResult> toSearchResults(SearchResponse response, boolean withHighlight) {
        // Read query result
        SearchHit[] searchHits = response.getHits().getHits();
        List<SearchResult> result = Lists.newArrayListWithCapacity(searchHits.length);
        for (SearchHit searchHit : searchHits) {
            SearchResult value = new SearchResult();
            value.setId(searchHit.getId());
            value.setType(searchHit.getType());
            value.setValue(searchHit.getId());

            result.add(value);

            // If possible, use highlights
            if (withHighlight) {
                Map<String, HighlightField> fields = searchHit.getHighlightFields();
                for (HighlightField field : fields.values()) {
                    String currencyNameHighLight = field.getFragments()[0].string();
                    value.setValue(currencyNameHighLight);
                }
            }
        }

        return result;
    }

    protected List<String> toSuggestions(SuggestResponse response, String suggestionName, String query) {
        if (response.getSuggest() == null
                || response.getSuggest().getSuggestion(suggestionName) == null) {
            return null;
        }

        // Read query result
        Iterator<? extends Suggest.Suggestion.Entry.Option> iterator =
                response.getSuggest().getSuggestion(suggestionName).iterator().next().getOptions().iterator();

        List<String> result = Lists.newArrayList();
        while (iterator.hasNext()) {
            Suggest.Suggestion.Entry.Option next = iterator.next();
            String suggestion = next.getText().string();
            result.add(suggestion);
        }

        return result;
    }

    protected List<String> toCurrencyNames(SearchResponse response, boolean withHighlight) {
        // Read query result
        SearchHit[] searchHits = response.getHits().getHits();
        List<String> result = Lists.newArrayListWithCapacity(searchHits.length);
        for (SearchHit searchHit : searchHits) {
            result.add(searchHit.getId());
        }

        return result;
    }
}

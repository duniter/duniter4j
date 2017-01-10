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


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.collections4.MapUtils;
import org.duniter.core.beans.Bean;
import org.duniter.core.client.model.bma.jackson.JacksonUtils;
import org.duniter.core.client.model.elasticsearch.Record;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.service.CryptoService;
import org.duniter.core.util.CollectionUtils;
import org.duniter.core.util.Preconditions;
import org.duniter.core.util.StringUtils;
import org.duniter.elasticsearch.PluginSettings;
import org.duniter.elasticsearch.exception.AccessDeniedException;
import org.duniter.elasticsearch.exception.InvalidFormatException;
import org.duniter.elasticsearch.exception.InvalidSignatureException;
import org.duniter.elasticsearch.exception.NotFoundException;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequestBuilder;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.nuiton.i18n.I18n;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by Benoit on 08/04/2015.
 */
public abstract class AbstractService implements Bean {


    protected final ESLogger logger;
    protected final Client client;
    protected final PluginSettings pluginSettings;
    protected final ObjectMapper objectMapper;
    protected final CryptoService cryptoService;
    protected final int retryCount;
    protected final int retryWaitDuration;

    public AbstractService(String loggerName, Client client, PluginSettings pluginSettings) {
        this(loggerName, client, pluginSettings, null);
    }

    public AbstractService(Client client, PluginSettings pluginSettings) {
        this(client, pluginSettings, null);
    }

    public AbstractService(Client client, PluginSettings pluginSettings, CryptoService cryptoService) {
        this("duniter", client, pluginSettings, cryptoService);
    }

    public AbstractService(String loggerName, Client client, PluginSettings pluginSettings, CryptoService cryptoService) {
        this.logger = Loggers.getLogger(loggerName);
        this.client = client;
        this.pluginSettings = pluginSettings;
        this.cryptoService = cryptoService;
        this.objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.retryCount = pluginSettings.getNodeRetryCount();
        this.retryWaitDuration = pluginSettings.getNodeRetryWaitDuration();
    }

    /* -- protected methods  -- */

    protected boolean existsIndex(String indexes) {
        IndicesExistsRequestBuilder requestBuilder = client.admin().indices().prepareExists(indexes);
        IndicesExistsResponse response = requestBuilder.execute().actionGet();
        return response.isExists();
    }

    protected void deleteIndexIfExists(String indexName){
        if (!existsIndex(indexName)) {
            return;
        }
        if (logger.isInfoEnabled()) {
            logger.info(String.format("Deleting index [%s]", indexName));
        }

        DeleteIndexRequestBuilder deleteIndexRequestBuilder = client.admin().indices().prepareDelete(indexName);
        deleteIndexRequestBuilder.execute().actionGet();
    }

    protected String checkIssuerAndIndexDocumentFromJson(String index, String type, String json) {

        JsonNode actualObj = readAndVerifyIssuerSignature(json);
        String issuer = getIssuer(actualObj);


        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Indexing a %s from issuer [%s]", type, issuer.substring(0, 8)));
        }

        return indexDocumentFromJson(index, type, json);
    }

    protected String indexDocumentFromJson(String index, String type, String json) {
        IndexResponse response = client.prepareIndex(index, type)
                .setSource(json)
                .setRefresh(true)
                .execute().actionGet();
        return response.getId();
    }
    protected void checkIssuerAndUpdateDocumentFromJson(String index, String type, String id, String json) {

        JsonNode actualObj = readAndVerifyIssuerSignature(json);
        String issuer = getIssuer(actualObj);

        // Check same document issuer
        checkSameDocumentIssuer(index, type, id, issuer);

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Updating %s [%s] from issuer [%s]", type, id, issuer.substring(0, 8)));
        }

        updateDocumentFromJson(index, type, id, json);
    }

    protected void updateDocumentFromJson(String index, String type, String id, String json) {
        // Execute indexBlocksFromNode
        client.prepareUpdate(index, type, id)
                .setRefresh(true)
                .setDoc(json)
                .execute().actionGet();
    }

    protected JsonNode readAndVerifyIssuerSignature(String recordJson) throws ElasticsearchException {

        try {
            JsonNode actualObj = objectMapper.readTree(recordJson);
            readAndVerifyIssuerSignature(recordJson, actualObj);
            return actualObj;
        }
        catch(IOException e) {
            throw new InvalidFormatException("Invalid record JSON: " + e.getMessage(), e);
        }
    }

    protected void readAndVerifyIssuerSignature(String recordJson, JsonNode actualObj) throws ElasticsearchException {

        Set<String> fieldNames = ImmutableSet.copyOf(actualObj.fieldNames());
        if (!fieldNames.contains(Record.PROPERTY_ISSUER)
                || !fieldNames.contains(Record.PROPERTY_SIGNATURE)) {
            throw new InvalidFormatException(String.format("Invalid record JSON format. Required fields [%s,%s]", Record.PROPERTY_ISSUER, Record.PROPERTY_SIGNATURE));
        }
        String issuer = getMandatoryField(actualObj, Record.PROPERTY_ISSUER).asText();
        String signature = getMandatoryField(actualObj, Record.PROPERTY_SIGNATURE).asText();

        // Remove hash and signature
        recordJson = JacksonUtils.removeAttribute(recordJson, Record.PROPERTY_SIGNATURE);
        recordJson = JacksonUtils.removeAttribute(recordJson, Record.PROPERTY_HASH);

        if (!cryptoService.verify(recordJson, signature, issuer)) {
            throw new InvalidSignatureException("Invalid signature of JSON string");
        }

        // TODO: check issuer is in the WOT ?
    }

    protected void checkSameDocumentIssuer(String index, String type, String id, String expectedIssuer) throws ElasticsearchException {
        checkSameDocumentField(index, type, id, Record.PROPERTY_ISSUER, expectedIssuer);
    }

    protected void checkSameDocumentField(String index, String type, String id, String fieldName, String expectedvalue) throws ElasticsearchException {

        GetResponse response = client.prepareGet(index, type, id)
                .setFields(fieldName)
                .execute().actionGet();
        boolean failed = !response.isExists();
        if (failed) {
            throw new NotFoundException(String.format("Document [%s/%s/%s] not exists.", index, type, id));
        } else {
            String docValue = (String)response.getFields().get(fieldName).getValue();
            if (!Objects.equals(expectedvalue, docValue)) {
                throw new AccessDeniedException(String.format("Could not delete this document: not same [%s].", fieldName));
            }
        }
    }

    protected boolean isDocumentExists(String index, String type, String id) throws ElasticsearchException {
        GetResponse response = client.prepareGet(index, type, id)
                .setFetchSource(false)
                .execute().actionGet();
        return response.isExists();
    }

    protected void checkDocumentExists(String index, String type, String id) throws ElasticsearchException {
        if (!isDocumentExists(index, type, id)) {
            throw new NotFoundException(String.format("Document [%s/%s/%s] not exists.", index, type, id));
        }
    }


    protected String getIssuer(JsonNode actualObj) {
        return  getMandatoryField(actualObj, Record.PROPERTY_ISSUER).asText();
    }

    protected JsonNode getMandatoryField(JsonNode actualObj, String fieldName) {
        JsonNode value = actualObj.get(fieldName);
        if (value.isMissingNode()) {
            throw new InvalidFormatException(String.format("Invalid format. Expected field '%s'", fieldName));
        }
        return value;
    }

    /**
     * Retrieve some field from a document id, and check if all field not null
     * @param docId
     * @return
     */
    protected Map<String, Object> getMandatoryFieldsById(String index, String type, String docId, String... fieldNames) {
        Map<String, Object> fields = getFieldsById(index, type, docId, fieldNames);
        if (MapUtils.isEmpty(fields)) throw new NotFoundException(String.format("Document [%s/%s/%s] not exists.", index, type, docId));
        Arrays.stream(fieldNames).forEach((fieldName) -> {
            if (!fields.containsKey(fieldName)) throw new NotFoundException(String.format("Document [%s/%s/%s] should have the madatory field [%s].", index, type, docId, fieldName));
        });
        return fields;
    }

    /**
     * Retrieve some field from a document id
     * @param docId
     * @return
     */
    protected Map<String, Object> getFieldsById(String index, String type, String docId, String... fieldNames) {
        // Prepare request
        SearchRequestBuilder searchRequest = client
                .prepareSearch(index)
                .setTypes(type)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH);

        searchRequest.setQuery(QueryBuilders.idsQuery().ids(docId));
        searchRequest.addFields(fieldNames);

        // Execute query
        try {
            SearchResponse response = searchRequest.execute().actionGet();

            Map<String, Object> result = new HashMap<>();
            // Read query result
            SearchHit[] searchHits = response.getHits().getHits();
            for (SearchHit searchHit : searchHits) {
                Map<String, SearchHitField> hitFields = searchHit.getFields();
                for(String fieldName: hitFields.keySet()) {
                    result.put(fieldName, hitFields.get(fieldName).getValue());
                }
                break;
            }
            return result;
        }
        catch(SearchPhaseExecutionException e) {
            // Failed or no item on index
            throw new TechnicalException(String.format("[%s/%s] Unable to retrieve fields [%s] for id [%s]",
                    index, type,
                    Joiner.on(',').join(fieldNames).toString(),
                    docId), e);
        }
    }

    /**
     * Retrieve some field from a document id
     * @param index
     * @param type
     * @param ids
     * @param fieldName
     * @return
     */
    protected Map<String, Object> getFieldByIds(String index, String type, Set<String> ids, String fieldName) {
        // Prepare request
        SearchRequestBuilder searchRequest = client
                .prepareSearch(index)
                .setTypes(type)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH);

        searchRequest.setQuery(QueryBuilders.idsQuery().ids(ids));
        searchRequest.addFields(fieldName);

        // Execute query
        try {
            SearchResponse response = searchRequest.execute().actionGet();

            Map<String, Object> result = new HashMap<>();
            // Read query result
            SearchHit[] searchHits = response.getHits().getHits();
            for (SearchHit searchHit : searchHits) {
                Map<String, SearchHitField> hitFields = searchHit.getFields();
                if (hitFields.get(fieldName) != null) {
                    result.put(searchHit.getId(), hitFields.get(fieldName).getValue());
                }
            }
            return result;
        }
        catch(SearchPhaseExecutionException e) {
            // Failed or no item on index
            throw new TechnicalException(String.format("[%s/%s] Unable to retrieve field [%s] for ids [%s]",
                    index, type, fieldName,
                    Joiner.on(',').join(ids).toString()), e);
        }
    }

    /**
     * Retrieve a field from a document id
     * @param docId
     * @return
     */
    protected Object getFieldById(String index, String type, String docId, String fieldName) {

        Map<String, Object> result = getFieldsById(index, type, docId, fieldName);
        if (MapUtils.isEmpty(result)) {
            return null;
        }
        return result.get(fieldName);
    }

    protected <T> T getTypedFieldById(String index, String type, String docId, String fieldName) {
        return (T)getFieldById(index, type, docId, fieldName);
    }

    /**
     * Retrieve a document by id (safe mode)
     * @param docId
     * @return
     */
    public <T extends Object> T getSourceByIdOrNull(String index, String type, String docId, Class<T> classOfT, String... fieldNames) {
        try {
            return getSourceById(index, type, docId, classOfT, fieldNames);
        }
        catch(TechnicalException e) {
            return null; // not found
        }
    }

    /**
     * Retrieve a document by id
     * @param docId
     * @return
     */
    public <T extends Object> T getSourceById(String index, String type, String docId, Class<T> classOfT, String... fieldNames) {

        // Prepare request
        SearchRequestBuilder searchRequest = client
                .prepareSearch(index)
                .setSearchType(SearchType.QUERY_AND_FETCH);

        searchRequest.setQuery(QueryBuilders.idsQuery(type).ids(docId));
        if (CollectionUtils.isNotEmpty(fieldNames)) {
            searchRequest.setFetchSource(fieldNames, null);
        }
        else {
            searchRequest.setFetchSource(true); // full source
        }

        // Execute query
        try {
            SearchResponse response = searchRequest.execute().actionGet();

            if (response.getHits().getTotalHits() == 0) return null;

            // Read query result
            SearchHit[] searchHits = response.getHits().getHits();

            for (SearchHit searchHit : searchHits) {
                if (searchHit.source() != null) {
                    return objectMapper.readValue(searchHit.source(), classOfT);
                }
                break;
            }
            return null;
        }
        catch(SearchPhaseExecutionException | IOException e) {
            // Failed to get source
            throw new TechnicalException(String.format("[%s/%s] Error while getting [%s]",
                    index, type,
                    docId), e);
        }
    }

    protected void bulkFromClasspathFile(String classpathFile, String indexName, String indexType) {
        bulkFromClasspathFile(classpathFile, indexName, indexType, null);
    }

    protected void bulkFromClasspathFile(String classpathFile, String indexName, String indexType, StringReaderHandler handler) {
        InputStream is = null;
        try {
            is = getClass().getClassLoader().getResourceAsStream(classpathFile);
            if (is == null) {
                throw new TechnicalException(String.format("Could not retrieve data file [%s] need to fill index [%s]: ", classpathFile, indexName));
            }

            bulkFromStream(is, indexName, indexType, handler);
        }
        finally {
            if (is != null) {
                try  {
                    is.close();
                }
                catch(IOException e) {
                    // Silent is gold
                }
            }
        }
    }

    protected void bulkFromFile(File file, String indexName, String indexType) {
        bulkFromFile(file, indexName, indexType, null);
    }

    protected void bulkFromFile(File file, String indexName, String indexType, StringReaderHandler handler) {
        Preconditions.checkNotNull(file);
        Preconditions.checkArgument(file.exists());

        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(file));
            bulkFromStream(is, indexName, indexType, handler);
        }
        catch(FileNotFoundException e) {
            throw new TechnicalException(String.format("[%s] Could not find file %s", indexName, file.getPath()), e);
        }
        finally {
            if (is != null) {
                try  {
                    is.close();
                }
                catch(IOException e) {
                    // Silent is gold
                }
            }
        }
    }

    protected void bulkFromStream(InputStream is, String indexName, String indexType) {
        bulkFromStream(is, indexName, indexType, null);
    }

    protected void bulkFromStream(InputStream is, String indexName, String indexType, StringReaderHandler handler) {
        Preconditions.checkNotNull(is);
        BulkRequest bulkRequest = Requests.bulkRequest();

        BufferedReader br = null;

        try {
            br = new BufferedReader(new InputStreamReader(is));

            String line = br.readLine();
            StringBuilder builder = new StringBuilder();
            while(line != null) {
                line = line.trim();
                if (StringUtils.isNotBlank(line)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace(String.format("[%s] Add to bulk: %s", indexName, line));
                    }
                    if (handler != null) {
                        line = handler.onReadLine(line.trim());
                    }
                    builder.append(line).append('\n');
                }
                line = br.readLine();
            }

            byte[] data = builder.toString().getBytes();
            bulkRequest.add(new BytesArray(data), indexName, indexType, false);

        } catch(Exception e) {
            throw new TechnicalException(String.format("[%s] Error while inserting rows into %s", indexName, indexType), e);
        }
        finally {
            if (br != null) {
                try  {
                    br.close();
                }
                catch(IOException e) {
                    // Silent is gold
                }
            }
        }

        try {
            client.bulk(bulkRequest).actionGet();
        } catch(Exception e) {
            throw new TechnicalException(String.format("[%s] Error while inserting rows into %s", indexName, indexType), e);
        }
    }

    protected void flushDeleteBulk(final String index, final String type, BulkRequestBuilder bulkRequest) {
        if (bulkRequest.numberOfActions() > 0) {

            BulkResponse bulkResponse = bulkRequest.execute().actionGet();
            // If failures, continue but save missing blocks
            if (bulkResponse.hasFailures()) {
                // process failures by iterating through each bulk response item
                for (BulkItemResponse itemResponse : bulkResponse) {
                    boolean skip = !itemResponse.isFailed();
                    if (!skip) {
                        logger.debug(String.format("[%s/%s] Error while deleting doc [%s]: %s. Skipping this deletion.", index, type, itemResponse.getId(), itemResponse.getFailureMessage()));
                    }
                }
            }
        }
    }

    public interface StringReaderHandler {

        String onReadLine(String line);
    }

    public class AddSequenceAttributeHandler implements StringReaderHandler {
        private int order;
        private final String attributeName;
        private final Pattern filterPattern;
        public AddSequenceAttributeHandler(String attributeName, String filterRegex, int startValue) {
            this.order = startValue;
            this.attributeName = attributeName;
            this.filterPattern = Pattern.compile(filterRegex);
        }

        @Override
        public String onReadLine(String line) {
            // add 'order' field into
            if (filterPattern.matcher(line).matches()) {
                return String.format("%s, \"%s\": %d}",
                        line.substring(0, line.length()-1),
                        attributeName,
                        order++);
            }
            return line;
        }
    }

    protected <T> T executeWithRetry(RetryFunction<T> retryFunction) throws TechnicalException{
        int retry = 0;
        while (retry < retryCount) {
            try {
                return retryFunction.execute();
            } catch (TechnicalException e) {
                retry++;

                if (retry == retryCount) {
                    throw e;
                }

                if (logger.isDebugEnabled()) {
                    logger.debug(I18n.t("duniter4j.removeServiceUtils.waitThenRetry", e.getMessage(), retry, retryCount));
                }

                try {
                    Thread.sleep(retryWaitDuration); // waiting
                } catch (InterruptedException e2) {
                    throw new TechnicalException(e2);
                }
            }
        }

        throw new TechnicalException("Error while trying to execute a function with retry");
    }

    public interface RetryFunction<T> {

        T execute() throws TechnicalException;
    }

}

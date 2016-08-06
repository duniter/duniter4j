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


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonSyntaxException;
import org.duniter.core.beans.Bean;
import org.duniter.core.client.model.elasticsearch.Record;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.service.CryptoService;
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
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.*;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Benoit on 08/04/2015.
 */
public abstract class AbstractService implements Bean {

    protected static final String JSON_STRING_PROPERTY_REGEX = "[,]?[\"\\s\\n\\r]*%s[\"]?[\\s\\n\\r]*:[\\s\\n\\r]*\"[^\"]+\"";
    protected static final String REGEX_WORD_SEPARATOR = "[-\\t@# ]+";
    protected static final String REGEX_SPACE = "[\\t\\n\\r ]+";

    protected final ESLogger logger;
    protected final Client client;
    protected final PluginSettings pluginSettings;
    protected final ObjectMapper objectMapper;
    protected final CryptoService cryptoService;

    public AbstractService(Client client, PluginSettings pluginSettings, CryptoService cryptoService) {
        this.logger = Loggers.getLogger(getClass());
        this.client = client;
        this.pluginSettings = pluginSettings;
        this.cryptoService = cryptoService;
        this.objectMapper = new ObjectMapper();
    }

    public AbstractService(Client client, PluginSettings pluginSettings) {
        this.logger = Loggers.getLogger(getClass());
        this.client = client;
        this.pluginSettings = pluginSettings;
        this.cryptoService = null;
        this.objectMapper = new ObjectMapper();
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

    protected XContentBuilder createDefaultAnalyzer() {
        try {
            XContentBuilder analyzer = XContentFactory.jsonBuilder().startObject().startObject("analyzer")
                    .startObject("custom_french_analyzer")
                    .field("tokenizer", "letter")
                    .field("filter", "asciifolding", "lowercase", "french_stem", "elision", "stop")
                    .endObject()
                    .startObject("tag_analyzer")
                    .field("tokenizer", "keyword")
                    .field("filter", "asciifolding", "lowercase")
                    .endObject()
                    .endObject().endObject();

            return analyzer;
        } catch(IOException e) {
            throw new TechnicalException("Error while preparing default index analyzer: " + e.getMessage(), e);
        }
    }

    protected JsonNode readAndVerifyIssuerSignature(String recordJson) throws ElasticsearchException {

        try {
            JsonNode actualObj = objectMapper.readTree(recordJson);
            readAndVerifyIssuerSignature(recordJson, actualObj);
            return actualObj;
        }
        catch(IOException | JsonSyntaxException e) {
            throw new InvalidFormatException("Invalid record JSON: " + e.getMessage(), e);
        }
    }

    protected void readAndVerifyIssuerSignature(String recordJson, JsonNode actualObj) throws ElasticsearchException {

        try {
            Set<String> fieldNames = ImmutableSet.copyOf(actualObj.fieldNames());
            if (!fieldNames.contains(Record.PROPERTY_ISSUER)
                    || !fieldNames.contains(Record.PROPERTY_SIGNATURE)) {
                throw new InvalidFormatException(String.format("Invalid record JSON format. Required fields [%s,%s]", Record.PROPERTY_ISSUER, Record.PROPERTY_SIGNATURE));
            }
            String issuer = actualObj.get(Record.PROPERTY_ISSUER).asText();
            String signature = actualObj.get(Record.PROPERTY_SIGNATURE).asText();

            String recordNoSign = recordJson.replaceAll(String.format(JSON_STRING_PROPERTY_REGEX, Record.PROPERTY_SIGNATURE), "")
                    .replaceAll(String.format(JSON_STRING_PROPERTY_REGEX, Record.PROPERTY_HASH), "");

            if (!cryptoService.verify(recordNoSign, signature, issuer)) {
                throw new InvalidSignatureException("Invalid signature for JSON string: " + recordNoSign);
            }

            // TODO: check issuer is in the WOT ?
        }
        catch(JsonSyntaxException e) {
            throw new InvalidFormatException("Invalid record JSON: " + e.getMessage(), e);
        }
    }

    protected void checkSameDocumentIssuer(String index, String type, String id, String expectedIssuer) throws ElasticsearchException {

        GetResponse response = client.prepareGet(index, type, id)
                .setFields(Record.PROPERTY_ISSUER)
                .execute().actionGet();
        boolean failed = !response.isExists();
        if (failed) {
            throw new NotFoundException(String.format("Document [%s/%s/%s] not exists.", index, type, id));
        } else {
            String docIssuer = (String)response.getFields().get(Record.PROPERTY_ISSUER).getValue();
            if (!Objects.equals(expectedIssuer, docIssuer)) {
                throw new AccessDeniedException(String.format("Could not delete this document: not issuer."));
            }
        }
    }



    protected String getIssuer(JsonNode actualObj) {
        return  actualObj.get(Record.PROPERTY_ISSUER).asText();
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
}

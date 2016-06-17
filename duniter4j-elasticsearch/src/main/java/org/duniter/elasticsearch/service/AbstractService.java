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


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import org.duniter.core.beans.Bean;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.util.StringUtils;
import org.duniter.elasticsearch.PluginSettings;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequestBuilder;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.component.Lifecycle;
import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.component.LifecycleListener;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.DeprecationLogger;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.node.NodeBuilder;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by Benoit on 08/04/2015.
 */
public abstract class AbstractService implements Bean {

    protected final ESLogger logger;
    protected final Client client;
    protected final PluginSettings pluginSettings;
    protected final ObjectMapper objectMapper;

    @Inject
    public AbstractService(Client client, PluginSettings pluginSettings) {
        this.logger = Loggers.getLogger(getClass());
        this.client = client;
        this.pluginSettings = pluginSettings;
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

    protected void bulkFromClasspathFile(String classpathFile, String indexName, String indexType) {
        InputStream is = null;
        try {
            is = getClass().getClassLoader().getResourceAsStream(classpathFile);
            if (is == null) {
                throw new TechnicalException(String.format("Could not retrieve data file [%s] need to fill index [%s]: ", classpathFile, indexName));
            }

            bulkFromStream(is, indexName, indexType);
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
        Preconditions.checkNotNull(file);
        Preconditions.checkArgument(file.exists());

        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(file));
            bulkFromStream(is, indexName, indexType);
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
        Preconditions.checkNotNull(is);
        BulkRequest bulkRequest = Requests.bulkRequest();

        BufferedReader br = null;

        try {
            br = new BufferedReader(new InputStreamReader(is));

            String line = br.readLine();
            StringBuilder builder = new StringBuilder();
            while(line != null) {
                if (StringUtils.isNotBlank(line)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace(String.format("[%s] Add to bulk: %s", indexName, line));
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
}

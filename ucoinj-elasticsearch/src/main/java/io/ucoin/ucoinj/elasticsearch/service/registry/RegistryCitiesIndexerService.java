package io.ucoin.ucoinj.elasticsearch.service.registry;

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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.ucoin.ucoinj.core.client.model.bma.gson.GsonUtils;
import io.ucoin.ucoinj.core.exception.TechnicalException;
import io.ucoin.ucoinj.core.util.StringUtils;
import io.ucoin.ucoinj.elasticsearch.config.Configuration;
import io.ucoin.ucoinj.elasticsearch.service.BaseIndexerService;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Benoit on 30/03/2015.
 */
public class RegistryCitiesIndexerService extends BaseIndexerService {

    private static final Logger log = LoggerFactory.getLogger(RegistryCitiesIndexerService.class);

    private static final String CITIES_CLASSPATH_FILE = "cities/countriesToCities.json";

    public static final String INDEX_NAME = "registry";
    public static final String INDEX_TYPE = "city";

    private Gson gson;

    private Configuration config;

    public RegistryCitiesIndexerService() {
        gson = GsonUtils.newBuilder().create();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        config = Configuration.instance();
    }

    @Override
    public void close() throws IOException {
        super.close();
        config = null;
        gson = null;
    }

    /**
     * Delete currency index, and all data
     * @throws JsonProcessingException
     */
    public void deleteIndex() throws JsonProcessingException {
        deleteIndexIfExists(INDEX_NAME);
    }


    public boolean existsIndex() {
        return super.existsIndex(INDEX_NAME);
    }

    /**
     * Create index need for currency registry, if need
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
     * Create index need for category registry
     * @throws JsonProcessingException
     */
    public void createIndex() throws JsonProcessingException {
        log.info(String.format("Creating index [%s/%s]", INDEX_NAME, INDEX_TYPE));

        CreateIndexRequestBuilder createIndexRequestBuilder = getClient().admin().indices().prepareCreate(INDEX_NAME);
        Settings indexSettings = Settings.settingsBuilder()
                .put("number_of_shards", 1)
                .put("number_of_replicas", 1)
                .put("analyzer", createDefaultAnalyzer())
                .build();
        createIndexRequestBuilder.setSettings(indexSettings);
        createIndexRequestBuilder.addMapping(INDEX_TYPE, createIndexMapping());
        createIndexRequestBuilder.execute().actionGet();
    }

    public void initCities() {
        if (log.isDebugEnabled()) {
            log.debug("Initializing all registry cities");
        }

        // Insert cities
        BulkRequest bulkRequest = Requests.bulkRequest();

        InputStream ris = null;
        try {
            ris = getClass().getClassLoader().getResourceAsStream(CITIES_CLASSPATH_FILE);
            if (ris == null) {
                throw new TechnicalException(String.format("Could not retrieve data file [%s] need to fill index [%s/%s]: ", CITIES_CLASSPATH_FILE, INDEX_NAME, INDEX_TYPE));
            }

            StringBuilder builder = new StringBuilder();
            BufferedReader bf = new BufferedReader(new InputStreamReader(ris), 2048);
            char[] buf = new char[2048];
            int len;
            boolean ignoreBOM = true;
            while((len = bf.read(buf)) != -1) {
                if (ignoreBOM) {
                    builder.append(buf, 2, len-2);
                    ignoreBOM = false;
                }
                else {
                    builder.append(buf, 0, len);
                }
            }


            java.lang.reflect.Type typeOfHashMap = new TypeToken<Map<String, String[]>>() { }.getType();

            Map<String, String[]> cities = new HashMap<>();
            cities.put("chine", new String[]{"ABC", "def"});
            String json = gson.toJson(cities, typeOfHashMap);
            log.debug(String.format("test json: ", json));
            log.debug(String.format("test json: ", builder.substring(0,19)));

            Map<String, String[]> citiesByCountry = gson.fromJson(builder.toString(), typeOfHashMap);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Register %s countries", citiesByCountry.size()));
            }

            //bulkRequest.add(new BytesArray(data), indexName, indexType, false);

        } catch(Exception e) {
            throw new TechnicalException(String.format("[%s] Error while inserting rows", INDEX_TYPE), e);
        }
        finally {
            if (ris != null) {
                try  {
                    ris.close();
                }
                catch(IOException e) {
                    // Silent is gold
                }
            }
        }

        try {
            getClient().bulk(bulkRequest).actionGet();
        } catch(Exception e) {
            throw new TechnicalException(String.format("[%s] Error while inserting rows into %s", INDEX_NAME, INDEX_TYPE), e);
        }
    }

    /* -- Internal methods -- */


    public XContentBuilder createIndexMapping() {
        try {
            XContentBuilder mapping = XContentFactory.jsonBuilder().startObject().startObject(INDEX_TYPE)
                    .startObject("properties")

                    // city
                    .startObject("city")
                    .field("type", "string")
                    .endObject()

                    // country
                    .startObject("country")
                    .field("type", "string")
                    .endObject()

                    .endObject()
                    .endObject().endObject();

            return mapping;
        }
        catch(IOException ioe) {
            throw new TechnicalException(String.format("Error while getting mapping for index [%s/%s]: %s", INDEX_NAME, INDEX_TYPE, ioe.getMessage()), ioe);
        }
    }
}

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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Map;

/**
 * Created by Benoit on 30/03/2015.
 */
public class RegistryCitiesIndexerService extends BaseIndexerService {

    private static final Logger log = LoggerFactory.getLogger(RegistryCitiesIndexerService.class);

    private static final String CITIES_BULK_FILENAME = "registry-cities-bulk-insert.json";

    private static final String CITIES_SOURCE_CLASSPATH_FILE = "cities/countriesToCities.json";

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

        File bulkFile = createCitiesBulkFile();

        // Insert cities
        bulkFromFile(bulkFile, INDEX_NAME, INDEX_TYPE);
    }


    /* -- Internal methods -- */


    public XContentBuilder createIndexMapping() {
        try {
            XContentBuilder mapping = XContentFactory.jsonBuilder().startObject().startObject(INDEX_TYPE)
                    .startObject("properties")

                    // city
                    .startObject("name")
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

    public File createCitiesBulkFile() {

        File result = new File(config.getTempDirectory(), CITIES_BULK_FILENAME);

        InputStream ris = null;
        BufferedReader bf = null;
        FileWriter fw = null;
        try {
            if (result.exists()) {
                FileUtils.forceDelete(result);
            }
            else if (!result.getParentFile().exists()) {
                FileUtils.forceMkdir(result.getParentFile());
            }

            ris = getClass().getClassLoader().getResourceAsStream(CITIES_SOURCE_CLASSPATH_FILE);
            if (ris == null) {
                throw new TechnicalException(String.format("Could not retrieve file [%s] from test classpath. Make sure git submodules has been initialized before building.", CITIES_SOURCE_CLASSPATH_FILE));
            }

            boolean firstLine = true;
            java.lang.reflect.Type typeOfHashMap = new TypeToken<Map<String, String[]>>() { }.getType();

            Gson gson = GsonUtils.newBuilder().create();

            StringBuilder builder = new StringBuilder();
            bf = new BufferedReader(
                    new InputStreamReader(
                            ris, "UTF-16LE"), 2048);

            fw = new FileWriter(result);
            char[] buf = new char[2048];
            int len;

            while((len = bf.read(buf)) != -1) {
                String bufStr = new String(buf, 0, len);

                if (firstLine) {
                    // Remove UTF-16 BOM char
                    int objectStartIndex = bufStr.indexOf('\uFEFF');
                    if (objectStartIndex != -1) {
                        bufStr = bufStr.substring(objectStartIndex);
                    }
                    firstLine=false;
                }

                int arrayEndIndex = bufStr.indexOf("],\"");
                if (arrayEndIndex == -1) {
                    arrayEndIndex = bufStr.indexOf("]}");
                }

                if (arrayEndIndex == -1) {
                    builder.append(bufStr);
                }
                else {
                    builder.append(bufStr.substring(0, arrayEndIndex+1));
                    builder.append("}");
                    if (log.isTraceEnabled()) {
                        log.trace(builder.toString());
                    }
                    Map<String, String[]> citiesByCountry = gson.fromJson(builder.toString(), typeOfHashMap);

                    builder.setLength(0);
                    for (String country: citiesByCountry.keySet()) {
                        if (StringUtils.isNotBlank(country)) {
                            for (String city : citiesByCountry.get(country)) {
                                if (StringUtils.isNotBlank(city)) {
                                    fw.write(String.format("{\"index\":{\"_id\" : \"%s-%s\"}}\n", country, city));
                                    fw.write(String.format("{\"country\":\"%s\", \"name\":\"%s\"}\n", country, city));
                                }
                            }
                        }
                    }

                    fw.flush();

                    // reset and prepare buffer for next country
                    builder.setLength(0);
                    builder.append("{");
                    if (arrayEndIndex+2 < bufStr.length()) {
                        builder.append(bufStr.substring(arrayEndIndex+2));
                    }
                }
            }

            fw.close();
            bf.close();

        } catch(Exception e) {
            throw new TechnicalException(String.format("Error while creating cities file [%s]", result.getName()), e);
        }
        finally {
            IOUtils.closeQuietly(bf);
            IOUtils.closeQuietly(ris);
            IOUtils.closeQuietly(fw);
        }

        return result;
    }
}

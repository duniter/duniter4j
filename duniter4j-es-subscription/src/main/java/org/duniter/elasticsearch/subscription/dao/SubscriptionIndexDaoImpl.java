package org.duniter.elasticsearch.subscription.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.duniter.core.exception.TechnicalException;
import org.duniter.elasticsearch.dao.AbstractIndexDao;
import org.duniter.elasticsearch.dao.IndexTypeDao;
import org.duniter.elasticsearch.dao.handler.AddSequenceAttributeHandler;
import org.duniter.elasticsearch.subscription.PluginSettings;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by blavenie on 03/04/17.
 */
public class SubscriptionIndexDaoImpl extends AbstractIndexDao<SubscriptionIndexDao> implements SubscriptionIndexDao {


    private static final String CATEGORIES_BULK_CLASSPATH_FILE = "subscription-categories-bulk-insert.json";

    private PluginSettings pluginSettings;
    private List<IndexTypeDao<?>> indexTypeDaos = new ArrayList<>();

    @Inject
    public SubscriptionIndexDaoImpl(PluginSettings pluginSettings) {
        super(SubscriptionIndexDao.INDEX);

        this.pluginSettings = pluginSettings;
    }

    public SubscriptionIndexDao register(IndexTypeDao<?> indexTypeDao) {
        indexTypeDaos.add(indexTypeDao);
        return this;
    }

    @Override
    protected void createIndex() throws JsonProcessingException {
        logger.info(String.format("Creating index [%s]", INDEX));

        CreateIndexRequestBuilder createIndexRequestBuilder = client.admin().indices().prepareCreate(INDEX);
        org.elasticsearch.common.settings.Settings indexSettings = org.elasticsearch.common.settings.Settings.settingsBuilder()
                .put("number_of_shards", 3)
                .put("number_of_replicas", 1)
                //.put("analyzer", createDefaultAnalyzer())
                .build();
        createIndexRequestBuilder.setSettings(indexSettings);
        indexTypeDaos.forEach(indexTypeDao -> createIndexRequestBuilder.addMapping(indexTypeDao.getType(), indexTypeDao.createTypeMapping()));
        createIndexRequestBuilder.addMapping(CATEGORY_TYPE, createCategoryTypeMapping());
        createIndexRequestBuilder.execute().actionGet();

        // Fill categories
        fillCategories();
    }



    protected XContentBuilder createCategoryTypeMapping() {
        try {
            XContentBuilder mapping = XContentFactory.jsonBuilder().startObject()
                    .startObject(CATEGORY_TYPE)
                    .startObject("properties")

                    // name
                    .startObject("name")
                    .field("type", "string")
                    .field("analyzer", pluginSettings.getDefaultStringAnalyzer())
                    .endObject()

                    // parent
                    .startObject("parent")
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()

                    .endObject()
                    .endObject().endObject();

            return mapping;
        }
        catch(IOException ioe) {
            throw new TechnicalException(String.format("Error while getting mapping for index [%s/%s]: %s", getIndex(), CATEGORY_TYPE, ioe.getMessage()), ioe);
        }
    }

    protected void fillCategories() {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("[%s/%s] Fill data", getIndex(), CATEGORY_TYPE));
        }

        // Insert categories
        client.bulkFromClasspathFile(CATEGORIES_BULK_CLASSPATH_FILE, getIndex(), CATEGORY_TYPE,
                // Add order attribute
                new AddSequenceAttributeHandler("order", "\\{.*\"name\".*\\}", 1));
    }
}

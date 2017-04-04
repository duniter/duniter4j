package org.duniter.elasticsearch.gchange.dao.registry;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.duniter.core.exception.TechnicalException;
import org.duniter.elasticsearch.dao.AbstractIndexDao;
import org.duniter.elasticsearch.dao.AbstractIndexTypeDao;
import org.duniter.elasticsearch.dao.IndexDao;
import org.duniter.elasticsearch.dao.IndexTypeDao;
import org.duniter.elasticsearch.dao.handler.AddSequenceAttributeHandler;
import org.duniter.elasticsearch.gchange.PluginSettings;
import org.duniter.elasticsearch.gchange.dao.AbstractCommentDaoImpl;
import org.duniter.elasticsearch.gchange.dao.AbstractRecordDaoImpl;
import org.duniter.elasticsearch.gchange.dao.CommentDao;
import org.duniter.elasticsearch.gchange.dao.RecordDao;
import org.duniter.elasticsearch.gchange.service.RegistryService;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.IOException;

/**
 * Created by blavenie on 03/04/17.
 */
public class RegistryIndexDaoImpl extends AbstractIndexDao<RegistryIndexDao> implements RegistryIndexDao {


    private static final String CATEGORIES_BULK_CLASSPATH_FILE = "registry-categories-bulk-insert.json";

    private PluginSettings pluginSettings;
    private IndexTypeDao<?> categoryDao;
    private RecordDao recordDao;
    private CommentDao commentDao;

    @Inject
    public RegistryIndexDaoImpl(PluginSettings pluginSettings, RegistryRecordDao recordDao, RegistryCommentDao commentDao) {
        super(RegistryIndexDao.INDEX);

        this.pluginSettings = pluginSettings;
        this.commentDao = commentDao;
        this.recordDao = recordDao;
        this.categoryDao = createCategoryDao(pluginSettings);
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
        createIndexRequestBuilder.addMapping(recordDao.getType(), recordDao.createTypeMapping());
        createIndexRequestBuilder.addMapping(commentDao.getType(), commentDao.createTypeMapping());
        createIndexRequestBuilder.addMapping(categoryDao.getType(), categoryDao.createTypeMapping());
        createIndexRequestBuilder.execute().actionGet();

        // Fill categories
        fillRecordCategories();
    }

    public void fillRecordCategories() {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("[%s/%s] Fill data", INDEX, RegistryIndexDao.CATEGORY_TYPE));
        }

        // Insert categories
        categoryDao.bulkFromClasspathFile(CATEGORIES_BULK_CLASSPATH_FILE,
                // Add order attribute
                new AddSequenceAttributeHandler("order", "\\{.*\"name\".*\\}", 1));
    }


    protected IndexTypeDao<?> createCategoryDao(final PluginSettings settings) {
        return new AbstractIndexTypeDao(INDEX, RegistryIndexDao.CATEGORY_TYPE) {
            @Override
            protected void createIndex() throws JsonProcessingException {
                throw new TechnicalException("not implemented");
            }

            @Override
            public XContentBuilder createTypeMapping() {
                try {
                    XContentBuilder mapping = XContentFactory.jsonBuilder().startObject()
                            .startObject(getType())
                            .startObject("properties")

                            // name
                            .startObject("name")
                            .field("type", "string")
                            .field("analyzer", settings.getDefaultStringAnalyzer())
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
                    throw new TechnicalException(String.format("Error while getting mapping for index [%s/%s]: %s", getIndex(), getType(), ioe.getMessage()), ioe);
                }
            }
        };
    }
}

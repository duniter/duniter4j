package org.duniter.elasticsearch.subscription.dao.execution;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.duniter.core.client.model.bma.BlockchainBlock;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.util.CollectionUtils;
import org.duniter.core.util.Preconditions;
import org.duniter.core.util.StringUtils;
import org.duniter.elasticsearch.subscription.PluginSettings;
import org.duniter.elasticsearch.subscription.dao.AbstractSubscriptionIndexTypeDao;
import org.duniter.elasticsearch.subscription.dao.SubscriptionIndexDao;
import org.duniter.elasticsearch.subscription.model.SubscriptionExecution;
import org.duniter.elasticsearch.subscription.model.SubscriptionRecord;
import org.duniter.elasticsearch.subscription.model.email.EmailSubscription;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by blavenie on 03/04/17.
 */
public class SubscriptionExecutionDaoImpl extends AbstractSubscriptionIndexTypeDao<SubscriptionExecutionDaoImpl> implements SubscriptionExecutionDao<SubscriptionExecutionDaoImpl> {

    @Inject
    public SubscriptionExecutionDaoImpl(PluginSettings pluginSettings, SubscriptionIndexDao indexDao) {
        super(SubscriptionIndexDao.INDEX, TYPE, pluginSettings);

        indexDao.register(this);
    }

    @Override
    public SubscriptionExecution getLastExecution(SubscriptionRecord record) {
        Preconditions.checkNotNull(record);
        Preconditions.checkNotNull(record.getIssuer());
        Preconditions.checkNotNull(record.getType());
        Preconditions.checkNotNull(record.getId());

        return getLastExecution(record.getIssuer(), record.getType(), record.getId());
    }

    @Override
    public SubscriptionExecution getLastExecution(String recipient, String recordType, String recordId) {

        BoolQueryBuilder query = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery(SubscriptionExecution.PROPERTY_RECIPIENT, recipient))
                .must(QueryBuilders.termsQuery(SubscriptionExecution.PROPERTY_RECORD_TYPE, recordType))
                .must(QueryBuilders.termQuery(SubscriptionExecution.PROPERTY_RECORD_ID, recordId));

        SearchResponse response = client.prepareSearch(SubscriptionIndexDao.INDEX)
                .setTypes(SubscriptionExecutionDao.TYPE)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(query)
                .setFetchSource(true)
                .setFrom(0).setSize(1)
                .addSort(SubscriptionExecution.PROPERTY_TIME, SortOrder.DESC)
                .get();

        if (response.getHits().getTotalHits() == 0) return null;

        SearchHit hit = response.getHits().getHits()[0];
        return client.readSourceOrNull(hit, SubscriptionExecution.class);
    }

    @Override
    public Long getLastExecutionTime(SubscriptionRecord record) {
        Preconditions.checkNotNull(record);
        Preconditions.checkNotNull(record.getIssuer());
        Preconditions.checkNotNull(record.getType());
        Preconditions.checkNotNull(record.getId());

        return getLastExecutionTime(record.getIssuer(), record.getType(), record.getId());
    }

    @Override
    public Long getLastExecutionTime(String recipient, String recordType, String recordId) {

        BoolQueryBuilder query = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery(SubscriptionExecution.PROPERTY_RECIPIENT, recipient))
                .must(QueryBuilders.termQuery(SubscriptionExecution.PROPERTY_RECORD_ID, recordId))
                .must(QueryBuilders.termsQuery(SubscriptionExecution.PROPERTY_RECORD_ID, recordType));

        SearchResponse response = client.prepareSearch(SubscriptionIndexDao.INDEX)
                .setTypes(SubscriptionExecutionDao.TYPE)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(query)
                .addField(SubscriptionExecution.PROPERTY_TIME)
                .setFrom(0).setSize(1)
                .addSort(SubscriptionExecution.PROPERTY_TIME, SortOrder.DESC)
                .get();

        if (response.getHits().getTotalHits() == 0) return null;
        SearchHit hit = response.getHits().getHits()[0];
        return hit.field(SubscriptionExecution.PROPERTY_TIME).getValue();
    }

    @Override
    public XContentBuilder createTypeMapping() {
        try {
            XContentBuilder mapping = XContentFactory.jsonBuilder().startObject()
                    .startObject(getType())
                    .startObject("properties")

                    // issuer
                    .startObject("issuer")
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()

                    // recipient
                    .startObject("recipient")
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()

                    // record type
                    .startObject("recordType")
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()

                    // record id
                    .startObject("recordId")
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()

                    // time
                    .startObject("time")
                    .field("type", "integer")
                    .endObject()

                    // hash
                    .startObject("hash")
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()

                    // signature
                    .startObject("signature")
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

}

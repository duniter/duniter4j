package org.duniter.elasticsearch.subscription.dao;

import org.duniter.elasticsearch.dao.IndexDao;
import org.duniter.elasticsearch.dao.IndexTypeDao;

/**
 * Created by blavenie on 03/04/17.
 */
public interface SubscriptionIndexDao extends IndexDao<SubscriptionIndexDao> {
    String INDEX = "subscription";
    String CATEGORY_TYPE = "category";

    SubscriptionIndexDao register(IndexTypeDao<?> indexTypeDao);
}

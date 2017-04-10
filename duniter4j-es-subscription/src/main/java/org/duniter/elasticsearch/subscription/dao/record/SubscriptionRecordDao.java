package org.duniter.elasticsearch.subscription.dao.record;

import org.duniter.elasticsearch.subscription.dao.SubscriptionIndexTypeDao;
import org.duniter.elasticsearch.subscription.model.SubscriptionRecord;

import java.util.List;

/**
 * Created by blavenie on 03/04/17.
 */
public interface SubscriptionRecordDao<T extends SubscriptionIndexTypeDao> extends SubscriptionIndexTypeDao<T> {

    String TYPE = "record";

    List<SubscriptionRecord> getSubscriptions(int from, int size, String recipient, String... types);
}

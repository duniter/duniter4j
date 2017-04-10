package org.duniter.elasticsearch.subscription.dao.execution;

import org.duniter.elasticsearch.subscription.dao.SubscriptionIndexTypeDao;
import org.duniter.elasticsearch.subscription.model.SubscriptionExecution;
import org.duniter.elasticsearch.subscription.model.SubscriptionRecord;

import java.util.List;

/**
 * Created by blavenie on 03/04/17.
 */
public interface SubscriptionExecutionDao<T extends SubscriptionIndexTypeDao> extends SubscriptionIndexTypeDao<T> {

    String TYPE = "execution";

    SubscriptionExecution getLastExecution(SubscriptionRecord record);

    SubscriptionExecution getLastExecution(String recipient, String subscriptionType, String recordId);

    Long getLastExecutionTime(String recipient, String subscriptionType, String recordId);

    Long getLastExecutionTime(SubscriptionRecord record);
}

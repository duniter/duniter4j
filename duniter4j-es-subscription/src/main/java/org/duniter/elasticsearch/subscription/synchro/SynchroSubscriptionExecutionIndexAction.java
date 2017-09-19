package org.duniter.elasticsearch.subscription.synchro;

import org.duniter.core.service.CryptoService;
import org.duniter.elasticsearch.client.Duniter4jClient;
import org.duniter.elasticsearch.synchro.SynchroService;
import org.duniter.elasticsearch.subscription.dao.SubscriptionIndexDao;
import org.duniter.elasticsearch.subscription.dao.execution.SubscriptionExecutionDao;
import org.duniter.elasticsearch.threadpool.ThreadPool;
import org.duniter.elasticsearch.user.PluginSettings;
import org.duniter.elasticsearch.synchro.AbstractSynchroAction;
import org.elasticsearch.common.inject.Inject;

public class SynchroSubscriptionExecutionIndexAction extends AbstractSynchroAction {

    @Inject
    public SynchroSubscriptionExecutionIndexAction(Duniter4jClient client,
                                                   PluginSettings pluginSettings,
                                                   CryptoService cryptoService,
                                                   ThreadPool threadPool,
                                                   SynchroService synchroService) {
        super(SubscriptionIndexDao.INDEX, SubscriptionExecutionDao.TYPE, client, pluginSettings.getDelegate(),
                cryptoService, threadPool);

        synchroService.register(this);
    }

}

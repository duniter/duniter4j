package org.duniter.elasticsearch.subscription.synchro;

import org.duniter.core.service.CryptoService;
import org.duniter.elasticsearch.client.Duniter4jClient;
import org.duniter.elasticsearch.service.synchro.AbstractSynchroAction;
import org.duniter.elasticsearch.service.synchro.SynchroService;
import org.duniter.elasticsearch.subscription.dao.SubscriptionIndexDao;
import org.duniter.elasticsearch.subscription.dao.record.SubscriptionRecordDao;
import org.duniter.elasticsearch.threadpool.ThreadPool;
import org.duniter.elasticsearch.user.PluginSettings;
import org.elasticsearch.common.inject.Inject;

public class SynchroSubscriptionRecordAction extends AbstractSynchroAction {

    @Inject
    public SynchroSubscriptionRecordAction(Duniter4jClient client,
                                           PluginSettings pluginSettings,
                                           ThreadPool threadPool,
                                           CryptoService cryptoService,
                                           SynchroService synchroService) {
        super(SubscriptionIndexDao.INDEX, SubscriptionRecordDao.TYPE, client, pluginSettings, cryptoService, threadPool);

        setEnableUpdate(true); // with update

        synchroService.register(this);
    }

}

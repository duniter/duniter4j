package org.duniter.elasticsearch.user.synchro.page;

import org.duniter.core.service.CryptoService;
import org.duniter.elasticsearch.client.Duniter4jClient;
import org.duniter.elasticsearch.service.synchro.AbstractSynchroAction;
import org.duniter.elasticsearch.service.synchro.SynchroService;
import org.duniter.elasticsearch.threadpool.ThreadPool;
import org.duniter.elasticsearch.user.PluginSettings;
import org.duniter.elasticsearch.user.dao.page.RegistryIndexDao;
import org.duniter.elasticsearch.user.dao.page.RegistryRecordDao;
import org.elasticsearch.common.inject.Inject;

public class SynchroPageRecordAction extends AbstractSynchroAction {

    @Inject
    public SynchroPageRecordAction(Duniter4jClient client,
                                   PluginSettings pluginSettings,
                                   CryptoService cryptoService,
                                   ThreadPool threadPool,
                                   SynchroService synchroService) {
        super(RegistryIndexDao.INDEX, RegistryRecordDao.TYPE, client, pluginSettings, cryptoService, threadPool);

        setEnableUpdate(true); // with update

        synchroService.register(this);
    }

}

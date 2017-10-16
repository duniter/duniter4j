package org.duniter.elasticsearch.user.synchro.page;

import org.duniter.core.service.CryptoService;
import org.duniter.elasticsearch.client.Duniter4jClient;
import org.duniter.elasticsearch.synchro.SynchroService;
import org.duniter.elasticsearch.threadpool.ThreadPool;
import org.duniter.elasticsearch.user.PluginSettings;
import org.duniter.elasticsearch.user.dao.page.PageIndexDao;
import org.duniter.elasticsearch.user.dao.page.PageRecordDao;
import org.duniter.elasticsearch.synchro.AbstractSynchroAction;
import org.elasticsearch.common.inject.Inject;

public class SynchroPageRecordAction extends AbstractSynchroAction {

    @Inject
    public SynchroPageRecordAction(Duniter4jClient client,
                                   PluginSettings pluginSettings,
                                   CryptoService cryptoService,
                                   ThreadPool threadPool,
                                   SynchroService synchroService) {
        super(PageIndexDao.INDEX, PageRecordDao.TYPE, client, pluginSettings.getDelegate(), cryptoService, threadPool);

        setEnableUpdate(true); // with update

        synchroService.register(this);
    }

}

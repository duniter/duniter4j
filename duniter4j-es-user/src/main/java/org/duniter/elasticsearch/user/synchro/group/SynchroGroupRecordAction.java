package org.duniter.elasticsearch.user.synchro.group;

import org.duniter.core.service.CryptoService;
import org.duniter.elasticsearch.client.Duniter4jClient;
import org.duniter.elasticsearch.synchro.SynchroService;
import org.duniter.elasticsearch.threadpool.ThreadPool;
import org.duniter.elasticsearch.user.PluginSettings;
import org.duniter.elasticsearch.user.service.GroupService;
import org.duniter.elasticsearch.synchro.AbstractSynchroAction;
import org.elasticsearch.common.inject.Inject;

public class SynchroGroupRecordAction extends AbstractSynchroAction {

    @Inject
    public SynchroGroupRecordAction(Duniter4jClient client,
                                    PluginSettings pluginSettings,
                                    CryptoService cryptoService,
                                    ThreadPool threadPool,
                                    SynchroService synchroService) {
        super(GroupService.INDEX, GroupService.RECORD_TYPE, client, pluginSettings.getDelegate(), cryptoService, threadPool);

        setEnableUpdate(true); // with update

        synchroService.register(this);
    }

}

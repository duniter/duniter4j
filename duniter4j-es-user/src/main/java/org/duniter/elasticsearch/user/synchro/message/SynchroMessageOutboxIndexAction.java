package org.duniter.elasticsearch.user.synchro.message;

import org.duniter.core.service.CryptoService;
import org.duniter.elasticsearch.client.Duniter4jClient;
import org.duniter.elasticsearch.service.synchro.AbstractSynchroAction;
import org.duniter.elasticsearch.service.synchro.SynchroService;
import org.duniter.elasticsearch.threadpool.ThreadPool;
import org.duniter.elasticsearch.user.PluginSettings;
import org.duniter.elasticsearch.user.service.MessageService;
import org.elasticsearch.common.inject.Inject;

public class SynchroMessageOutboxIndexAction extends AbstractSynchroAction {

    @Inject
    public SynchroMessageOutboxIndexAction(Duniter4jClient client,
                                           PluginSettings pluginSettings,
                                           CryptoService cryptoService,
                                           ThreadPool threadPool,
                                           SynchroService synchroService) {
        super(MessageService.INDEX, MessageService.OUTBOX_TYPE, client, pluginSettings, cryptoService, threadPool);

        setEnableUpdate(false); // no update

        synchroService.register(this);
    }

}

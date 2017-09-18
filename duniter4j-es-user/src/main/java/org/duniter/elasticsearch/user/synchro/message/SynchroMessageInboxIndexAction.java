package org.duniter.elasticsearch.user.synchro.message;

import org.duniter.core.service.CryptoService;
import org.duniter.elasticsearch.client.Duniter4jClient;
import org.duniter.elasticsearch.service.synchro.AbstractSynchroAction;
import org.duniter.elasticsearch.service.synchro.SynchroService;
import org.duniter.elasticsearch.threadpool.ThreadPool;
import org.duniter.elasticsearch.user.PluginSettings;
import org.duniter.elasticsearch.user.service.MessageService;
import org.elasticsearch.common.inject.Inject;

public class SynchroMessageInboxIndexAction extends AbstractSynchroAction {

    @Inject
    public SynchroMessageInboxIndexAction(Duniter4jClient client,
                                          PluginSettings pluginSettings,
                                          CryptoService cryptoService,
                                          ThreadPool threadPool,
                                          SynchroService synchroService,
                                          MessageService service) {
        super(MessageService.INDEX, MessageService.INBOX_TYPE, client, pluginSettings, cryptoService, threadPool);

        addInsertListener(service::notifyUser);

        synchroService.register(this);
    }

}

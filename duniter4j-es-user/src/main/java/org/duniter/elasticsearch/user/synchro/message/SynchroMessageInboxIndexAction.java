package org.duniter.elasticsearch.user.synchro.message;

import com.fasterxml.jackson.databind.JsonNode;
import org.duniter.core.service.CryptoService;
import org.duniter.elasticsearch.client.Duniter4jClient;
import org.duniter.elasticsearch.synchro.SynchroActionResult;
import org.duniter.elasticsearch.synchro.SynchroService;
import org.duniter.elasticsearch.threadpool.ThreadPool;
import org.duniter.elasticsearch.user.PluginSettings;
import org.duniter.elasticsearch.user.service.MessageService;
import org.duniter.elasticsearch.synchro.AbstractSynchroAction;
import org.elasticsearch.common.inject.Inject;

public class SynchroMessageInboxIndexAction extends AbstractSynchroAction {

    private MessageService service;
    @Inject
    public SynchroMessageInboxIndexAction(Duniter4jClient client,
                                          PluginSettings pluginSettings,
                                          CryptoService cryptoService,
                                          ThreadPool threadPool,
                                          SynchroService synchroService,
                                          MessageService service) {
        super(MessageService.INDEX, MessageService.INBOX_TYPE, client, pluginSettings.getDelegate(), cryptoService, threadPool);

        this.service = service;

        addInsertionListener(this::onInsert);

        synchroService.register(this);
    }

    protected void onInsert(String id, JsonNode source, SynchroActionResult result) {
        service.notifyUser(id, source);
    }
}

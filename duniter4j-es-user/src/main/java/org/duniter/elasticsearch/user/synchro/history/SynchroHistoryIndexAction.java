package org.duniter.elasticsearch.user.synchro.history;

import com.fasterxml.jackson.databind.JsonNode;
import org.duniter.core.service.CryptoService;
import org.duniter.elasticsearch.client.Duniter4jClient;
import org.duniter.elasticsearch.exception.NotFoundException;
import org.duniter.elasticsearch.service.synchro.AbstractSynchroAction;
import org.duniter.elasticsearch.service.synchro.SynchroService;
import org.duniter.elasticsearch.threadpool.ThreadPool;
import org.duniter.elasticsearch.user.PluginSettings;
import org.duniter.elasticsearch.user.service.HistoryService;
import org.elasticsearch.common.inject.Inject;

public class SynchroHistoryIndexAction extends AbstractSynchroAction {

    private HistoryService service;
    @Inject
    public SynchroHistoryIndexAction(final Duniter4jClient client,
                                     PluginSettings pluginSettings,
                                     CryptoService cryptoService,
                                     ThreadPool threadPool,
                                     SynchroService synchroService,
                                     HistoryService service) {
        super(service.INDEX, service.DELETE_TYPE, client, pluginSettings, cryptoService, threadPool);
        this.service = service;

        addInsertListener(this::onInsertDeletion);

        synchroService.register(this);
    }

    protected void onInsertDeletion(String historyId, JsonNode source) {
        try {
            // Check if valid document
            service.checkIsValidDeletion(source);

            // Delete the document
            service.applyDocDelete(source);

        } catch(NotFoundException e) {
            // doc not exists: continue
            logger.debug("Doc to delete could not be found. Skipping deletion");
        }
    }
}

package org.duniter.elasticsearch.user.synchro.user;

import org.duniter.core.service.CryptoService;
import org.duniter.elasticsearch.client.Duniter4jClient;
import org.duniter.elasticsearch.synchro.SynchroService;
import org.duniter.elasticsearch.threadpool.ThreadPool;
import org.duniter.elasticsearch.user.PluginSettings;
import org.duniter.elasticsearch.user.service.UserService;
import org.duniter.elasticsearch.synchro.AbstractSynchroAction;
import org.elasticsearch.common.inject.Inject;

public class SynchroUserProfileAction extends AbstractSynchroAction {

    @Inject
    public SynchroUserProfileAction(Duniter4jClient client,
                                    PluginSettings pluginSettings,
                                    CryptoService cryptoService,
                                    ThreadPool threadPool,
                                    SynchroService synchroService) {
        super(UserService.INDEX, UserService.PROFILE_TYPE, client, pluginSettings.getDelegate(), cryptoService, threadPool);

        setEnableUpdate(true); // with update

        synchroService.register(this);
    }

}

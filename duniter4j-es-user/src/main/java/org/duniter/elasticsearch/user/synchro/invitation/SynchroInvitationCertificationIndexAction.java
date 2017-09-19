package org.duniter.elasticsearch.user.synchro.invitation;

import org.duniter.core.service.CryptoService;
import org.duniter.elasticsearch.client.Duniter4jClient;
import org.duniter.elasticsearch.synchro.SynchroService;
import org.duniter.elasticsearch.threadpool.ThreadPool;
import org.duniter.elasticsearch.user.PluginSettings;
import org.duniter.elasticsearch.user.service.UserInvitationService;
import org.duniter.elasticsearch.synchro.AbstractSynchroAction;
import org.elasticsearch.common.inject.Inject;

public class SynchroInvitationCertificationIndexAction extends AbstractSynchroAction {

    @Inject
    public SynchroInvitationCertificationIndexAction(Duniter4jClient client,
                                                     PluginSettings pluginSettings,
                                                     CryptoService cryptoService,
                                                     ThreadPool threadPool,
                                                     SynchroService synchroService,
                                                     UserInvitationService service) {
        super(UserInvitationService.INDEX, UserInvitationService.CERTIFICATION_TYPE, client,
                pluginSettings.getDelegate(), cryptoService, threadPool);

        addInsertionListener(service::notifyUser);

        synchroService.register(this);
    }


}

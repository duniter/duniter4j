package org.duniter.elasticsearch.user.synchro.user;

import com.fasterxml.jackson.databind.JsonNode;
import org.duniter.core.service.CryptoService;
import org.duniter.elasticsearch.client.Duniter4jClient;
import org.duniter.elasticsearch.exception.AccessDeniedException;
import org.duniter.elasticsearch.exception.NotFoundException;
import org.duniter.elasticsearch.synchro.SynchroActionResult;
import org.duniter.elasticsearch.synchro.SynchroService;
import org.duniter.elasticsearch.threadpool.ThreadPool;
import org.duniter.elasticsearch.user.PluginSettings;
import org.duniter.elasticsearch.user.dao.profile.UserSettingsDao;
import org.duniter.elasticsearch.user.service.UserService;
import org.duniter.elasticsearch.synchro.AbstractSynchroAction;
import org.elasticsearch.common.inject.Inject;

import java.util.Objects;

public class SynchroUserSettingsAction extends AbstractSynchroAction {

    @Inject
    public SynchroUserSettingsAction(Duniter4jClient client,
                                     PluginSettings pluginSettings,
                                     CryptoService cryptoService,
                                     ThreadPool threadPool,
                                     SynchroService synchroService) {
        super(UserService.INDEX, UserService.SETTINGS_TYPE, client, pluginSettings.getDelegate(), cryptoService, threadPool);

        setEnableUpdate(true); // with update

        addValidationListener(this::onValidate);

        synchroService.register(this);
    }

    protected void onValidate(String id, JsonNode source, SynchroActionResult result) {

        String issuer = getIssuer(source);
        if (!Objects.equals(issuer, id)) {
            throw new AccessDeniedException(String.format("Could not save this document: id must be equals to issuer."));
        }
    }
}

package org.duniter.elasticsearch.user.service;

/*
 * #%L
 * Duniter4j :: ElasticSearch Plugin
 * %%
 * Copyright (C) 2014 - 2016 EIS
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import org.duniter.core.client.model.elasticsearch.Protocol;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.service.CryptoService;
import org.duniter.elasticsearch.client.Duniter4jClient;
import org.duniter.elasticsearch.user.PluginSettings;
import org.duniter.elasticsearch.model.SynchroResult;
import org.duniter.elasticsearch.service.ServiceLocator;
import org.duniter.elasticsearch.service.AbstractSynchroService;
import org.duniter.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;

/**
 * Created by blavenie on 27/10/16.
 */
public class SynchroService extends AbstractSynchroService {

   @Inject
    public SynchroService(Duniter4jClient client, PluginSettings settings, CryptoService cryptoService,
                          ThreadPool threadPool, final ServiceLocator serviceLocator) {
        super(client, settings.getDelegate(), cryptoService, threadPool, serviceLocator);
    }

    public void synchronize() {
        logger.info("Synchronizing user data...");

        Peer peer = getPeerFromAPI(Protocol.ES_API);
        synchronize(peer);
    }

    /* -- protected methods -- */


    protected void synchronize(Peer peer) {

        long sinceTime = 0; // ToDO: get last sync time from somewhere ? (e.g. a specific index)

        logger.info(String.format("[%s] Synchronizing user data since %s...", peer.toString(), sinceTime));

        SynchroResult result = new SynchroResult();
        long time = System.currentTimeMillis();

        importHistoryChanges(result, peer, sinceTime);
        importUserChanges(result, peer, sinceTime);
        importMessageChanges(result, peer, sinceTime);
        importGroupChanges(result, peer, sinceTime);
        importInvitationChanges(result, peer, sinceTime);

        long duration = System.currentTimeMillis() - time;
        logger.info(String.format("[%s] Synchronizing user data since %s [OK] %s (ins %s ms)", peer.toString(), sinceTime, result.toString(), duration));
    }

    protected void importHistoryChanges(SynchroResult result, Peer peer, long sinceTime) {
        importChanges(result, peer, HistoryService.INDEX, HistoryService.DELETE_TYPE,  sinceTime);
    }

    protected void importUserChanges(SynchroResult result, Peer peer, long sinceTime) {
        importChanges(result, peer, UserService.INDEX, UserService.PROFILE_TYPE,  sinceTime);
        importChanges(result, peer, UserService.INDEX, UserService.SETTINGS_TYPE,  sinceTime);
    }

    protected void importMessageChanges(SynchroResult result, Peer peer, long sinceTime) {
        // For compat
        // TODO: remove this later
        importChangesRemap(result, peer, MessageService.INDEX, MessageService.RECORD_TYPE,
                MessageService.INDEX, MessageService.INBOX_TYPE,
                sinceTime);

        importChanges(result, peer, MessageService.INDEX, MessageService.INBOX_TYPE,  sinceTime);
        importChanges(result, peer, MessageService.INDEX, MessageService.OUTBOX_TYPE,  sinceTime);
    }

    protected void importGroupChanges(SynchroResult result, Peer peer, long sinceTime) {
        importChanges(result, peer, GroupService.INDEX, GroupService.RECORD_TYPE,  sinceTime);
    }

    protected void importInvitationChanges(SynchroResult result, Peer peer, long sinceTime) {
        importChanges(result, peer, UserInvitationService.INDEX, UserInvitationService.CERTIFICATION_TYPE,  sinceTime);
    }

}

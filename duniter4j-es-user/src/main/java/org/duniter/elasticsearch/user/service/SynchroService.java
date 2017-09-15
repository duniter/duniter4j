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

import org.duniter.core.client.model.bma.EndpointApi;
import org.duniter.core.client.model.elasticsearch.Protocol;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.service.CryptoService;
import org.duniter.elasticsearch.client.Duniter4jClient;
import org.duniter.elasticsearch.model.SynchroResult;
import org.duniter.elasticsearch.service.AbstractSynchroService;
import org.duniter.elasticsearch.service.ServiceLocator;
import org.duniter.elasticsearch.threadpool.ThreadPool;
import org.duniter.elasticsearch.user.PluginSettings;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

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
        logger.info("Starting user data synchronization...");

        Peer peer = getPeerFromAPI(EndpointApi.ES_USER_API);
        synchronize(peer);

        logger.info("User data synchronization [OK]");
    }

    /* -- protected methods -- */


    protected void synchronize(Peer peer) {
        long now = System.currentTimeMillis();
        SynchroResult result = new SynchroResult();

        long fromTime = 0; // TODO: get last sync time from somewhere ? (e.g. a specific index)
        synchronize(peer, fromTime, result);

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("[%s] User data imported in %s ms: %s", peer, System.currentTimeMillis() - now, result.toString()));
        }

    }

    protected void synchronize(Peer peer, long fromTime, SynchroResult result) {
        synchronizeHistory(peer, fromTime, result);
        synchronizeUser(peer, fromTime, result);
        synchronizeMessage(peer, fromTime, result);
        synchronizeGroup(peer, fromTime, result);
        synchronizeInvitation(peer, fromTime, result);
    }

    protected void synchronizeHistory(Peer peer, long fromTime, SynchroResult result) {
        safeSynchronizeIndex(peer, HistoryService.INDEX, HistoryService.DELETE_TYPE, fromTime, result);
    }

    protected void synchronizeUser(Peer peer, long fromTime, SynchroResult result) {
        safeSynchronizeIndex(peer, UserService.INDEX, UserService.PROFILE_TYPE,  fromTime, result);
        safeSynchronizeIndex(peer, UserService.INDEX, UserService.SETTINGS_TYPE,  fromTime, result);
    }

    protected void synchronizeMessage(Peer peer, long fromTime, SynchroResult result) {
        safeSynchronizeIndex(peer, MessageService.INDEX, MessageService.INBOX_TYPE, fromTime, result);
        safeSynchronizeIndex(peer, MessageService.INDEX, MessageService.OUTBOX_TYPE, fromTime, result);

        // User events, that reference message index
        synchronizeUserEventsOnReferenceIndex(peer, MessageService.INDEX, fromTime, result);
    }

    protected void synchronizeGroup(Peer peer, long fromTime, SynchroResult result) {
        safeSynchronizeIndex(peer, GroupService.INDEX, GroupService.RECORD_TYPE, fromTime, result);

        // User events, that reference invitation index
        synchronizeUserEventsOnReferenceIndex(peer, GroupService.INDEX, fromTime, result);
    }

    protected void synchronizeInvitation(Peer peer, long fromTime, SynchroResult result) {
        safeSynchronizeIndex(peer, UserInvitationService.INDEX, UserInvitationService.CERTIFICATION_TYPE, fromTime, result);

        // User events, that reference invitation index
        synchronizeUserEventsOnReferenceIndex(peer, UserInvitationService.INDEX, fromTime, result);
    }


    protected void synchronizeUserEventsOnReferenceIndex(Peer peer, String referenceIndex, long fromTime, SynchroResult result) {

        /*QueryBuilder query = QueryBuilders.boolQuery()
                .filter(QueryBuilders.rangeQuery("time").gte(fromTime));
        safeSynchronizeIndex(peer, UserService.INDEX, UserEventService.EVENT_TYPE, query, result);*/
    }

}

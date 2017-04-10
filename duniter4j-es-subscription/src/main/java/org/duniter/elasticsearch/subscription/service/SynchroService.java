package org.duniter.elasticsearch.subscription.service;

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

import org.duniter.core.client.model.local.Peer;
import org.duniter.core.service.CryptoService;
import org.duniter.elasticsearch.client.Duniter4jClient;
import org.duniter.elasticsearch.model.SynchroResult;
import org.duniter.elasticsearch.service.AbstractSynchroService;
import org.duniter.elasticsearch.service.ServiceLocator;
import org.duniter.elasticsearch.subscription.dao.SubscriptionIndexDao;
import org.duniter.elasticsearch.subscription.dao.record.SubscriptionRecordDao;
import org.duniter.elasticsearch.subscription.model.Protocol;
import org.duniter.elasticsearch.threadpool.ThreadPool;
import org.duniter.elasticsearch.user.PluginSettings;
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
        logger.info("Synchronizing subscription data...");
        Peer peer = getPeerFromAPI(Protocol.EMAIL_API);
        synchronize(peer);
    }

    /* -- protected methods -- */

    protected void synchronize(Peer peer) {

        long sinceTime = 0; // TODO: get last sync time from somewhere ? (e.g. a specific index)

        logger.info(String.format("[%s] Synchronizing subscription data since %s...", peer.toString(), sinceTime));

        SynchroResult result = new SynchroResult();
        long time = System.currentTimeMillis();

        importMailChanges(result, peer, sinceTime);

        long duration = System.currentTimeMillis() - time;
        logger.info(String.format("[%s] Synchronizing subscription data since %s [OK] %s (in %s ms)", peer.toString(), sinceTime, result.toString(), duration));
    }

    protected void importMailChanges(SynchroResult result, Peer peer, long sinceTime) {
        importChanges(result, peer, SubscriptionIndexDao.INDEX, SubscriptionRecordDao.TYPE,  sinceTime);
    }
}

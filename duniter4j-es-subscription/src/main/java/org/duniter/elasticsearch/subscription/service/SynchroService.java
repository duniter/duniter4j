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

import org.duniter.core.client.dao.CurrencyDao;
import org.duniter.core.client.dao.PeerDao;
import org.duniter.core.client.model.bma.EndpointApi;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.service.CryptoService;
import org.duniter.core.util.CollectionUtils;
import org.duniter.elasticsearch.client.Duniter4jClient;
import org.duniter.elasticsearch.model.SynchroResult;
import org.duniter.elasticsearch.service.AbstractSynchroService;
import org.duniter.elasticsearch.service.PeerService;
import org.duniter.elasticsearch.service.ServiceLocator;
import org.duniter.elasticsearch.subscription.dao.SubscriptionIndexDao;
import org.duniter.elasticsearch.subscription.dao.execution.SubscriptionExecutionDao;
import org.duniter.elasticsearch.subscription.dao.record.SubscriptionRecordDao;
import org.duniter.elasticsearch.subscription.model.Protocol;
import org.duniter.elasticsearch.threadpool.ThreadPool;
import org.duniter.elasticsearch.user.PluginSettings;
import org.elasticsearch.common.inject.Inject;

import java.util.List;

/**
 * Created by blavenie on 27/10/16.
 */
public class SynchroService extends AbstractSynchroService {

    private static final EndpointApi ENDPOINT_API = EndpointApi.ES_SUBSCRIPTION_API;

    @Inject
    public SynchroService(Duniter4jClient client, PluginSettings settings, CryptoService cryptoService,
                          ThreadPool threadPool,
                          PeerService peerService,
                          CurrencyDao currencyDao,
                          PeerDao peerDao,
                          final ServiceLocator serviceLocator) {
        super(client, settings.getDelegate(), cryptoService, threadPool, currencyDao, peerDao, serviceLocator);

        // Configure peer service to allow API
        peerService.addIncludeEndpointApi(ENDPOINT_API.name());
    }

    @Override
    protected void synchronize() {
        logger.info("Starting subscription data synchronization...");

        // Get peers
        List<Peer> peers = getPeersFromApi(ENDPOINT_API);
        if (CollectionUtils.isNotEmpty(peers)) {
            peers.forEach(this::synchronize);
            logger.info("User subscription synchronization [OK]");
        }
        else {
            logger.info(String.format("User subscription synchronization [OK] - no endpoint found for API [%s]", ENDPOINT_API.name()));
        }
    }

    /* -- protected methods -- */

    protected void synchronize(Peer peer) {
        SynchroResult result = new SynchroResult();
        long now = System.currentTimeMillis();

        long fromTime = 0; // TODO: get last sync time from somewhere ? (e.g. a specific index)
        synchronizeSubscriptions(peer, fromTime, result);

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("[%s] Subscription data imported in %s ms: %s", peer, System.currentTimeMillis() - now, result.toString()));
        }
    }

    protected void synchronizeSubscriptions(Peer peer, long fromTime, SynchroResult result) {
        // Workaround to skip data older than june 2017
        long executionFromTime = Math.max(fromTime, 1493743177);
        safeSynchronizeIndex(peer, SubscriptionIndexDao.INDEX, SubscriptionExecutionDao.TYPE, executionFromTime, result);
        
        safeSynchronizeIndex(peer, SubscriptionIndexDao.INDEX, SubscriptionRecordDao.TYPE,  fromTime, result);
    }
}

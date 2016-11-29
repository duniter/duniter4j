package org.duniter.elasticsearch.node;

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

import org.duniter.core.client.model.elasticsearch.Currency;
import org.duniter.core.client.model.local.Peer;
import org.duniter.elasticsearch.PluginSettings;
import org.duniter.elasticsearch.action.security.RestSecurityController;
import org.duniter.elasticsearch.service.*;
import org.duniter.elasticsearch.service.event.Event;
import org.duniter.elasticsearch.service.event.EventCodes;
import org.duniter.elasticsearch.service.event.EventService;
import org.duniter.elasticsearch.service.synchro.SynchroService;
import org.duniter.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.RestRequest;

/**
 * Created by blavenie on 17/06/16.
 */
public class DuniterNode extends AbstractLifecycleComponent<DuniterNode> {

    private final PluginSettings pluginSettings;
    private final ThreadPool threadPool;
    private final Injector injector;
    private final static ESLogger logger = Loggers.getLogger("node");
    private final Client client;
    private final String clusterName;

    @Inject
    public DuniterNode(Client client, Settings settings, PluginSettings pluginSettings, ThreadPool threadPool, final Injector injector) {
        super(settings);
        this.pluginSettings = pluginSettings;
        this.threadPool = threadPool;
        this.injector = injector;
        this.client = client;
        this.clusterName = settings.get("cluster.name");
    }

    @Override
    protected void doStart() {
        threadPool.scheduleOnClusterHealthStatus(() -> {
            createIndices();

            // Waiting cluster back to GREEN or YELLOW state, before synchronize
            threadPool.scheduleOnClusterHealthStatus(() -> {
                synchronize();
            }, ClusterHealthStatus.YELLOW, ClusterHealthStatus.GREEN);
        }, ClusterHealthStatus.YELLOW, ClusterHealthStatus.GREEN);

        // When started
        threadPool.scheduleOnStarted(() -> {
            // Notify admin
            injector.getInstance(EventService.class)
                    .notifyAdmin(new Event(
                            Event.EventType.INFO,
                            EventCodes.NODE_STARTED.name(),
                            new String[]{clusterName}));
        });
    }

    @Override
    protected void doStop() {

    }

    @Override
    protected void doClose() {

    }

    protected void createIndices() {

        boolean reloadIndices = pluginSettings.reloadIndices();

        if (reloadIndices) {
            if (logger.isInfoEnabled()) {
                logger.info("Reloading all Duniter indices...");
            }
            injector.getInstance(RegistryService.class)
                    .deleteIndex()
                    .createIndexIfNotExists();
            injector.getInstance(MarketService.class)
                    .deleteIndex()
                    .createIndexIfNotExists();
            injector.getInstance(MessageService.class)
                    .deleteIndex()
                    .createIndexIfNotExists();

            injector.getInstance(UserService.class)
                    .deleteIndex()
                    .createIndexIfNotExists();

            injector.getInstance(HistoryService.class)
                    .deleteIndex()
                    .createIndexIfNotExists();

            injector.getInstance(EventService.class)
                    .deleteIndex()
                    .createIndexIfNotExists();


            if (logger.isInfoEnabled()) {
                logger.info("Reloading all Duniter indices... [OK]");
            }
        }
        else {
            if (logger.isInfoEnabled()) {
                logger.info("Checking Duniter indices...");
            }
            injector.getInstance(RegistryService.class).createIndexIfNotExists();
            injector.getInstance(MarketService.class).createIndexIfNotExists();
            injector.getInstance(MessageService.class).createIndexIfNotExists();
            injector.getInstance(UserService.class).createIndexIfNotExists();
            injector.getInstance(HistoryService.class).createIndexIfNotExists();
            injector.getInstance(EventService.class).createIndexIfNotExists();

            if (logger.isInfoEnabled()) {
                logger.info("Checking Duniter indices... [OK]");
            }
        }
    }

    protected void synchronize() {
        if (pluginSettings.enableBlockchainSync()) {

            Peer peer = pluginSettings.checkAndGetPeer();

            // Index (or refresh) node's currency
            Currency currency = injector.getInstance(RegistryService.class).indexCurrencyFromPeer(peer, true);

            // Add access to currency index
            injector.getInstance(RestSecurityController.class).allowIndexType(RestRequest.Method.GET,
                    currency.getCurrencyName(),
                    BlockchainService.BLOCK_TYPE);

            // Index blocks (and listen if new block appear)
            injector.getInstance(BlockchainService.class)
                    .indexLastBlocks(peer)
                    .listenAndIndexNewBlock(peer);
        }

        if (pluginSettings.enableDataSync()) {
            // Synchronize
            injector.getInstance(SynchroService.class).synchronize();
        }
    }
}

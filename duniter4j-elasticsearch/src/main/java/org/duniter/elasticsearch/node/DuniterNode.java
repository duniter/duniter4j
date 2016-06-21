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

import org.duniter.core.client.model.local.Peer;
import org.duniter.elasticsearch.PluginSettings;
import org.duniter.elasticsearch.service.BlockchainService;
import org.duniter.elasticsearch.service.MarketService;
import org.duniter.elasticsearch.service.RegistryService;
import org.duniter.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.settings.Settings;

/**
 * Created by blavenie on 17/06/16.
 */
public class DuniterNode extends AbstractLifecycleComponent<DuniterNode> {

    private final PluginSettings pluginSettings;
    private final ThreadPool threadPool;
    private final Injector injector;

    @Inject
    public DuniterNode(Settings settings, PluginSettings pluginSettings, ThreadPool threadPool, final Injector injector) {
        super(settings);
        this.pluginSettings = pluginSettings;
        this.threadPool = threadPool;
        this.injector = injector;

    }

    @Override
    protected void doStart() {
        threadPool.scheduleOnStarted(() -> {
            createIndices();
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
        Peer peer = pluginSettings.checkAndGetPeer();
        if (reloadIndices) {
            if (logger.isInfoEnabled()) {
                logger.info("Reloading all Duniter indices...");
            }
            injector.getInstance(RegistryService.class)
                    .deleteIndex()
                    .createIndexIfNotExists()
                    .fillRecordCategories()
                    .indexCurrencyFromPeer(peer);
            injector.getInstance(MarketService.class)
                    .deleteIndex()
                    .createIndexIfNotExists()
                    .fillRecordCategories();

            injector.getInstance(BlockchainService.class)
                    .indexLastBlocks(peer);

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

            if (logger.isInfoEnabled()) {
                logger.info("Checking Duniter indices... [OK]");
            }
        }

    }
}

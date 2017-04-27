package org.duniter.elasticsearch.service;

/*
 * #%L
 * UCoin Java Client :: Core API
 * %%
 * Copyright (C) 2014 - 2015 EIS
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


import com.google.common.collect.ImmutableList;
import org.duniter.core.client.model.bma.BlockchainBlock;
import org.duniter.core.client.model.bma.util.BlockchainBlockUtils;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.service.CryptoService;
import org.duniter.core.util.CollectionUtils;
import org.duniter.elasticsearch.PluginSettings;
import org.duniter.elasticsearch.client.Duniter4jClient;
import org.duniter.elasticsearch.dao.BlockStatDao;
import org.duniter.elasticsearch.model.BlockchainBlockStat;
import org.duniter.elasticsearch.service.changes.ChangeEvent;
import org.duniter.elasticsearch.service.changes.ChangeService;
import org.duniter.elasticsearch.service.changes.ChangeSource;
import org.duniter.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.metrics.CounterMetric;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Created by Benoit on 26/04/2017.
 */
public abstract class AbstractBlockchainListenerService extends AbstractService implements ChangeService.ChangeListener {

    private static final List<ChangeSource> CHANGE_LISTEN_SOURCES = ImmutableList.of(new ChangeSource("*", BlockchainService.BLOCK_TYPE));

    protected final boolean enable;
    protected final String listenerId;
    protected final ThreadPool threadPool;

    @Inject
    public AbstractBlockchainListenerService(String loggerName,
                                             Duniter4jClient client,
                                             PluginSettings settings,
                                             CryptoService cryptoService,
                                             ThreadPool threadPool) {
        super(loggerName, client, settings, cryptoService);
        this.listenerId = loggerName;
        this.enable = pluginSettings.enableBlockchain();
        this.threadPool = threadPool;

        if (this.enable) {
            ChangeService.registerListener(this);
        }
    }


    @Override
    public String getId() {
        return listenerId;
    }

    @Override
    public final void onChange(ChangeEvent change) {

        // Skip _id=current
        if("current".equals(change.getId())) return;

        switch (change.getOperation()) {
            // on create
            case CREATE: // create
                if (change.getSource() != null) {
                    CompletableFuture.runAsync(() -> {
                        processCreateBlock(change);
                    }, threadPool.scheduler());
                }
                break;

            // on update
            case INDEX:
                if (change.getSource() != null) {
                    // Delete existing stat
                    CompletableFuture.runAsync(() ->  processBlockDelete(change, true), threadPool.scheduler())
                        // Then process block
                        .thenAcceptAsync(aVoid -> processCreateBlock(change));
                }
                break;

            // on DELETE : remove user event on block (using link
            case DELETE:
                // Delete existing stat
                CompletableFuture.runAsync(() ->  processBlockDelete(change, false));
                break;
        }

    }

    @Override
    public Collection<ChangeSource> getChangeSources() {
        return CHANGE_LISTEN_SOURCES;
    }

    /* -- internal method -- */

    protected abstract void processCreateBlock(final ChangeEvent change);

    protected abstract void processBlockDelete(ChangeEvent change, boolean wait);


}

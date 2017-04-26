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
import org.duniter.core.util.Preconditions;
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
public class BlockchainStatsService extends AbstractService implements ChangeService.ChangeListener {

    private static final List<ChangeSource> CHANGE_LISTEN_SOURCES = ImmutableList.of(new ChangeSource("*", BlockchainService.BLOCK_TYPE));

    private final boolean enable;
    private final BlockStatDao blockStatDao;
    private final ThreadPool threadPool;

    @Inject
    public BlockchainStatsService(Duniter4jClient client, PluginSettings settings, CryptoService cryptoService,
                                  BlockStatDao blockStatDao,
                                  ThreadPool threadPool) {
        super("duniter.blockchain.stats", client, settings, cryptoService);
        this.enable = pluginSettings.enableBlockchainSync();
        this.blockStatDao = blockStatDao;
        this.threadPool = threadPool;

        if (this.enable) {
            ChangeService.registerListener(this);
        }
    }

    @Override
    public String getId() {
        return "duniter.blockchain.stats";
    }

    @Override
    public void onChange(ChangeEvent change) {

        // Skip _id=current
        if(change.getId() == "current") return;

        try {

            switch (change.getOperation()) {
                // on create
                case CREATE: // create
                    if (change.getSource() != null) {
                        BlockchainBlock block = objectMapper.readValue(change.getSource().streamInput(), BlockchainBlock.class);
                        processCreateBlock(block);
                    }
                    break;

                // on update
                case INDEX:
                    if (change.getSource() != null) {
                        BlockchainBlock block = objectMapper.readValue(change.getSource().streamInput(), BlockchainBlock.class);
                        processUpdateBlock(block);
                    }
                    break;

                // on DELETE : remove user event on block (using link
                case DELETE:
                    processBlockDelete(change);

                    break;
            }

        }
        catch(IOException e) {
            throw new TechnicalException(String.format("Unable to parse received block %s", change.getId()), e);
        }

    }

    @Override
    public Collection<ChangeSource> getChangeSources() {
        return CHANGE_LISTEN_SOURCES;
    }

    /* -- internal method -- */

    private void processCreateBlock(BlockchainBlock block) {

        BlockchainBlockStat stat = newBlockStat(block);

        // Tx
        if (CollectionUtils.isNotEmpty(block.getTransactions())) {
            CounterMetric txChangeCounter = new CounterMetric();
            CounterMetric txAmountCounter = new CounterMetric();
            Arrays.stream(block.getTransactions())
                .forEach(tx -> {
                    long txAmount = BlockchainBlockUtils.getTxAmount(tx);
                    if (txAmount == 0l) {
                        txChangeCounter.inc();
                    }
                    else {
                        txAmountCounter.inc(txAmount);
                    }
                });

            stat.setTxAmount(BigInteger.valueOf(txAmountCounter.count()));
            stat.setTxChangeCount((int)txChangeCounter.count());
            stat.setTxCount(block.getTransactions().length);
        }
        else {
            stat.setTxAmount(BigInteger.valueOf(0));
            stat.setTxChangeCount(0);
            stat.setTxCount(0);
        }

        // Add to index
        blockStatDao.create(stat, false/*wait*/);
    }

    private void processUpdateBlock(final BlockchainBlock block) {
        Preconditions.checkNotNull(block);
        Preconditions.checkNotNull(block.getNumber());

        // Delete existing stat
        CompletableFuture.runAsync(() -> blockStatDao.delete(block.getCurrency(), block.getNumber().toString(), true /*wait*/), threadPool.scheduler())
                // Then process block
                .thenAccept(aVoid -> processCreateBlock(block));
    }

    private void processBlockDelete(ChangeEvent change) {
        if (change.getId() == null) return;

        // Delete existing stat
        blockStatDao.delete(change.getIndex(), change.getId(), false /*wait*/);
    }

    protected BlockchainBlockStat newBlockStat(BlockchainBlock block) {
        BlockchainBlockStat stat = new BlockchainBlockStat();

        stat.setNumber(block.getNumber());
        stat.setHash(block.getHash());
        stat.setCurrency(block.getCurrency());
        stat.setMedianTime(block.getMedianTime());
        stat.setMembersCount(block.getMembersCount());
        stat.setMonetaryMass(block.getMonetaryMass());
        stat.setUnitbase(block.getUnitbase());
        stat.setVersion(block.getVersion());
        stat.setDividend(block.getDividend());

        return stat;
    }


}

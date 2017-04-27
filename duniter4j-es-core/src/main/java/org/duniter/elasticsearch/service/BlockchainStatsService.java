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
import org.duniter.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.metrics.CounterMetric;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

/**
 * Created by Benoit on 26/04/2017.
 */
public class BlockchainStatsService extends AbstractBlockchainListenerService {

    private final BlockStatDao blockStatDao;

    @Inject
    public BlockchainStatsService(Duniter4jClient client, PluginSettings settings, CryptoService cryptoService,
                                  BlockStatDao blockStatDao,
                                  ThreadPool threadPool) {
        super("duniter.blockchain.stats", client, settings, cryptoService, threadPool);
        this.blockStatDao = blockStatDao;
    }

    protected void processCreateBlock(final ChangeEvent change) {
        try {
            BlockchainBlock block = objectMapper.readValue(change.getSource().streamInput(), BlockchainBlock.class);
            processCreateBlock(block);
        } catch (IOException e) {
            throw new TechnicalException(String.format("Unable to parse received block %s", change.getId()), e);
        }
    }

    protected void processBlockDelete(ChangeEvent change, boolean wait) {
        if (change.getId() == null) return;

        // Delete existing stat
        blockStatDao.delete(change.getIndex(), change.getId(), wait);
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

    private BlockchainBlockStat newBlockStat(BlockchainBlock block) {
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

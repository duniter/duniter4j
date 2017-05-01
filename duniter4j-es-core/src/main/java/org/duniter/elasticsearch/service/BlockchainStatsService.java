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


import com.fasterxml.jackson.core.JsonProcessingException;
import org.duniter.core.client.model.bma.BlockchainBlock;
import org.duniter.core.client.model.bma.util.BlockchainBlockUtils;
import org.duniter.core.service.CryptoService;
import org.duniter.core.util.CollectionUtils;
import org.duniter.elasticsearch.PluginSettings;
import org.duniter.elasticsearch.client.Duniter4jClient;
import org.duniter.elasticsearch.dao.BlockStatDao;
import org.duniter.elasticsearch.model.BlockchainBlockStat;
import org.duniter.elasticsearch.service.changes.ChangeEvent;
import org.duniter.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.metrics.CounterMetric;
import org.elasticsearch.common.unit.TimeValue;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Created by Benoit on 26/04/2017.
 */
public class BlockchainStatsService extends AbstractBlockchainListenerService {

    @Inject
    public BlockchainStatsService(Duniter4jClient client, PluginSettings settings, CryptoService cryptoService,
                                  ThreadPool threadPool) {
        super("duniter.blockchain.stats", client, settings, cryptoService, threadPool,
                new TimeValue(500, TimeUnit.MILLISECONDS));
    }

    @Override
    protected void processBlockIndex(ChangeEvent change) {

        BlockchainBlock block = readBlock(change);
        BlockchainBlockStat stat = toBlockStat(block);

        // Add a delete to bulk
        bulkRequest.add(client.prepareDelete(block.getCurrency(), BlockStatDao.TYPE, String.valueOf(block.getNumber()))
                .setRefresh(false));
        flushBulkRequestOrSchedule();

        // Add a insert to bulk
        try {
            bulkRequest.add(client.prepareIndex(block.getCurrency(), BlockStatDao.TYPE, String.valueOf(block.getNumber()))
                    .setRefresh(false) // recommended for heavy indexing
                    .setSource(objectMapper.writeValueAsString(stat)));
            flushBulkRequestOrSchedule();
        }
        catch(JsonProcessingException e) {
            logger.error("Could not serialize BlockchainBlockStat into JSON: " + e.getMessage(), e);
        }
    }

    protected void processBlockDelete(ChangeEvent change) {
        // Add delete to bulk
        bulkRequest.add(client.prepareDelete(change.getIndex(), BlockStatDao.TYPE, change.getId())
                .setRefresh(false));
        flushBulkRequestOrSchedule();
    }

    protected void beforeFlush() {
        // Nothing to do
    }

    protected BlockchainBlockStat toBlockStat(BlockchainBlock block) {

        BlockchainBlockStat result = newBlockStat(block);

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
            result.setTxAmount(BigInteger.valueOf(txAmountCounter.count()));
            result.setTxChangeCount((int)txChangeCounter.count());
            result.setTxCount(block.getTransactions().length);
        }
        else {
            result.setTxAmount(BigInteger.valueOf(0));
            result.setTxChangeCount(0);
            result.setTxCount(0);
        }

        return result;
    }

    /* -- internal method -- */

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

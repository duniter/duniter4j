package org.duniter.core.client.model;

import org.duniter.core.client.model.bma.BlockchainBlock;
import org.duniter.core.client.model.bma.BlockchainBlocks;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by benoit on 27/11/17.
 */
public class BlockchainBlocksTest {

    @Test
    public void getTxAmount_issue30() {

        // This test should detect when TX.inputs format is invalid - see #19
        BlockchainBlock block = BlockFileUtils.readBlockFile("block_issue_30.json");
        long txAmount = BlockchainBlocks.getTxAmount(block.getTransactions()[0]);
        Assert.assertEquals(72000l, txAmount);

    }
}

package org.duniter.core.client.model;

import org.duniter.core.client.model.bma.BlockchainBlock;
import org.duniter.core.client.model.exception.InvalidFormatException;
import org.duniter.core.client.model.local.Movement;
import org.duniter.core.client.model.local.Movements;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by blavenie on 23/05/17.
 */
public class MovementsTest {


    private static final Logger log = LoggerFactory.getLogger(MovementsTest.class);

    @Test
    public void getMovements() throws Exception {

        final BlockchainBlock block = BlockFileUtils.readBlockFile("block_with_tx.json");
        List<Movement> mov = Movements.getMovements(block);
        Assert.assertTrue(mov.size() > 0);
    }



    @Test
    public void getMovements_issue19() {

        // This test should detect when TX.inputs format is invalid - see #19
        BlockchainBlock block = BlockFileUtils.readBlockFile("block_issue_19.json");
        try {
            List<Movement> mov = Movements.getMovements(block);
            Assert.fail("no exception");
        } catch(InvalidFormatException e) {
            // OK
            log.error(e.getMessage());
        }

    }
}

package org.duniter.core.client.model;

/*-
 * #%L
 * Duniter4j :: Core Client API
 * %%
 * Copyright (C) 2014 - 2017 EIS
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

    @Test
    public void getMovements_issue30() {

        // This test should detect when TX.inputs format is invalid - see #19
        BlockchainBlock block = BlockFileUtils.readBlockFile("block_issue_30.json");
        List<Movement> mov = Movements.getMovements(block);
        Assert.assertEquals(1, mov.size());
        Assert.assertEquals(72000l, mov.get(0).getAmount());

    }
}

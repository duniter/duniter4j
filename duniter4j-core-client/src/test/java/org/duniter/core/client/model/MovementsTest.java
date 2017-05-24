package org.duniter.core.client.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.duniter.core.client.model.bma.BlockchainBlock;
import org.duniter.core.client.model.bma.jackson.JacksonUtils;
import org.duniter.core.client.model.local.Movement;
import org.duniter.core.client.model.local.Movements;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

/**
 * Created by blavenie on 23/05/17.
 */
public class MovementsTest {

    @Test
    public void testGetMovements() throws Exception {

        final BlockchainBlock block = readBlockFile("block_with_tx.json");
        List<Movement> mov = Movements.getMovements(block);
        Assert.assertTrue(mov.size() > 0);
    }

    /* -- internal methods -- */

    private BlockchainBlock readBlockFile(String jsonFileName) {
        try {
            ObjectMapper om = JacksonUtils.newObjectMapper();
            BlockchainBlock block = om.readValue(Files.readAllBytes(new File("src/test/resources" , jsonFileName).toPath()), BlockchainBlock.class);
            Assume.assumeNotNull(block);
            return block;
        }
        catch(Exception e) {
            Assume.assumeNoException(e);
            return null;
        }
    }

}

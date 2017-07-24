package org.duniter.core.client.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.duniter.core.client.model.bma.BlockchainBlock;
import org.junit.Assume;

import java.io.File;
import java.nio.file.Files;

/**
 * Created by blavenie on 24/07/17.
 */
public class BlockFileUtils {

    public static BlockchainBlock readBlockFile(String jsonFileName) {
        try {
            ObjectMapper om = org.duniter.core.client.model.bma.jackson.JacksonUtils.newObjectMapper();
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

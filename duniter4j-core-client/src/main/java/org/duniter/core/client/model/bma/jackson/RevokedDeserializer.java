package org.duniter.core.client.model.bma.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.google.gson.JsonParseException;
import org.apache.commons.lang3.StringUtils;
import org.duniter.core.client.model.bma.BlockchainBlock;

import java.io.IOException;

/**
 * Created by blavenie on 07/12/16.
 */
public class RevokedDeserializer extends JsonDeserializer<BlockchainBlock.Revoked> {
    @Override
    public BlockchainBlock.Revoked deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        String str = jp.getText();
        if (StringUtils.isBlank(str)) {
            return null;
        }

        String[] parts = str.split(":");
        if (parts.length != 2) {
            throw new JsonParseException(String.format("Bad format for BlockchainBlock.Revoked. Should have 2 parts, but found %s.", parts.length));
        }

        BlockchainBlock.Revoked result = new BlockchainBlock.Revoked();
        int i = 0;

        result.setSignature(parts[i++]);
        result.setUserId(parts[i++]);

        return result;
    }
}
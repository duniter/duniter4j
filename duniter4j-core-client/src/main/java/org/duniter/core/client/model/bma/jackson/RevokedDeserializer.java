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
        String identityStr = jp.getText();
        if (StringUtils.isBlank(identityStr)) {
            return null;
        }

        String[] identityParts = identityStr.split(":");
        if (identityParts.length != 2) {
            throw new JsonParseException(String.format("Bad format for BlockchainBlock.Revoked. Should have 4 parts, but found %s.", identityParts.length));
        }

        BlockchainBlock.Revoked result = new BlockchainBlock.Revoked();
        int i = 0;

        result.setSignature(identityParts[i++]);
        result.setUserId(identityParts[i++]);

        return result;
    }
}
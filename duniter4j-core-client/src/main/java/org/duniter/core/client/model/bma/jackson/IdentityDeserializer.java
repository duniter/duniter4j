package org.duniter.core.client.model.bma.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.apache.commons.lang3.StringUtils;
import org.duniter.core.client.model.bma.BlockchainBlock;
import org.duniter.core.util.json.JsonSyntaxException;

import java.io.IOException;

/**
 * Created by blavenie on 07/12/16.
 */
public class IdentityDeserializer extends JsonDeserializer<BlockchainBlock.Identity> {
    @Override
    public BlockchainBlock.Identity deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {

        String identityStr = jp.getText();
        if (StringUtils.isBlank(identityStr)) {
            return null;
        }

        String[] identityParts = identityStr.split(":");
        if (identityParts.length != 4) {
            throw new JsonSyntaxException(String.format("Bad format for BlockchainBlock.Identity. Should have 4 parts, but found %s.", identityParts.length));
        }

        BlockchainBlock.Identity result = new BlockchainBlock.Identity();
        int i = 0;

        result.setPublicKey(identityParts[i++]);
        result.setSignature(identityParts[i++]);
        result.setBlockUid(identityParts[i++]);
        result.setUserId(identityParts[i++]);

        return result;
    }
}
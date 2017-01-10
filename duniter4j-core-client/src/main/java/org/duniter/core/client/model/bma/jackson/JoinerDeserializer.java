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
public class JoinerDeserializer extends JsonDeserializer<BlockchainBlock.Joiner> {
    @Override
    public BlockchainBlock.Joiner deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        String identityStr = jp.getText();
        if (StringUtils.isBlank(identityStr)) {
            return null;
        }

        String[] identityParts = identityStr.split(":");
        if (identityParts.length != 5) {
            throw new JsonSyntaxException(String.format("Bad format for BlockchainBlock.Identity. Should have 5 parts, but found %s.", identityParts.length));
        }

        BlockchainBlock.Joiner result = new BlockchainBlock.Joiner();
        int i = 0;

        result.setPublicKey(identityParts[i++]);
        result.setSignature(identityParts[i++]);
        result.setMembershipBlockUid(identityParts[i++]);
        result.setIdtyBlockUid(identityParts[i++]);
        result.setUserId(identityParts[i++]);

        return result;
    }
}
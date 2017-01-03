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
public class CertificationDeserializer extends JsonDeserializer<BlockchainBlock.Certification> {
    @Override
    public BlockchainBlock.Certification deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        String certificationStr = jp.getText();
        if (StringUtils.isBlank(certificationStr)) {
            return null;
        }

        String[] parts = certificationStr.split(":");
        if (parts.length != 4) {
            throw new JsonParseException(String.format("Bad format for BlockchainBlock.Certification. Should have 4 parts, but found %s.", parts.length));
        }

        BlockchainBlock.Certification result = new BlockchainBlock.Certification();
        int i = 0;

        result.setFromPubkey(parts[i++]);
        result.setToPubkey(parts[i++]);
        result.setBlockId(parts[i++]);
        result.setSignature(parts[i++]);

        return result;
    }
}
package org.duniter.core.client.model.bma.jackson;

/*
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
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
public class RevokedDeserializer extends JsonDeserializer<BlockchainBlock.Revoked> {
    @Override
    public BlockchainBlock.Revoked deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        String str = jp.getText();
        if (StringUtils.isBlank(str)) {
            return null;
        }

        String[] parts = str.split(":");
        if (parts.length != 2) {
            throw new JsonSyntaxException(String.format("Bad format for BlockchainBlock.Revoked. Should have 2 parts, but found %s.", parts.length));
        }

        BlockchainBlock.Revoked result = new BlockchainBlock.Revoked();
        int i = 0;

        result.setPubkey(parts[i++]);
        result.setSignature(parts[i++]);

        return result;
    }
}
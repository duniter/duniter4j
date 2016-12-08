package org.duniter.core.client.model.bma.gson;

/*
 * #%L
 * UCoin Java Client :: Core API
 * %%
 * Copyright (C) 2014 - 2015 EIS
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


import com.google.gson.*;
import org.duniter.core.client.model.bma.BlockchainBlock;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Type;

public class RevokedTypeAdapter implements JsonDeserializer<BlockchainBlock.Revoked>, JsonSerializer<BlockchainBlock.Revoked>{

    @Override
    public BlockchainBlock.Revoked deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String identityStr = json.getAsString();
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

    @Override
    public JsonElement serialize(BlockchainBlock.Revoked input, Type type, JsonSerializationContext context) {
        String result = new StringBuilder()
                .append(input.getSignature()).append(":")
                .append(input.getUserId()).toString();

        return context.serialize(result.toString(), String.class);
    }
}

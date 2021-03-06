package org.duniter.core.client.model.bma.converter;

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

import org.apache.commons.lang3.StringUtils;
import org.duniter.core.client.model.bma.BlockchainBlock;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.util.converter.Converter;

/**
 * Created by blavenie on 07/12/16.
 */
public class StringToIdentityConverter
        implements Converter<String, BlockchainBlock.Identity> {

    @Override
    public BlockchainBlock.Identity convert(String identityStr) {
        if (StringUtils.isBlank(identityStr)) return null;

        String[] parts = identityStr.split(":");
        if (parts.length != 4) {
            throw new TechnicalException(String.format("Bad format for BlockchainBlock.Identity. Should have 4 parts, but found %s.", parts.length));
        }

        int i = 0;
        return BlockchainBlock.Identity.builder()
            .publicKey(parts[i++])
            .signature(parts[i++])
            .blockUid(parts[i++])
            .userId(parts[i++])
            .build();
    }
}
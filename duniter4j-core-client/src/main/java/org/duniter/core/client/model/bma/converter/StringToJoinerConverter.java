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
 * Converter for BlockchainBlock.Joiner
 */
public class StringToJoinerConverter implements Converter<String, BlockchainBlock.Joiner> {

    @Override
    public BlockchainBlock.Joiner convert(String source) {
        if (StringUtils.isBlank(source)) return null;

        String[] parts = source.split(":");
        if (parts.length != 5) {
            throw new TechnicalException(String.format("Bad format for BlockchainBlock.Identity. Should have 5 parts, but found %s.", parts.length));
        }

        int i = 0;
        return BlockchainBlock.Joiner.builder()
                .publicKey(parts[i++])
                .signature(parts[i++])
                .membershipBlockUid(parts[i++])
                .idtyBlockUid(parts[i++])
                .userId(parts[i++])
                .build();
    }
}
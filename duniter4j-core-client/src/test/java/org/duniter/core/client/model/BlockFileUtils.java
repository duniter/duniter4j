package org.duniter.core.client.model;

/*-
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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.duniter.core.client.model.bma.BlockchainBlock;
import org.duniter.core.client.model.bma.jackson.JacksonUtils;
import org.junit.Assume;

import java.io.File;
import java.nio.file.Files;

/**
 * Created by blavenie on 24/07/17.
 */
public class BlockFileUtils {

    public static BlockchainBlock readBlockFile(String jsonFileName) {
        try {
            ObjectMapper om = JacksonUtils.getThreadObjectMapper();
            BlockchainBlock block = om.readValue(Files.readAllBytes(new File("src/test/resources" , jsonFileName).toPath()), BlockchainBlock.class);
            Assume.assumeNotNull(block);
            return block;
        }
        catch(Exception e) {
            Assume.assumeNoException(e);
            return null;
        }
    }
}

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
import org.duniter.core.client.model.bma.Endpoints;
import org.duniter.core.client.model.bma.NetworkPeering;
import org.duniter.core.client.model.bma.Ws2pHead;
import org.duniter.core.client.model.bma.Ws2pHeads;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by blavenie on 07/12/16.
 */
public class Ws2pHeadDeserializer extends JsonDeserializer<Ws2pHead> {

    private static final Logger log = LoggerFactory.getLogger(Ws2pHeadDeserializer.class);

    private boolean debug;

    public Ws2pHeadDeserializer() {
        this.debug = log.isDebugEnabled();
    }

    @Override
    public Ws2pHead deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {

        String ept = jp.getText();

        try {
            return Ws2pHeads.parse(ept);
        } catch(IOException e) {
            // Unable to parse endpoint: continue (will skip this endpoint)
            if (debug) {
                log.warn(e.getMessage(), e); // link the exception
            }
            else {
                log.debug(e.getMessage());
            }
            return null;
        }
    }
}
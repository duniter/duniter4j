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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.duniter.core.client.model.bma.Endpoints;
import org.duniter.core.client.model.bma.NetworkPeering;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by blavenie on 17/10/18.
 */
public class EndpointSerializer extends JsonSerializer<NetworkPeering.Endpoint> {

    private static final Logger log = LoggerFactory.getLogger(EndpointSerializer.class);

    private boolean debug;

    public EndpointSerializer() {
        this.debug = log.isDebugEnabled();
    }

    @Override
    public void serialize(NetworkPeering.Endpoint endpoint, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) {

        try {
            jsonGenerator.writeString(endpoint.toString());
        } catch(IOException e) {
            // Unable to parse endpoint: continue (will skip this endpoint)
            if (debug) {
                log.warn(e.getMessage(), e); // link the exception
            }
            else {
                log.debug(e.getMessage());
            }
        }
    }
}
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

import org.duniter.core.client.model.bma.Endpoints;
import org.duniter.core.client.model.bma.NetworkPeering;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.util.converter.Converter;

import java.io.IOException;

/**
 * Created by blavenie on 07/12/16.
 */
public class StringToEndpointConverter
        implements Converter<String, NetworkPeering.Endpoint> {

    @Override
    public NetworkPeering.Endpoint convert(String source) {
        try {
            return Endpoints.parse(source).orElse(null);
        } catch (IOException e) {
            throw new TechnicalException(e);
        }
    }
}
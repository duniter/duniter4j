package org.duniter.core.client.model.bma;

/*
 * #%L
 * UCoin Java :: Core Client API
 * %%
 * Copyright (C) 2014 - 2016 EIS
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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Endpoint used by Duniter protocol, and Cesium-plus-pod API.<br/>
 * Label can be override using static the method <code>EndpointApi.setLabel()</code>
 */
public enum EndpointApi implements IEndpointApi {

    BASIC_MERKLED_API(),
    BMAS(),
    BMATOR(),
    WS2P(),
    WS2PTOR(),
    ES_CORE_API(),
    ES_USER_API(),
    ES_SUBSCRIPTION_API(),
    MONIT_API(),
    UNDEFINED(),
    // TODO: remove this
    GCHANGE_API();


    private static final Logger log = LoggerFactory.getLogger(EndpointApi.class);

    private String label;

    EndpointApi() {
        this.label = this.name();
    }

    public String label() {
        return this.label;
    }

    /**
     * Allow to change the API label.
     * Useful for reuse and API enumeration, with a new label (eg: ES_CORE_API => GCHANGE_API)
     * @param api
     * @param label
     */
    public void setLabel(String label) {
        if (!this.label.equals(label)) {
            log.warn(String.format("Endpoint API '%s' label change to '%s'", this.name(), label));
            this.label = label;
        }
    }

    public boolean useHttpProtocol(String api) {
        return !useWebSocketProtocol(api);
    }

    public static boolean useWebSocketProtocol(String api) {
        return WS2P.name().equals(api);
    }
}

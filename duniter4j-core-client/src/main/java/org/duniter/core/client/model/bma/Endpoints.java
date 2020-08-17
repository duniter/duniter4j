package org.duniter.core.client.model.bma;

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
import org.apache.http.conn.util.InetAddressUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by blavenie on 07/12/16.
 */
public class Endpoints {


    private static final Logger log = LoggerFactory.getLogger(Endpoints.class);

    // Path regexp (can have no starting slash - see issue https://git.duniter.org/clients/cesium-grp/cesium-plus-pod/-/issues/41)
    public static final String PATH_REGEXP = "\\/?[^\\/\\s]+(?:\\/[^\\/\\s]+)*";
    public static final String EP_END_REGEXP = "(?: ([a-z0-9_ğĞ][a-z0-9-_.ğĞ]*))?(?: ([0-9.]+))?(?: ([0-9a-f:]+)(?:%[a-z0-9]+)?)?(?: ([0-9]+))(?: ("+PATH_REGEXP + "))?$";
    public static final String BMA_API_REGEXP = "^BASIC_MERKLED_API" + EP_END_REGEXP;
    public static final String BMAS_API_REGEXP = "^BMAS" + EP_END_REGEXP;
    public static final String WS2P_API_REGEXP = "^(WS2P(?:TOR)?) ([a-f0-9]{7,8})" + EP_END_REGEXP;
    public static final String OTHER_API_REGEXP = "^([A-Z_-]+)" + EP_END_REGEXP;

    private static Pattern bmaPattern = Pattern.compile(BMA_API_REGEXP);
    private static Pattern bmasPattern = Pattern.compile(BMAS_API_REGEXP);
    private static Pattern ws2pPattern = Pattern.compile(WS2P_API_REGEXP);
    private static Pattern otherApiPattern = Pattern.compile(OTHER_API_REGEXP);

    private Endpoints() {
       // helper class
    }

    public static Optional<NetworkPeering.Endpoint> parse(String raw) throws IOException {

        NetworkPeering.Endpoint endpoint = new NetworkPeering.Endpoint();
        endpoint.setRaw(raw);

        // BMA API
        Matcher mather = bmaPattern.matcher(raw);
        if (mather.matches()) {
            endpoint.api = EndpointApi.BASIC_MERKLED_API.name();
            parseDefaultFormatEndPoint(mather, endpoint, 1);
            return Optional.of(endpoint);
        }

        // BMAS API
        mather = bmasPattern.matcher(raw);
        if (mather.matches()) {
            endpoint.api = EndpointApi.BMAS.name();
            parseDefaultFormatEndPoint(mather, endpoint, 1);
            return Optional.of(endpoint);
        }

        // WS2P API
        mather = ws2pPattern.matcher(raw);
        if (mather.matches()) {
            String api = mather.group(1);
            try {
                endpoint.api = EndpointApi.valueOf(api).name();
                endpoint.id = mather.group(2);
                parseDefaultFormatEndPoint(mather, endpoint, 3);
                return Optional.of(endpoint);
            } catch(Exception e) {
                // Unknown API
                throw new IOException("Unable to deserialize endpoint: WS2P api [" + api + "]", e); // link the exception
            }
        }

        // Other API
        mather = otherApiPattern.matcher(raw);
        if (mather.matches()) {
            String api = mather.group(1);
            try {
                endpoint.api = api;
                parseDefaultFormatEndPoint(mather, endpoint, 2);
                return Optional.of(endpoint);
            } catch(Exception e) {
                // Unknown API
                throw new IOException("Unable to deserialize endpoint: unknown api [" + api + "]", e); // link the exception
            }
        }

        log.warn("Unable to parse Endpoint: " + raw);
        return Optional.empty();
    }

    public static void parseDefaultFormatEndPoint(Matcher matcher, NetworkPeering.Endpoint endpoint, int startGroup) {
        for(int i=startGroup; i<=matcher.groupCount(); i++) {
            String word = matcher.group(i);

            if (StringUtils.isNotBlank(word)) {
                if (InetAddressUtils.isIPv4Address(word)) {
                    endpoint.ipv4 = word;
                } else if (InetAddressUtils.isIPv6Address(word)) {
                    endpoint.ipv6 = word;
                } else if ((i == matcher.groupCount() || i == matcher.groupCount() -1) && word.matches("^\\d+$")){
                    endpoint.port = Integer.parseInt(word);
                } else if (i == matcher.groupCount()) {
                    // Path without starting slash must e accepted - fix issue https://git.duniter.org/clients/cesium-grp/cesium-plus-pod/-/issues/41
                    if (word.startsWith("/") || word.matches(PATH_REGEXP)) {
                        endpoint.path = word;
                    }
                }
                else {
                    endpoint.dns = word;
                }
            }
        }
    }
}
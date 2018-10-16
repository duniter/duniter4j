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

import org.duniter.core.util.StringUtils;
import org.duniter.core.util.http.InetAddressUtils;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by blavenie on 07/12/16.
 */
public class Endpoints {

    public static final String EP_END_REGEXP = "(?:[ ]+([a-z0-9-_]+[.][a-z0-9-_.]*))?(?:[ ]+([0-9.]+))?(?:[ ]+([0-9a-f:]+))?(?:[ ]+([0-9]+))(?:[ ]+(/[^/]+))?$";
    public static final String BMA_API_REGEXP = "^BASIC_MERKLED_API" + EP_END_REGEXP;
    public static final String BMAS_API_REGEXP = "^BMAS" + EP_END_REGEXP;
    public static final String WS2P_API_REGEXP = "^WS2P[ ]+([a-z0-9]+)[ ]+" + EP_END_REGEXP;
    public static final String OTHER_API_REGEXP = "^([A-Z_-]+)" + EP_END_REGEXP;

    private static Pattern bmaPattern = Pattern.compile(BMA_API_REGEXP);
    private static Pattern bmasPattern = Pattern.compile(BMAS_API_REGEXP);
    private static Pattern ws2pPattern = Pattern.compile(WS2P_API_REGEXP);
    private static Pattern otherApiPattern = Pattern.compile(OTHER_API_REGEXP);

    private Endpoints() {
       // helper class
    }

    public static NetworkPeering.Endpoint parse(String ept) throws IOException {

        NetworkPeering.Endpoint endpoint = new NetworkPeering.Endpoint();

        // BMA API
        Matcher mather = bmaPattern.matcher(ept);
        if (mather.matches()) {
            endpoint.api = EndpointApi.BASIC_MERKLED_API;
            parseDefaultFormatEndPoint(mather, endpoint, 1);
            return endpoint;
        }

        // BMAS API
        mather = bmasPattern.matcher(ept);
        if (mather.matches()) {
            endpoint.api = EndpointApi.BMAS;
            parseDefaultFormatEndPoint(mather, endpoint, 1);
            return endpoint;
        }

        // WS2P API
        mather = ws2pPattern.matcher(ept);
        if (mather.matches()) {
            endpoint.api = EndpointApi.WS2P;
            endpoint.id = mather.group(1);
            parseDefaultFormatEndPoint(mather, endpoint, 2);
            return endpoint;
        }

        // Other API
        mather = otherApiPattern.matcher(ept);
        if (mather.matches()) {
            String api = mather.group(1);
            try {
                endpoint.api = EndpointApi.valueOf(api);
                parseDefaultFormatEndPoint(mather, endpoint, 2);
                return endpoint;
            } catch(Exception e) {
                // Unknown API
                throw new IOException("Unable to deserialize endpoint: unknown api [" + api + "]", e); // link the exception
            }
        }

        return null;
    }

    public static void parseDefaultFormatEndPoint(Matcher matcher, NetworkPeering.Endpoint endpoint, int startGroup) {
        for(int i=startGroup; i<=matcher.groupCount(); i++) {
            String word = matcher.group(i);

            if (StringUtils.isNotBlank(word)) {
                if (InetAddressUtils.isIPv4Address(word)) {
                    endpoint.ipv4 = word;
                } else if (InetAddressUtils.isIPv6Address(word)) {
                    endpoint.ipv6 = word;
                } else if (i == matcher.groupCount() || (i == matcher.groupCount() -1) && word.matches("\\d+")){
                    endpoint.port = Integer.parseInt(word);
                } else if (word.startsWith("/")) {
                    endpoint.path = word;
                } else {
                    endpoint.dns = word;
                }
            }
        }
    }
}
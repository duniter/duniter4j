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

import com.google.common.collect.Lists;
import org.duniter.core.util.Preconditions;
import org.duniter.core.util.StringUtils;
import org.duniter.core.util.http.InetAddressUtils;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Created by blavenie on 07/12/16.
 */
public class NetworkPeerings {

    private NetworkPeerings() {
       // helper class
    }

    public static NetworkPeering parse(String document) throws IOException {
        NetworkPeering result = new NetworkPeering();
        return parse(document, result);
    }

    public static NetworkPeering parse(String document, NetworkPeering result) throws IOException {
        Preconditions.checkNotNull(document);

        try {

            String[] lines = document.trim().split("\n");

            Preconditions.checkArgument(lines.length >= 7, "Invalid document");

            int i = 0;
            String line;
            for (; i < 5; ) {
                line = lines[i++].trim();
                if (line.startsWith("Version: ")) {
                    result.setVersion(line.substring(9));
                } else if (line.startsWith("Type: ")) {
                    String type = line.substring(6);
                    Preconditions.checkArgument(Protocol.TYPE_PEER.equals(type), "Invalid type found in document. Expected: " + Protocol.TYPE_PEER);
                } else if (line.startsWith("Currency: ")) {
                    result.setCurrency(line.substring(10));
                } else if (line.startsWith("PublicKey: ")) {
                    result.setPubkey(line.substring(11));
                } else if (line.startsWith("Block: ")) {
                    result.setBlock(line.substring(7));
                }
            }
            line = lines[i++].trim();
            Preconditions.checkArgument("Endpoints:".equals(line), "Invalid document format. Missing line 'Endpoint:' !");
            List<NetworkPeering.Endpoint> endpoints = Lists.newArrayList();
            for (; i < lines.length - 1; ) {
                line = lines[i++].trim();
                NetworkPeering.Endpoint ep = Endpoints.parse(line);
                Preconditions.checkNotNull(ep, "Unable to parse endpoint: " + line);
                endpoints.add(ep);
            }
            result.setEndpoints(endpoints.toArray(new NetworkPeering.Endpoint[endpoints.size()]));

            result.setSignature(lines[lines.length - 1]);

            result.setStatus("UP");

            return result;
        }
        catch(Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }
}
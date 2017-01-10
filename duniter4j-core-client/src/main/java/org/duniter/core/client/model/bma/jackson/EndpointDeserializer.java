package org.duniter.core.client.model.bma.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.apache.http.conn.util.InetAddressUtils;
import org.duniter.core.client.model.bma.EndpointProtocol;
import org.duniter.core.client.model.bma.NetworkPeering;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by blavenie on 07/12/16.
 */
public class EndpointDeserializer extends JsonDeserializer<NetworkPeering.Endpoint> {
    @Override
    public NetworkPeering.Endpoint deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {

        String ept = jp.getText();
        ArrayList<String> parts = new ArrayList<>(Arrays.asList(ept.split(" ")));
        NetworkPeering.Endpoint endpoint = new NetworkPeering.Endpoint();
        endpoint.port = Integer.parseInt(parts.remove(parts.size() - 1));
        for (String word : parts) {
            if (InetAddressUtils.isIPv4Address(word)) {
                endpoint.ipv4 = word;
            } else if (InetAddressUtils.isIPv6Address(word)) {
                endpoint.ipv6 = word;
            } else if (word.startsWith("http")) {
                endpoint.url = word;
            } else {
                try {
                    endpoint.protocol = EndpointProtocol.valueOf(word);
                } catch (IllegalArgumentException e) {
                    // skip this part
                }
            }
        }

        if (endpoint.protocol == null) {
            endpoint.protocol = EndpointProtocol.UNDEFINED;
        }

        return endpoint;
    }
}
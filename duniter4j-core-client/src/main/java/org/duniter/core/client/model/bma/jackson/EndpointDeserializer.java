package org.duniter.core.client.model.bma.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.duniter.core.client.model.bma.EndpointApi;
import org.duniter.core.client.model.bma.NetworkPeering;
import org.duniter.core.util.StringUtils;
import org.duniter.core.util.http.InetAddressUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by blavenie on 07/12/16.
 */
public class EndpointDeserializer extends JsonDeserializer<NetworkPeering.Endpoint> {

    private static final Logger log = LoggerFactory.getLogger(EndpointDeserializer.class);

    public static final String EP_END_REGEXP = "(?:[ ]+([a-z0-9-_]+[.][a-z0-9-_.]*))?(?:[ ]+([0-9.]+))?(?:[ ]+([0-9a-f:]+))?(?:[ ]+([0-9]+))$";
    public static final String BMA_API_REGEXP = "^BASIC_MERKLED_API" + EP_END_REGEXP;
    public static final String BMAS_API_REGEXP = "^BMAS" + EP_END_REGEXP;
    public static final String OTHER_API_REGEXP = "^([A-Z_-]+)" + EP_END_REGEXP;

    private Pattern bmaPattern;
    private Pattern bmasPattern;
    private Pattern otherApiPattern;

    public EndpointDeserializer() {
        bmaPattern = Pattern.compile(BMA_API_REGEXP);
        bmasPattern = Pattern.compile(BMAS_API_REGEXP);
        otherApiPattern = Pattern.compile(OTHER_API_REGEXP);
    }

    @Override
    public NetworkPeering.Endpoint deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {

        String ept = jp.getText();

        NetworkPeering.Endpoint endpoint = new NetworkPeering.Endpoint();

        // BMA API
        Matcher mather = bmaPattern.matcher(ept);
        if (mather.matches()) {
            endpoint.api = EndpointApi.BASIC_MERKLED_API;

            for(int i=1; i<=mather.groupCount(); i++) {
                String word = mather.group(i);

                if (StringUtils.isNotBlank(word)) {
                    if (InetAddressUtils.isIPv4Address(word)) {
                        endpoint.ipv4 = word;
                    } else if (InetAddressUtils.isIPv6Address(word)) {
                        endpoint.ipv6 = word;
                    } else if (i == mather.groupCount() && word.matches("\\d+")){
                        endpoint.port = Integer.parseInt(word);
                    } else {
                        endpoint.dns = word;
                    }
                }
            }

            return endpoint;
        }

        // BMAS API
        mather = bmasPattern.matcher(ept);
        if (mather.matches()) {
            endpoint.api = EndpointApi.BMAS;

            for(int i=1; i<=mather.groupCount(); i++) {
                String word = mather.group(i);

                if (StringUtils.isNotBlank(word)) {
                    if (InetAddressUtils.isIPv4Address(word)) {
                        endpoint.ipv4 = word;
                    } else if (InetAddressUtils.isIPv6Address(word)) {
                        endpoint.ipv6 = word;
                    } else if (i == mather.groupCount() && word.matches("\\d+")){
                        endpoint.port = Integer.parseInt(word);
                    } else {
                        endpoint.dns = word;
                    }
                }
            }

            return endpoint;
        }

        // Other API
        mather = otherApiPattern.matcher(ept);
        if (mather.matches()) {
            try {
                endpoint.api = EndpointApi.valueOf(mather.group(1));
            } catch(Exception e) {
                log.warn("Unable to deserialize endpoint: unknown api [" + mather.group(1) + "]");
                // not known API: skip
                return null;
            }

            for(int i=2; i<=mather.groupCount(); i++) {
                String word = mather.group(i);

                if (StringUtils.isNotBlank(word)) {
                    if (InetAddressUtils.isIPv4Address(word)) {
                        endpoint.ipv4 = word;
                    } else if (InetAddressUtils.isIPv6Address(word)) {
                        endpoint.ipv6 = word;
                    } else if (i == mather.groupCount() && word.matches("\\d+")){
                        endpoint.port = Integer.parseInt(word);
                    } else {
                        endpoint.dns = word;
                    }
                }
            }

            return endpoint;
        }

        return null;
    }
}
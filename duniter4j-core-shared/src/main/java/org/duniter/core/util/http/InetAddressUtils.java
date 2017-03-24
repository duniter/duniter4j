package org.duniter.core.util.http;

import java.util.regex.Pattern;

/**
 * Created by blavenie on 24/03/17.
 */
public class InetAddressUtils {

    public static final Pattern LOCAL_IP_ADDRESS_PATTERN = Pattern.compile("^127[.]0[.]0.|192[.]168[.]|10[.]0[.]0[.]|172[.]16[.]");

    private InetAddressUtils() {
    }

    public static boolean isNotLocalIPv4Address(String input) {
        return org.apache.http.conn.util.InetAddressUtils.isIPv4Address(input) &&
                !LOCAL_IP_ADDRESS_PATTERN.matcher(input).matches();
    }

    public static boolean isIPv4Address(String input) {
        return org.apache.http.conn.util.InetAddressUtils.isIPv4Address(input);
    }

    public static boolean isIPv6Address(String input) {
        return org.apache.http.conn.util.InetAddressUtils.isIPv6Address(input);
    }
}

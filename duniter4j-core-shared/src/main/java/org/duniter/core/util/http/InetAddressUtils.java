package org.duniter.core.util.http;

/*
 * #%L
 * Duniter4j :: Core Shared
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

import java.util.regex.Pattern;

/**
 * Created by blavenie on 24/03/17.
 */
public class InetAddressUtils {

    public static final Pattern LOCAL_IP_ADDRESS_PATTERN = Pattern.compile("^127[.]0[.]0.");

    public static final Pattern INTRANET_ADDRESS_PATTERN = Pattern.compile("^127[.]0[.]0.|192[.]168[.]|10[.]0[.]0[.]|172[.]16[.]");

    private InetAddressUtils() {
    }

    public static boolean isIPv4Address(String input) {
        return org.apache.http.conn.util.InetAddressUtils.isIPv4Address(input);
    }

    public static boolean isIPv6Address(String input) {
        return org.apache.http.conn.util.InetAddressUtils.isIPv6Address(input);
    }

    public static boolean isInternetIPv4Address(String input) {
        return isIPv4Address(input) &&
                !INTRANET_ADDRESS_PATTERN.matcher(input).find();
    }

    public static boolean isIntranetIPv4Address(String input) {
        return isIPv4Address(input) && INTRANET_ADDRESS_PATTERN.matcher(input).find();
    }


    public static boolean isIntranetAddress(String input) {
        return isIntranetIPv4Address(input) || "localhost".equalsIgnoreCase(input);
    }

    public static boolean isInternetAddress(String input) {
        return isIPv6Address(input) || isInternetIPv4Address(input);
    }

    public static boolean isLocalAddress(String input) {
        return (isIPv4Address(input) && LOCAL_IP_ADDRESS_PATTERN.matcher(input).find()) || "localhost".equalsIgnoreCase(input);
    }

    public static boolean isNotLocalAddress(String input) {
        return isIPv6Address(input) || (isIPv4Address(input) && !LOCAL_IP_ADDRESS_PATTERN.matcher(input).find());
    }
}

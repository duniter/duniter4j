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
 * Created by blavenie on 04/08/23
 */
public class DnsUtils {

    public static final Pattern HOST_NAME_PATTERN = Pattern.compile("^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$");

    private DnsUtils() {
    }

    public static String normalize(String input) {
        return org.apache.http.conn.util.DnsUtils.normalize(input);
    }

    public static boolean isHostName(String input) {
        return !InetAddressUtils.isIPv4Address(input)
            && !InetAddressUtils.isIPv6Address(input)
            && HOST_NAME_PATTERN.matcher(input).matches();
    }

    public static boolean isLocalhost(String input) {
        return "localhost".equalsIgnoreCase(input);
    }

    public static boolean isInternetHostName(String input) {
        return isHostName(input) && !isLocalhost(input);
    }
}

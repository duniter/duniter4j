package org.duniter.core.util.http;

/*-
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

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by blavenie on 30/03/17.
 */
public class InetAddressUtilsTest {

    @Test
    public void isIPv4Address() {

        boolean checkTrue = InetAddressUtils.isIPv4Address("192.168.0.254");
        Assert.assertTrue(checkTrue);

        // Try a host name
        boolean checkFalse = InetAddressUtils.isInternetAddress("g1.duniter.org");
        Assert.assertFalse(checkFalse);
    }

    @Test
    public void isInternetIPv4Address() {

        Assert.assertFalse(InetAddressUtils.isInternetIPv4Address("192.168.1.11"));
        Assert.assertFalse(InetAddressUtils.isInternetIPv4Address("abc"));

        Assert.assertTrue(InetAddressUtils.isInternetIPv4Address("82.239.120.237"));

        // Try a host name
        boolean checkFalse = InetAddressUtils.isInternetAddress("g1.duniter.org");
        Assert.assertFalse(checkFalse);
    }

    @Test
    public void isIntranetIPv4Address() {

        boolean check = InetAddressUtils.isIntranetIPv4Address("192.168.1.11");
        Assert.assertTrue(check);

        check = InetAddressUtils.isIntranetIPv4Address("127.0.0.1");
        Assert.assertTrue(check);

        // Try a host name
        boolean checkFalse = InetAddressUtils.isInternetAddress("g1.duniter.org");
        Assert.assertFalse(checkFalse);
    }

    @Test
    public void isLocalAddress() {

        boolean checkTrue = InetAddressUtils.isLocalAddress("127.0.0.1");
        Assert.assertTrue(checkTrue);

        checkTrue = InetAddressUtils.isLocalAddress("localhost");
        Assert.assertTrue(checkTrue);

        boolean checkFalse = InetAddressUtils.isLocalAddress("192.168.0.254");
        Assert.assertFalse(checkFalse);

        checkFalse = InetAddressUtils.isLocalAddress("10.0.0.1");
        Assert.assertFalse(checkFalse);

        // Try a host name
        checkFalse = InetAddressUtils.isInternetAddress("g1.duniter.org");
        Assert.assertFalse(checkFalse);
    }

    @Test
    public void isIntranetAddress() {

        boolean checkTrue = InetAddressUtils.isIntranetAddress("127.0.0.1");
        Assert.assertTrue(checkTrue);

        checkTrue = InetAddressUtils.isIntranetAddress("10.0.0.1");
        Assert.assertTrue(checkTrue);

        checkTrue = InetAddressUtils.isIntranetAddress("localhost");
        Assert.assertTrue(checkTrue);

        checkTrue = InetAddressUtils.isIntranetAddress("192.168.0.254");
        Assert.assertTrue(checkTrue);

        // Try a host name
        boolean checkFalse = InetAddressUtils.isInternetAddress("g1.duniter.org");
        Assert.assertFalse(checkFalse);
    }

    @Test
    public void isInternetAddress() {

        boolean checkTrue = InetAddressUtils.isInternetAddress("88.168.0.254");
        Assert.assertTrue(checkTrue);

        boolean checkFalse = InetAddressUtils.isInternetAddress("127.0.0.1");
        Assert.assertFalse(checkFalse);

        checkFalse = InetAddressUtils.isInternetAddress("10.0.0.1");
        Assert.assertFalse(checkFalse);

        checkFalse = InetAddressUtils.isInternetAddress("localhost");
        Assert.assertFalse(checkFalse);

        checkFalse = InetAddressUtils.isInternetAddress("192.168.0.254");
        Assert.assertFalse(checkFalse);

        // Try a host name
        checkFalse = InetAddressUtils.isInternetAddress("g1.duniter.org");
        Assert.assertFalse(checkFalse);
    }

    @Test
    public void isNotLocalAddress() {

        boolean checkTrue = InetAddressUtils.isNotLocalAddress("10.0.0.1");
        Assert.assertTrue(checkTrue);

        checkTrue = InetAddressUtils.isNotLocalAddress("192.168.0.254");
        Assert.assertTrue(checkTrue);

        boolean checkFalse = InetAddressUtils.isNotLocalAddress("127.0.0.1");
        Assert.assertFalse(checkFalse);

        checkFalse = InetAddressUtils.isNotLocalAddress("localhost");
        Assert.assertFalse(checkFalse);

        // Try a host name
        checkFalse = InetAddressUtils.isNotLocalAddress("g1.duniter.org");
        Assert.assertFalse(checkFalse);
    }
}

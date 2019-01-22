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
    public void isNotLocalIPv4Address() {

        Assert.assertFalse(InetAddressUtils.isNotLocalIPv4Address("192.168.1.11"));
        Assert.assertFalse(InetAddressUtils.isNotLocalIPv4Address("abc"));

        Assert.assertTrue(InetAddressUtils.isNotLocalIPv4Address("82.239.120.237"));
    }

    @Test
    public void isLocalIPv4Address() {

        boolean check = InetAddressUtils.isLocalIPv4Address("192.168.1.11");
        Assert.assertTrue(check);

        check = InetAddressUtils.isLocalIPv4Address("127.0.0.1");
        Assert.assertTrue(check);
    }

    @Test
    public void isLocalAddress() {

        boolean check = InetAddressUtils.isLocalAddress("localhost");
        Assert.assertTrue(check);
    }
}

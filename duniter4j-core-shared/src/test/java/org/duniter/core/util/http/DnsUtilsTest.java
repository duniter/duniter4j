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
public class DnsUtilsTest {

    @Test
    public void isHostName() {

        boolean checkTrue = DnsUtils.isHostName("g1.duniter.org");
        Assert.assertTrue(checkTrue);

        checkTrue = DnsUtils.isHostName("localhost");
        Assert.assertTrue(checkTrue);

        // Try with an IP
        boolean checkFalse = DnsUtils.isHostName("192.168.0.254");
        Assert.assertFalse(checkFalse);
    }

    @Test
    public void isInternetHostName() {

        boolean checkTrue = DnsUtils.isInternetHostName("g1.duniter.org");
        Assert.assertTrue(checkTrue);

        boolean checkFalse = DnsUtils.isInternetHostName("localhost");
        Assert.assertFalse(checkFalse);

        // Try with an IP
        checkFalse = DnsUtils.isInternetHostName("192.168.0.254");
        Assert.assertFalse(checkFalse);
    }
}

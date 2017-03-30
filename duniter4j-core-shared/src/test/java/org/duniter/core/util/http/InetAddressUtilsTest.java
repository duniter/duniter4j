package org.duniter.core.util.http;

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
    }

}

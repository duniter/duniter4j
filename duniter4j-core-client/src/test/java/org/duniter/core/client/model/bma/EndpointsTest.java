package org.duniter.core.client.model.bma;

import org.junit.Assert;
import org.junit.Test;

public class EndpointsTest {

    @Test
    public void parse() throws Exception {

        NetworkPeering.Endpoint ep = Endpoints.parse("BASIC_MERKLED_API g1.duniter.fr 81.81.81.81 80");
        Assert.assertNotNull(ep);
        Assert.assertEquals(EndpointApi.BASIC_MERKLED_API, ep.api);
        Assert.assertEquals("g1.duniter.fr", ep.dns);
        Assert.assertEquals("81.81.81.81", ep.ipv4);
        Assert.assertNotNull(ep.port);
        Assert.assertEquals(80, ep.port.intValue());
        Assert.assertNull(ep.id);
        Assert.assertNull(ep.path);

        ep = Endpoints.parse("BMAS g1.duniter.fr 443");
        Assert.assertNotNull(ep);
        Assert.assertEquals(EndpointApi.BMAS, ep.api);
        Assert.assertEquals("g1.duniter.fr", ep.dns);
        Assert.assertNotNull(ep.port);
        Assert.assertEquals(443, ep.port.intValue());
        Assert.assertNull(ep.id);
        Assert.assertNull(ep.path);

        ep = Endpoints.parse("WS2P fb17fcd4 g1.duniter.fr 443 /ws2p");
        Assert.assertNotNull(ep);
        Assert.assertNotNull(ep.id);
        Assert.assertNotNull(ep.path);
        Assert.assertEquals(EndpointApi.WS2P, ep.api);
        Assert.assertEquals("g1.duniter.fr", ep.dns);
        Assert.assertNotNull(ep.port);
        Assert.assertEquals(443, ep.port.intValue());

        ep = Endpoints.parse("GCHANGE_API data.gchange.fr 443");
        Assert.assertNotNull(ep);
        Assert.assertEquals(ep.api, EndpointApi.GCHANGE_API);
        Assert.assertNull(ep.id);
        Assert.assertNull(ep.path);

    }

}

package org.duniter.core.client.model.bma;

import org.junit.Assert;
import org.junit.Test;

public class EndpointsTest {

    @Test
    public void parse() throws Exception {

        // Parse valid endpoints
        NetworkPeering.Endpoint ep = Endpoints.parse("BASIC_MERKLED_API g1.duniter.fr 81.81.81.81 80").orElse(null);
        Assert.assertNotNull(ep);
        Assert.assertEquals(EndpointApi.BASIC_MERKLED_API, ep.api);
        Assert.assertEquals("g1.duniter.fr", ep.dns);
        Assert.assertEquals("81.81.81.81", ep.ipv4);
        Assert.assertNotNull(ep.port);
        Assert.assertEquals(80, ep.port.intValue());
        Assert.assertNull(ep.id);
        Assert.assertNull(ep.path);

        ep = Endpoints.parse("BMAS g1.duniter.fr 443").orElse(null);
        Assert.assertNotNull(ep);
        Assert.assertEquals(EndpointApi.BMAS, ep.api);
        Assert.assertEquals("g1.duniter.fr", ep.dns);
        Assert.assertNotNull(ep.port);
        Assert.assertEquals(443, ep.port.intValue());
        Assert.assertNull(ep.id);
        Assert.assertNull(ep.path);

        ep = Endpoints.parse("WS2P fb17fcd4 g1.duniter.fr 443 /ws2p").orElse(null);
        Assert.assertNotNull(ep);
        Assert.assertNotNull(ep.id);
        Assert.assertNotNull(ep.path);
        Assert.assertEquals(EndpointApi.WS2P, ep.api);
        Assert.assertEquals("g1.duniter.fr", ep.dns);
        Assert.assertNotNull(ep.port);
        Assert.assertEquals(443, ep.port.intValue());

        // ws2pId on 7 characters
        ep = Endpoints.parse("WS2P 90e9b12 duniter.g1.1000i100.fr 443 /ws2p").orElse(null);
        Assert.assertNotNull(ep);
        Assert.assertEquals(ep.api, EndpointApi.WS2P);
        Assert.assertNotNull(ep.id);
        Assert.assertNotNull(ep.path);

        ep = Endpoints.parse("WS2PTOR 1be86653 3k2zovlpihbt3j3g.onion 20901").orElse(null);
        Assert.assertNotNull(ep);
        Assert.assertNotNull(ep.id);
        Assert.assertNull(ep.path);
        Assert.assertEquals(EndpointApi.WS2PTOR, ep.api);
        Assert.assertEquals("3k2zovlpihbt3j3g.onion", ep.dns);
        Assert.assertNotNull(ep.port);
        Assert.assertEquals(20901, ep.port.intValue());

        ep = Endpoints.parse("GCHANGE_API data.gchange.fr 443").orElse(null);
        Assert.assertNotNull(ep);
        Assert.assertEquals(ep.api, EndpointApi.GCHANGE_API);
        Assert.assertNull(ep.id);
        Assert.assertNull(ep.path);

        // Parse Invalid endpoints

        // This must failed (missing port)
        ep = Endpoints.parse("BMAS g1.cgeek.fr").orElse(null);
        Assert.assertNull(ep);

        // This must failed (because bad ID)
        ep = Endpoints.parse("WS2P R8t2sg7w g1.ambau.ovh 443").orElse(null);
        Assert.assertNull(ep);

        // This must failed (missing path first '/')
        ep = Endpoints.parse("WS2P 90e9b12 duniter.g1.1000i100.fr 443 ws2p").orElse(null);
        Assert.assertNull(ep);
    }

}

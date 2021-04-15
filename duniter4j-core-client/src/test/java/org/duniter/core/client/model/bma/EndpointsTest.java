package org.duniter.core.client.model.bma;

/*-
 * #%L
 * Duniter4j :: Core Client API
 * %%
 * Copyright (C) 2014 - 2021 Duniter Team
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

public class EndpointsTest {

    @Test
    public void parse() throws Exception {

        // Parse valid endpoints
        NetworkPeering.Endpoint ep = Endpoints.parse("BASIC_MERKLED_API g1.duniter.fr 81.81.81.81 80").orElse(null);
        Assert.assertNotNull(ep);
        Assert.assertEquals(EndpointApi.BASIC_MERKLED_API.label(), ep.api);
        Assert.assertEquals("g1.duniter.fr", ep.dns);
        Assert.assertEquals("81.81.81.81", ep.ipv4);
        Assert.assertNotNull(ep.port);
        Assert.assertEquals(80, ep.port.intValue());
        Assert.assertNull(ep.id);
        Assert.assertNull(ep.path);

        ep = Endpoints.parse("BMAS g1.duniter.org 443").orElse(null);
        Assert.assertNotNull(ep);
        Assert.assertEquals(EndpointApi.BMAS.label(), ep.api);
        Assert.assertEquals("g1.duniter.org", ep.dns);
        Assert.assertNotNull(ep.port);
        Assert.assertEquals(443, ep.port.intValue());
        Assert.assertNull(ep.id);
        Assert.assertNull(ep.path);

        ep = Endpoints.parse("WS2P fb17fcd4 g1.duniter.org 443 /ws2p").orElse(null);
        Assert.assertNotNull(ep);
        Assert.assertNotNull("fb17fcd4", ep.id);
        Assert.assertEquals("g1.duniter.org", ep.dns);
        Assert.assertEquals(EndpointApi.WS2P.label(), ep.api);
        Assert.assertNotNull(ep.port);
        Assert.assertEquals(443, ep.port.intValue());
        Assert.assertEquals("/ws2p", ep.path);

        ep = Endpoints.parse("WS2P fb17fcd4 g1.duniter.org 443 ws2p").orElse(null);
        Assert.assertNotNull(ep);
        Assert.assertNotNull(ep.id);
        Assert.assertEquals(EndpointApi.WS2P.label(), ep.api);
        Assert.assertEquals("g1.duniter.org", ep.dns);
        Assert.assertNotNull(ep.port);
        Assert.assertEquals(443, ep.port.intValue());
        Assert.assertEquals("ws2p", ep.path);

        // ws2pId on 7 characters
        ep = Endpoints.parse("WS2P 90e9b12 duniter.g1.1000i100.fr 443 /ws2p").orElse(null);
        Assert.assertNotNull(ep);
        Assert.assertEquals(ep.api, EndpointApi.WS2P.label());
        Assert.assertNotNull(ep.id);
        Assert.assertNotNull(ep.path);

        ep = Endpoints.parse("WS2PTOR 1be86653 3k2zovlpihbt3j3g.onion 20901").orElse(null);
        Assert.assertNotNull(ep);
        Assert.assertNotNull(ep.id);
        Assert.assertNull(ep.path);
        Assert.assertEquals(EndpointApi.WS2PTOR.label(), ep.api);
        Assert.assertEquals("3k2zovlpihbt3j3g.onion", ep.dns);
        Assert.assertNotNull(ep.port);
        Assert.assertEquals(20901, ep.port.intValue());


        // GVA
        ep = Endpoints.parse("GVA S g1.librelois.fr 443 gva").orElse(null);
        Assert.assertNotNull(ep);
        Assert.assertEquals(EndpointApi.GVA.label(), ep.api);
        Assert.assertEquals("g1.librelois.fr", ep.dns);
        Assert.assertNull(ep.ipv4);
        Assert.assertNotNull(ep.port);
        Assert.assertEquals(443, ep.port.intValue());
        Assert.assertNull(ep.id);
        Assert.assertEquals("gva", ep.path);

        // GVA SUB
        ep = Endpoints.parse("GVASUB S g1.librelois.fr 443 gva-sub").orElse(null);
        Assert.assertNotNull(ep);
        Assert.assertEquals(EndpointApi.GVASUB.label(), ep.api);
        Assert.assertEquals("g1.librelois.fr", ep.dns);
        Assert.assertNull(ep.ipv4);
        Assert.assertNotNull(ep.port);
        Assert.assertEquals(443, ep.port.intValue());
        Assert.assertNull(ep.id);
        Assert.assertEquals("gva-sub", ep.path);

        // Any other api
        ep = Endpoints.parse("GCHANGE_API data.gchange.fr 443").orElse(null);
        Assert.assertNotNull(ep);
        Assert.assertEquals(ep.api, "GCHANGE_API");
        Assert.assertNull(ep.id);
        Assert.assertNull(ep.path);

        // Parse Invalid endpoints

        // This must failed (missing port)
        ep = Endpoints.parse("BMAS g1.cgeek.fr").orElse(null);
        Assert.assertNull(ep);

        // This must failed (because bad ID)
        ep = Endpoints.parse("WS2P R8t2sg7w g1.ambau.ovh 443").orElse(null);
        Assert.assertNull(ep);

    }

}

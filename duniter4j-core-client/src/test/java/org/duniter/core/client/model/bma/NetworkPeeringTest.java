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

public class NetworkPeeringTest {

    @Test
    public void toStringTest() throws Exception {


        NetworkPeering peering = new NetworkPeering();
        peering.setCurrency("g1");
        Assert.assertNotNull(peering);

        peering.setCurrency("g1");
        peering.setVersion(new Integer(10));
        peering.setPubkey("38MEAZN68Pz1DTvT3tqgxx4yQP6snJCQhPqEFxbDk4aE");
        peering.setBlock("162694-0000067CAF81B13E4BD7AE72A06F9981D80EB957E4D46C23A67B4DF734E258ED");
        peering.setSignature("U+obPZqDQ3WDDclyCrOhT80Dq/8sPZp0ng+hj4THPAaxKNQwc9cijNnfvwzSsQ/hZBJpZ6+Gzrzso+zprhNICQ==");
        peering.setStatus("UP");

        NetworkPeering.Endpoint epBma = new NetworkPeering.Endpoint();
        epBma.setApi(EndpointApi.BASIC_MERKLED_API.label());
        epBma.setDns("g1.duniter.fr");
        epBma.setPort(80);

        NetworkPeering.Endpoint epWs2p = new NetworkPeering.Endpoint();
        epWs2p.setApi(EndpointApi.WS2P.label());
        epWs2p.setDns("g1.duniter.fr");
        epWs2p.setPath("/ws2p");
        epWs2p.setId("fb17fcd4");
        epWs2p.setPort(443);

        peering.setEndpoints(new NetworkPeering.Endpoint[]{epBma, epWs2p});

        String raw = peering.toString();
        String expedtedRaw = "Version: 10\nType: Peer\nCurrency: g1\nPublicKey: 38MEAZN68Pz1DTvT3tqgxx4yQP6snJCQhPqEFxbDk4aE\nBlock: 162694-0000067CAF81B13E4BD7AE72A06F9981D80EB957E4D46C23A67B4DF734E258ED\nEndpoints:\nBASIC_MERKLED_API g1.duniter.fr 80\nWS2P fb17fcd4 g1.duniter.fr 443 /ws2p\nU+obPZqDQ3WDDclyCrOhT80Dq/8sPZp0ng+hj4THPAaxKNQwc9cijNnfvwzSsQ/hZBJpZ6+Gzrzso+zprhNICQ==\n";

        Assert.assertEquals(expedtedRaw, raw);

    }

}

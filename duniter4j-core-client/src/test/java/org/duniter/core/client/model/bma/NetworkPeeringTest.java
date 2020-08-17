package org.duniter.core.client.model.bma;

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
        epBma.setApi(EndpointApi.BASIC_MERKLED_API.name());
        epBma.setDns("g1.duniter.fr");
        epBma.setPort(80);

        NetworkPeering.Endpoint epWs2p = new NetworkPeering.Endpoint();
        epWs2p.setApi(EndpointApi.WS2P.name());
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

package org.duniter.core.client.model.bma;

import org.junit.Assert;
import org.junit.Test;

public class NetworkPeeringsTest {

    @Test
    public void parse() throws Exception {

        String doc = "Version: 10\nType: Peer\nCurrency: g1\nPublicKey: 38MEAZN68Pz1DTvT3tqgxx4yQP6snJCQhPqEFxbDk4aE\nBlock: 162694-0000067CAF81B13E4BD7AE72A06F9981D80EB957E4D46C23A67B4DF734E258ED\nEndpoints:\nBMAS g1.duniter.fr 443\nES_CORE_API g1.data.duniter.fr 443\nES_USER_API g1.data.duniter.fr 443\nES_SUBSCRIPTION_API g1.data.duniter.fr 443\nBASIC_MERKLED_API g1.duniter.fr 80\nWS2P fb17fcd4 g1.duniter.fr 443 /ws2p\nU+obPZqDQ3WDDclyCrOhT80Dq/8sPZp0ng+hj4THPAaxKNQwc9cijNnfvwzSsQ/hZBJpZ6+Gzrzso+zprhNICQ==\n";

        NetworkPeering peering = NetworkPeerings.parse(doc);
        Assert.assertNotNull(peering);

        Assert.assertEquals("g1", peering.getCurrency());
        Assert.assertEquals("10", peering.getVersion());
        Assert.assertEquals("38MEAZN68Pz1DTvT3tqgxx4yQP6snJCQhPqEFxbDk4aE", peering.getPubkey());
        Assert.assertEquals("162694-0000067CAF81B13E4BD7AE72A06F9981D80EB957E4D46C23A67B4DF734E258ED", peering.getBlock());
        Assert.assertEquals("U+obPZqDQ3WDDclyCrOhT80Dq/8sPZp0ng+hj4THPAaxKNQwc9cijNnfvwzSsQ/hZBJpZ6+Gzrzso+zprhNICQ==", peering.getSignature());
        Assert.assertEquals(6, peering.getEndpoints().length);
    }

}

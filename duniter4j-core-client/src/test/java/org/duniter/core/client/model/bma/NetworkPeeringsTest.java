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

import org.duniter.core.client.TestResource;
import org.duniter.core.client.service.ServiceLocator;
import org.duniter.core.service.CryptoService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

public class NetworkPeeringsTest {

    @ClassRule
    public static final TestResource resource = TestResource.create();

    private CryptoService cryptoService;

    @Before
    public void setUp() {
        cryptoService = ServiceLocator.instance().getCryptoService();
    }

    @Test
    public void parse() throws Exception {

        String unsignedRaw = "Version: 10\nType: Peer\nCurrency: g1\nPublicKey: 38MEAZN68Pz1DTvT3tqgxx4yQP6snJCQhPqEFxbDk4aE\nBlock: 162694-0000067CAF81B13E4BD7AE72A06F9981D80EB957E4D46C23A67B4DF734E258ED\nEndpoints:\nBMAS g1.duniter.fr 443\nES_CORE_API g1.data.duniter.fr 443\nES_USER_API g1.data.duniter.fr 443\nES_SUBSCRIPTION_API g1.data.duniter.fr 443\nBASIC_MERKLED_API g1.duniter.fr 80\nWS2P fb17fcd4 g1.duniter.fr 443 /ws2p\n";
        String signedRaw = unsignedRaw + "U+obPZqDQ3WDDclyCrOhT80Dq/8sPZp0ng+hj4THPAaxKNQwc9cijNnfvwzSsQ/hZBJpZ6+Gzrzso+zprhNICQ==\n";

        NetworkPeering peering = NetworkPeerings.parse(signedRaw);
        Assert.assertNotNull(peering);

        Assert.assertEquals("g1", peering.getCurrency());
        Assert.assertEquals(new Integer(10), peering.getVersion());
        Assert.assertEquals("38MEAZN68Pz1DTvT3tqgxx4yQP6snJCQhPqEFxbDk4aE", peering.getPubkey());
        Assert.assertEquals("162694-0000067CAF81B13E4BD7AE72A06F9981D80EB957E4D46C23A67B4DF734E258ED", peering.getBlock());
        Assert.assertEquals("U+obPZqDQ3WDDclyCrOhT80Dq/8sPZp0ng+hj4THPAaxKNQwc9cijNnfvwzSsQ/hZBJpZ6+Gzrzso+zprhNICQ==", peering.getSignature());
        Assert.assertEquals(6, peering.getEndpoints().length);
        Assert.assertEquals(unsignedRaw, peering.getRaw());
        Assert.assertEquals(signedRaw, peering.toString());
    }

    @Test
    public void parseAndVerify() throws Exception {

        String unsignedRaw = "Version: 10\nType: Peer\nCurrency: g1\nPublicKey: B9BJkrZsXeoWxeUcu8rwz8PtCT67bN2xJ9xJdZNzxTxd\nBlock: 348676-00000032B293BD0B9709239D333D2D904839AD20ADE8840236BDC1732299589F\nEndpoints:\nWS2P 471cffa5 77.152.31.154 20900\n";
        String signature = "Q+/SIISPcOu+i2MqvdbrGdyp2pFAkIShgpAp0sziNZbiETd4WMANwtCb75dn04E4cMmVhpr0aBfa9Pcm/Y++AQ==";
        String signedRaw = unsignedRaw + signature + "\n";

        NetworkPeering peering = NetworkPeerings.parse(signedRaw);
        Assert.assertNotNull(peering);

        Assert.assertEquals("g1", peering.getCurrency());
        Assert.assertEquals(1, peering.getEndpoints().length);
        Assert.assertEquals(unsignedRaw, peering.getRaw());
        Assert.assertEquals(signedRaw, peering.toString());

        Assert.assertEquals(true, cryptoService.verify(unsignedRaw, signature, peering.getPubkey()));
    }
}

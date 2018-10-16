package org.duniter.core.client.model.bma;

import org.junit.Assert;
import org.junit.Test;

public class EndpointsTest {

    @Test
    public void parse() throws Exception {

        NetworkPeering.Endpoint ep = Endpoints.parse("GCHANGE_API data.gchange.fr 443");
        Assert.assertNotNull(ep);

        Assert.assertEquals(ep.api, EndpointApi.GCHANGE_API);
    }

}

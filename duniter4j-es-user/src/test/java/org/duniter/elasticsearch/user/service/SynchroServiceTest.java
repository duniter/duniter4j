package org.duniter.elasticsearch.user.service;

import org.duniter.core.client.model.local.Peer;
import org.duniter.elasticsearch.model.SynchroResult;
import org.duniter.elasticsearch.service.ServiceLocator;
import org.duniter.elasticsearch.synchro.SynchroService;
import org.duniter.elasticsearch.user.TestResource;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by blavenie on 13/09/17.
 */
public class SynchroServiceTest {

    private static final Logger log = LoggerFactory.getLogger(SynchroServiceTest.class);

    @ClassRule
    public static final TestResource resource = TestResource.create();

    private SynchroService service;
    private Peer peer;

    @Before
    public void setUp() throws Exception {
        service = ServiceLocator.instance().getBean(SynchroService.class);
        peer = new Peer.Builder()
                .setHost(resource.getConfiguration().getDataSyncHost())
                .setPort(resource.getConfiguration().getDataSyncPort()).build();

        while(!service.isReady()) {
            Thread.sleep(1000);
        }

        Thread.sleep(5000);
    }

    @Test
    public void synchronizePeer() throws Exception {
        SynchroResult result = service.synchronizePeer(peer, false);
        Assert.assertTrue(result.getInserts() > 0);
    }

    @Test
    @Ignore
    public void startNode() throws Exception {

        while(true) {
            Thread.sleep(10000);
        }
    }
}

package org.duniter.core.client.service.local;

/*
 * #%L
 * Duniter4j :: Core API
 * %%
 * Copyright (C) 2014 - 2015 EIS
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


import com.google.common.collect.ImmutableList;
import org.duniter.core.client.TestResource;
import org.duniter.core.client.config.Configuration;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.client.service.ServiceLocator;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class NetworkServiceTest {

	private static final Logger log = LoggerFactory.getLogger(NetworkServiceTest.class);

	@ClassRule
	public static final TestResource resource = TestResource.create();
	
	private NetworkService service;
	private Peer peer;
	
	@Before
	public void setUp() {
		peer = createTestPeer();
		service = ServiceLocator.instance().getNetworkService();
	}

	@Test
	public void getPeers() throws Exception {

		List<Peer> peers = service.getPeers(peer);

		Assert.assertNotNull(peers);
		Assert.assertTrue(peers.size() > 0);

		peers.forEach(p -> log.debug(" Found peer: " + p.toString()));
	}

	@Test
	@Ignore
	public void getGchangePeers() throws Exception {

		Peer gchangePeer = Peer.builder()
				//.setHost("data.gchange.fr")
				.host("gchange.data.presles.fr")
				.port(443)
				.api("GCHANGE_API")
				.build();

		NetworkService.Filter filterDef = new NetworkService.Filter();
		filterDef.filterStatus = Peer.PeerStatus.UP;
		filterDef.filterEndpoints = ImmutableList.of("GCHANGE_API");

		List<Peer> peers = service.getPeers(gchangePeer, filterDef, null);

		Assert.assertNotNull(peers);
		Assert.assertTrue(peers.size() > 0);


		peers.forEach(p -> log.debug(" Found peer: " + p.toString()));

		for (Peer peer: peers) {
			Assert.assertNotNull(peer.getPeering());
			Assert.assertNotNull(peer.getPeering().getRaw());
		}

	}

	/* -- internal methods */

    protected Peer createTestPeer() {
        return Peer.builder()
				.host(Configuration.instance().getNodeHost())
				.port(Configuration.instance().getNodePort())
				.build();
    }
}

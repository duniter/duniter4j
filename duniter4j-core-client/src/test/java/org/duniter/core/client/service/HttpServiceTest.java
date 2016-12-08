package org.duniter.core.client.service;

/*
 * #%L
 * UCoin Java Client :: Core API
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


import org.duniter.core.client.TestResource;
import org.duniter.core.client.config.Configuration;
import org.duniter.core.client.model.bma.EndpointProtocol;
import org.duniter.core.client.model.bma.NetworkPeering;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.client.service.bma.NetworkRemoteService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class HttpServiceTest {

	private static final Logger log = LoggerFactory.getLogger(HttpServiceTest.class);

	@ClassRule
	public static final TestResource resource = TestResource.create();
	
	private HttpService service;
	private Peer peer;
	
	@Before
	public void setUp() {
		service = ServiceLocator.instance().getHttpService();
		peer = createTestPeer();
	}

	@Test
	public void connect() throws Exception {

		service.connect(peer);
	}


	/* -- internal methods */

    protected Peer createTestPeer() {
        Peer peer = new Peer(
                Configuration.instance().getNodeHost(),
                Configuration.instance().getNodePort());

        return peer;
    }
}

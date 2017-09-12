package org.duniter.elasticsearch.service;

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


import com.fasterxml.jackson.databind.ObjectMapper;
import org.duniter.core.client.config.Configuration;
import org.duniter.core.client.model.bma.BlockchainBlock;
import org.duniter.core.client.model.bma.jackson.JacksonUtils;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.client.service.bma.BlockchainRemoteService;
import org.duniter.elasticsearch.TestResource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class BlockchainServiceTest {

	private static final Logger log = LoggerFactory.getLogger(BlockchainServiceTest.class);

	@ClassRule
	public static final TestResource resource = TestResource.create();

    private BlockchainService service;
    private BlockchainRemoteService remoteService;
    private Configuration config;
    private Peer peer;

    @Before
    public void setUp() throws Exception {
        service = ServiceLocator.instance().getBean(BlockchainService.class);
        remoteService = ServiceLocator.instance().getBlockchainRemoteService();
        config = Configuration.instance();
        peer = Peer.newBuilder().setHost(config.getNodeHost()).setPort(config.getNodePort()).build();

        // Init the currency
        CurrencyService currencyService = ServiceLocator.instance().getBean(CurrencyService.class);
        currencyService.createIndexIfNotExists()
                .indexCurrencyFromPeer(peer);
    }

    @Test
    public void indexLastBlocks() {
        service.indexLastBlocks(peer);
    }

    @Test
    public void indexBlock() throws Exception {
        BlockchainBlock current = remoteService.getCurrentBlock(peer);
        service.indexCurrentBlock(current, true/*wait*/);

        try {
            String blockStr = JacksonUtils.getThreadObjectMapper().writeValueAsString(current);

            service.indexBlockFromJson(peer, blockStr, true/*is current*/, false/*detected fork*/, true/*wait*/);
        }
        catch(Exception e) {
            Assert.fail(e.getMessage());
        }

        Thread.sleep(1000);

        // Try to get the indexed block
        BlockchainBlock retrievedBlock = service.getBlockById(current.getCurrency(), current.getNumber());
        Assert.assertNotNull(retrievedBlock);

    }

	/* -- internal methods */

}

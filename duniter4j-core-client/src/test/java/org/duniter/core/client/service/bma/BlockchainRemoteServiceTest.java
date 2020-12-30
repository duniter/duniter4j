package org.duniter.core.client.service.bma;

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


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.duniter.core.client.TestResource;
import org.duniter.core.client.config.Configuration;
import org.duniter.core.client.model.bma.BlockchainBlock;
import org.duniter.core.client.model.bma.BlockchainDifficulties;
import org.duniter.core.client.model.bma.BlockchainParameters;
import org.duniter.core.client.model.bma.ErrorCode;
import org.duniter.core.client.model.bma.jackson.JacksonUtils;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.client.model.local.Wallet;
import org.duniter.core.client.service.ServiceLocator;
import org.duniter.core.client.service.exception.HttpBadRequestException;
import org.duniter.core.util.crypto.CryptoUtils;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class BlockchainRemoteServiceTest {

    private static final Logger log = LoggerFactory.getLogger(BlockchainRemoteServiceTest.class);
    
    @ClassRule
    public static final TestResource resource = TestResource.create();

    private BlockchainRemoteService service;

    private boolean isWebSocketNewBlockReceived;

    @Before
    public void setUp() {
        service = ServiceLocator.instance().getBlockchainRemoteService();
        isWebSocketNewBlockReceived = false;
    }

    @Test
    public void getParameters() throws Exception {

        BlockchainParameters result = service.getParameters(createTestPeer());

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getCurrency());

        Assert.assertEquals(resource.getFixtures().getCurrency(), result.getCurrency());
    }
    
    @Test
    public void getBlock() throws Exception {

        BlockchainBlock result = service.getBlock(createTestPeer(), 0);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getCurrency());
        
        for (BlockchainBlock.Identity id: result.getIdentities()) {
            Assert.assertNotNull(id.getUserId());
        }
        
        for (BlockchainBlock.Joiner id: result.getJoiners()) {
            Assert.assertNotNull(id.getUserId());
        }
    }

    @Test
    // @FIXME timeout trop court
    public void getBlockWithTx() throws Exception {

        long[] blocks = service.getBlocksWithTx(createTestPeer());
        if (blocks == null) return;

        // Check first block with TX
        BlockchainBlock result = service.getBlock(createTestPeer(), blocks[0]);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getTransactions());
        Assert.assertTrue(result.getTransactions().length > 0);

        // Check last block with TX
        result = service.getBlock(createTestPeer(), blocks[blocks.length-1]);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getTransactions());
        Assert.assertTrue(result.getTransactions().length > 0);
    }

    @Test
    public void getBlocksAsJson() throws Exception {

        String[] result= service.getBlocksAsJson(createTestPeer(), 10, 0);

        Assert.assertNotNull(result);
        Assert.assertEquals(10, result.length);

        // Make sure allOfToList json are valid blocks
        ObjectMapper objectMapper = JacksonUtils.getThreadObjectMapper();

        int number = 0;
        for (String jsonBlock: result) {
            try {
                objectMapper.readValue(jsonBlock, BlockchainBlock.class);
            }
            catch(JsonProcessingException e) {
                e.printStackTrace();
                Assert.fail(String.format("Invalid block format #%s. See previous error", number));
            }
            number++;
        }

    }

    @Test
    public void getLastUD() throws Exception {
        Peer peer = createTestPeer();

        // Get the last UD
        BlockchainRemoteService blockchainRemoteService = ServiceLocator.instance().getBlockchainRemoteService();
        long lastUD = blockchainRemoteService.getLastUD(peer);
    }


    @Test
    public void addNewBlockListener() throws Exception {

        isWebSocketNewBlockReceived = false;

        service.addBlockListener(createTestPeer(), (message) -> {
            try {
                BlockchainBlock block = JacksonUtils.getThreadObjectMapper().readValue(message, BlockchainBlock.class);
                log.debug("Received block #" + block.getNumber());
                isWebSocketNewBlockReceived = true;
            }
            catch (IOException e) {
                Assert.fail(e.getMessage());
            }
        }, false/*autoReconnect*/);

        int count = 0;
        while(!isWebSocketNewBlockReceived) {
            Thread.sleep(1000); // wait 1s
            count++;
            Assert.assertTrue("No block received from WebSocket, after 10s", count<10);
        }
    }


    @Test
    public void requestMembership() throws Exception {
        Peer peer = createTestPeer();
        Wallet wallet = createTestWallet();
        String uid = resource.getFixtures().getUid();
        String currency = resource.getFixtures().getCurrency();
        String selfIdentityBlockUid = resource.getFixtures().getSelfIdentityBlockUid();

        // Get the block UID
        BlockchainRemoteService blockchainRemoteService = ServiceLocator.instance().getBlockchainRemoteService();
        BlockchainBlock currentBlock = blockchainRemoteService.getCurrentBlock(peer);
        Assume.assumeNotNull(currentBlock);
        String blockUid = currentBlock.getNumber() + "-" + currentBlock.getHash();

        try {
            service.requestMembership(peer, currency, wallet.getPubKey(), wallet.getSecKey(), uid, blockUid, selfIdentityBlockUid);
        }
        catch(HttpBadRequestException e) {
            Assert.assertEquals(ErrorCode.MEMBERSHRIP_ALREADY_SEND, e.getCode());

        }
    }

    @Test
    public void getDifficulties() {
        Peer peer = createTestPeer();

        BlockchainDifficulties result = service.getDifficulties(peer);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getBlock());

        Assert.assertNotNull(result.getLevels());
        Assert.assertTrue(result.getLevels().length > 0);
    }

    /* -- Internal methods -- */

    protected Peer createTestPeer() {
        return Peer.newBuilder()
                .setHost(Configuration.instance().getNodeHost())
                .setPort(Configuration.instance().getNodePort())
                .build();
    }

    protected Wallet createTestWallet() {
        Wallet wallet = new Wallet(
                resource.getFixtures().getCurrency(),
                resource.getFixtures().getUid(),
                CryptoUtils.decodeBase58(resource.getFixtures().getUserPublicKey()),
                CryptoUtils.decodeBase58(resource.getFixtures().getUserSecretKey()));

        return wallet;
    }
}

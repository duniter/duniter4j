package io.ucoin.ucoinj.core.client.service.bma;

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


import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import io.ucoin.ucoinj.core.client.TestResource;
import io.ucoin.ucoinj.core.client.config.Configuration;
import io.ucoin.ucoinj.core.client.model.bma.BlockchainBlock;
import io.ucoin.ucoinj.core.client.model.bma.BlockchainParameters;
import io.ucoin.ucoinj.core.client.model.bma.ErrorCode;
import io.ucoin.ucoinj.core.client.model.bma.gson.GsonUtils;
import io.ucoin.ucoinj.core.client.model.local.Peer;
import io.ucoin.ucoinj.core.client.model.local.Wallet;
import io.ucoin.ucoinj.core.client.service.ServiceLocator;
import io.ucoin.ucoinj.core.client.service.exception.HttpBadRequestException;
import io.ucoin.ucoinj.core.util.crypto.CryptoUtils;
import io.ucoin.ucoinj.core.util.websocket.WebsocketClientEndpoint;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public void getBlocksAsJson() throws Exception {

        String[] result= service.getBlocksAsJson(createTestPeer(), 10, 0);

        Assert.assertNotNull(result);
        Assert.assertEquals(10, result.length);

        // Make sure all json are valid blocks
        Gson gson = GsonUtils.newBuilder().create();
        int number = 0;
        for (String jsonBlock: result) {
            try {
                gson.fromJson(jsonBlock, BlockchainBlock.class);
            }
            catch(JsonSyntaxException e) {
                e.printStackTrace();
                Assert.fail(String.format("Invalid block format #%s. See previous error", number));
            }
            number++;
        }

    }

    @Test
    public void addNewBlockListener() throws Exception {

        isWebSocketNewBlockReceived = false;

        service.addNewBlockListener(createTestPeer(), new WebsocketClientEndpoint.MessageHandler() {
            @Override
            public void handleMessage(String message) {
                BlockchainBlock block = GsonUtils.newBuilder().create().fromJson(message, BlockchainBlock.class);
                log.debug("Received block #" + block.getNumber());
                isWebSocketNewBlockReceived = true;
            }
        });

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

    /* -- Internal methods -- */

    protected Peer createTestPeer() {
        Peer peer = new Peer(
                Configuration.instance().getNodeHost(),
                Configuration.instance().getNodePort());

        return peer;
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

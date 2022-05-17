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


import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.duniter.core.client.TestResource;
import org.duniter.core.client.config.Configuration;
import org.duniter.core.client.model.bma.TxSource;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.client.model.local.Wallet;
import org.duniter.core.client.service.ServiceLocator;
import org.duniter.core.client.service.exception.InsufficientCreditException;
import org.duniter.core.util.crypto.CryptoUtils;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

@Slf4j
public class TransactionRemoteServiceTest {

	@ClassRule
	public static final TestResource resource = TestResource.create();
	
	private TransactionRemoteService service;
	
	@Before
	public void setUp() {
		service = ServiceLocator.instance().getTransactionRemoteService();

		// Make sure fixtures has not been changed
		Assume.assumeTrue(
				"Invalid currencies in test fixtures",
				Objects.equals(resource.getFixtures().getDefaultCurrency(), resource.getFixtures().getCurrency()));
	}

	@Test
	public void transfer() throws Exception {

		try {
			service.transfer(
					createTestWallet(),
					resource.getFixtures().getOtherUserPublicKey(0),
					1,
					"my comments" + System.currentTimeMillis());
		} catch (InsufficientCreditException e) {
			// OK continue
		}
	}

	@Test
	public void transferMulti() throws Exception {

		Map<String, Long> destPubkeyAmount = ImmutableMap.<String, Long>builder()
			.put(resource.getFixtures().getOtherUserPublicKey(0), 1l)
			.put(resource.getFixtures().getOtherUserPublicKey(1), 2l)
			.build();

		try {
			service.transfer(
				createTestWallet(),
				destPubkeyAmount,
				"my comments" + System.currentTimeMillis());
		} catch (InsufficientCreditException e) {
			// OK continue
		}
	}
	
	@Test
	public void getSources() throws Exception {

		String pubKey = resource.getFixtures().getUserPublicKey();
        Peer peer = createTestPeer();
		
		TxSource sourceResults = service.getSources(peer, pubKey);

		Assert.assertNotNull(sourceResults);
		Assert.assertNotNull(sourceResults.getSources());
		Assert.assertEquals(resource.getFixtures().getCurrency(), sourceResults.getCurrency());
		Assert.assertEquals(pubKey, sourceResults.getPubkey());

        long credit = service.computeCredit(sourceResults.getSources());

        Assert.assertTrue(credit >= 0d);
	}

	/* -- internal methods */

	protected Wallet createTestWallet() {
		Wallet wallet = new Wallet(
				resource.getFixtures().getCurrency(),
				resource.getFixtures().getUid(),
				CryptoUtils.decodeBase58(resource.getFixtures().getUserPublicKey()),
				CryptoUtils.decodeBase58(resource.getFixtures().getUserSecretKey()));

        wallet.setCurrency(resource.getFixtures().getDefaultCurrency());

		return wallet;
	}

    protected Peer createTestPeer() {
		return Peer.builder()
				.host(Configuration.instance().getNodeHost())
				.port(Configuration.instance().getNodePort())
				.build();
    }
}

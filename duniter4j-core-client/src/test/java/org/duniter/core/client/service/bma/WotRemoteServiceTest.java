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


import org.apache.commons.lang3.mutable.MutableInt;
import org.duniter.core.client.TestResource;
import org.duniter.core.client.config.Configuration;
import org.duniter.core.client.model.bma.*;
import org.duniter.core.client.model.local.Identity;
import org.duniter.core.client.model.local.Member;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.client.model.local.Wallet;
import org.duniter.core.client.service.ServiceLocator;
import org.duniter.core.client.service.exception.HttpBadRequestException;
import org.duniter.core.util.CollectionUtils;
import org.duniter.core.util.crypto.CryptoUtils;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WotRemoteServiceTest {

	private static final Logger log = LoggerFactory.getLogger(WotRemoteServiceTest.class);

	@ClassRule
	public static final TestResource resource = TestResource.create();

	private WotRemoteService service;
	private Peer peer;

	@Before
	public void setUp() {
		service = ServiceLocator.instance().getWotRemoteService();
		peer = createTestPeer();
	}

	@Test
	public void getIdentity() throws Exception {
        Set<String> currencyIds = new HashSet<>();
        currencyIds.add(resource.getFixtures().getDefaultCurrency());
		List<Identity> result = service
				.findIdentities(currencyIds, resource.getFixtures().getUid());
		Assert.assertNotNull(result);
        Assert.assertTrue(result.size() > 0);
	}

	@Test
	public void findByUid() throws Exception {
		WotLookup.Uid result = service
				.findByUid(resource.getFixtures().getDefaultCurrency(),
						resource.getFixtures().getUid());
		Assert.assertNotNull(result);
	}

	@Test
	@Ignore
	// FIXME: user 'gab' has no certification
	public void getCertifiedBy() throws Exception {
		WotRemoteService service = ServiceLocator.instance().getWotRemoteService();
		WotCertification result = service.getCertifiedBy(
				resource.getFixtures().getDefaultCurrency(),
				resource.getFixtures().getUid());

		Assert.assertNotNull(result);
		Assert.assertNotNull(result.getUid());
		Assert.assertNotNull(result.getPubkey());

		Assert.assertTrue(
				String.format(
						"Test user (uid=%s) should have some certifications return by %s",
						resource.getFixtures().getUid(),
						"certified-by"), CollectionUtils
						.isNotEmpty(result.getCertifications()));

		for (WotCertification.Certification cert : result.getCertifications()) {
			Assert.assertNotNull(cert.getUid());

			WotCertification.CertTime certTime = cert.getCertTime();
			Assert.assertNotNull(certTime);
			Assert.assertTrue(certTime.getBlock() >= 0);
			Assert.assertNotNull(certTime.getMedianTime() >= 0);
		}
	}

	@Test
	@Ignore
	// FIXME: user 'gab' has no certification
	public void getCertifiersOf() throws Exception {
		WotCertification result = service.getCertifiersOf(
				resource.getFixtures().getDefaultCurrency(),
				resource.getFixtures().getUid());

		Assert.assertNotNull(result);
		Assert.assertNotNull(result.getUid());
		Assert.assertNotNull(result.getPubkey());

		Assert.assertTrue(
				String.format(
						"Test user (uid=%s) should have some certifications return by %s",
						resource.getFixtures().getUid(),
						"certifiers-of"),
                CollectionUtils.isNotEmpty(result.getCertifications()));

		for (WotCertification.Certification cert : result.getCertifications()) {
			Assert.assertNotNull(cert.getUid());

			WotCertification.CertTime certTime = cert.getCertTime();
			Assert.assertNotNull(certTime);
			Assert.assertTrue(certTime.getBlock() >= 0);
			Assert.assertNotNull(certTime.getMedianTime() >= 0);
		}
	}

	@Test
	public void sendIdentity() throws Exception {
        Peer peer = createTestPeer();
        Wallet wallet = createTestWallet();
        String uid = resource.getFixtures().getUid();
        String currency = resource.getFixtures().getCurrency();

        // Get the block UID
        BlockchainRemoteService blockchainRemoteService = ServiceLocator.instance().getBlockchainRemoteService();
        BlockchainBlock currentBlock = blockchainRemoteService.getCurrentBlock(peer);
        Assume.assumeNotNull(currentBlock);
        String blockUid = currentBlock.getNumber() + "-" + currentBlock.getHash();

        try {
            service.sendIdentity(peer, currency, wallet.getPubKey(), wallet.getSecKey(), uid, blockUid);
        }
        catch(HttpBadRequestException e) {
            Assert.assertTrue(ErrorCode.UID_ALREADY_USED == e.getCode() || ErrorCode.ALREADY_UP_TO_DATE == e.getCode());
        }
	}

	@Test
	public void sendCertification()  {
		Peer peer = createTestPeer();
		Wallet wallet = createTestWallet();
		WotRemoteService service = ServiceLocator.instance().getWotRemoteService();

		Identity result = service.getIdentity(peer, "kimamila", "5ocqzyDMMWf1V8bsoNhWb1iNwax1e9M7VTUN6navs8of");
		Assert.assertNotNull(result);
		Assert.assertNotNull(result.getUid());
		Assert.assertNotNull(result.getPubkey());

		try {
			service.sendCertification(wallet, result);
		} catch (BmaTechnicalException e) {
			// Test user is not a member: an 1002 should be return
			Assert.assertTrue(e.getCode() == 1002);
		}
	}

	@Test
	public void getMembers() {
		List<Member> result = service.getMembers(peer);
		Assert.assertNotNull(result);
		Assert.assertTrue(result.size() > 0);
	}

	@Test
	public void getPendingMemberships() {
		List<WotPendingMembership> result = service.getPendingMemberships(peer);
		Assert.assertNotNull(result);
		Assert.assertTrue(result.size() > 0);
	}

	@Test
	public void getRequirements() {
		List<WotPendingMembership> pendingMemberships = service.getPendingMemberships(peer);
		Assume.assumeTrue(CollectionUtils.isNotEmpty(pendingMemberships));

		MutableInt counter = new MutableInt(0);
		pendingMemberships.stream()
				// Get first 10
			.filter(ms -> {
				counter.increment();
				return counter.getValue() < 10;
			})
			.forEach(ms -> {
				List<WotRequirements> result = service.getRequirements(peer, ms.getPubkey());
				Assert.assertNotNull(result);
				Assert.assertNotNull(result.size() > 0);
			});
	}

	@Test
	public void getRequirementsByPubkey() {
		List<WotPendingMembership> pendingMemberships = service.getPendingMemberships(peer);
		Assume.assumeTrue(CollectionUtils.isNotEmpty(pendingMemberships));

		MutableInt counter = new MutableInt(0);
		pendingMemberships.stream()
			// Get first 10
			.filter(ms -> {
				counter.increment();
				return counter.getValue() < 10;
			})
			.forEach(ms -> {
				List<WotRequirements> result = service.getRequirementsByPubkey(peer, ms.getPubkey());
				Assert.assertNotNull(result);
				Assert.assertNotNull(result.size() > 0);
			});
	}


	/* -- internal methods */

	protected Wallet createTestWallet() {
		Wallet wallet = new Wallet(
				resource.getFixtures().getCurrency(),
				resource.getFixtures().getUid(),
				CryptoUtils.decodeBase58(resource.getFixtures().getUserPublicKey()),
				CryptoUtils.decodeBase58(resource.getFixtures().getUserSecretKey()));

		return wallet;
	}

	protected Peer createTestPeer() {
		return Peer.builder()
				.host(Configuration.instance().getNodeHost())
				.port(Configuration.instance().getNodePort())
				.build();
	}
}

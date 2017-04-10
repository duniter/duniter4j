package org.duniter.core.service;

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


import com.google.common.primitives.UnsignedBytes;
import org.abstractj.kalium.NaCl;
import org.abstractj.kalium.crypto.Box;
import org.abstractj.kalium.crypto.Util;
import org.duniter.core.test.TestFixtures;
import org.duniter.core.util.crypto.Base58;
import org.duniter.core.util.crypto.CryptoUtils;
import org.duniter.core.util.crypto.SecretBox;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import static org.abstractj.kalium.NaCl.Sodium.CRYPTO_BOX_CURVE25519XSALSA20POLY1305_ZEROBYTES;
import static org.abstractj.kalium.NaCl.Sodium.CRYPTO_SECRETBOX_XSALSA20POLY1305_NONCEBYTES;
import static org.abstractj.kalium.crypto.Util.checkLength;
import static org.abstractj.kalium.crypto.Util.isValid;

public class Ed25519CryptoServiceTest {

    private String message;
    private byte[] messageAsBytes;
    private CryptoService service;
    private TestFixtures fixtures;

    @Before
    public void setUp() throws UnsupportedEncodingException {
        message = "my message to encrypt !";
        messageAsBytes = message.getBytes("UTF-8");
        service = new Ed25519CryptoServiceImpl();
        fixtures = new TestFixtures();
    }

    @Test
    public void sign() throws Exception {
        SecretBox secretBox = createSecretBox();

        String signature = service.sign(message, Base58.decode(secretBox.getSecretKey()));

        Assert.assertEquals("aAxVThibiZGbpJWrFo8MzZe8RDIoJ1gMC1UIr0utDBQilG44PjA/7o+pOoPAOXgDE3sosGeLHTw1Q/RhFBa4CA==", signature);
    }

	@Test
	public void verify() throws Exception {

        SecretBox secretBox = createSecretBox();

        String signature = service.sign(message, Base58.decode(secretBox.getSecretKey()));

        boolean validSignature = service.verify(message, signature, secretBox.getPublicKey());
        Assert.assertTrue(validSignature);
	}

    @Test
    public void hash() throws Exception {

        String record = "{\"isCompany\":false,\"title\":\"toto\",\"description\":\"toto\",\"pictures\":[],\"time\":1461162142,\"issuer\":\"G2CBgZBPLe6FSFUgpx2Jf1Aqsgta6iib3vmDRA1yLiqU\"}";

        String hash = service.hash(record);

        Assert.assertEquals("AC31F1E3EAEB7A535A3BF1182AA53100BB3B610C5D63643ACB145EB99764B0CA", hash);
    }

    @Test
    public void packThenOpenBox() throws Exception {

        //
        String originalMessage = "test@test";
        String nonce = "AHHfny8igAJp1h7P5d8bEobKZfgoRcXs9";

        // Sender
        SecretBox receiver = createSecretBox();
        SecretBox sender = createSecretBox();

        // Create box
        String cypherText = service.box(originalMessage,
                CryptoUtils.decodeBase58(nonce),
                receiver.getSecretKey(), sender.getPublicKey());

        // Open box
        String decryptedText = service.openBox(
                cypherText,
                nonce,
                sender.getPublicKey(), receiver.getSecretKey());

        Assert.assertEquals(originalMessage, decryptedText);

    }

    @Test
    public void openBox() throws Exception {

        // Receiver & Receiver
        SecretBox receiver = createSecretBox();
        SecretBox sender = createSecretBox();

        // Open box
        String decryptedText = service.openBox(
                "RZwBeUpjGH2f4GUP0UW32WC82iiWfcrKbw==",
                "AHHfny8igAJp1h7P5d8bEobKZfgoRcXs9",
                sender.getPublicKey(), receiver.getSecretKey());

        Assert.assertEquals("test@test", decryptedText);
    }

	/* -- internal methods */

	protected SecretBox createSecretBox() {
		String salt = fixtures.getUserSalt();
		String password = fixtures.getUserPassword();
		SecretBox secretBox = new SecretBox(salt, password);

		return secretBox;
	}
}

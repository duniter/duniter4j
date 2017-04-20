package org.duniter.core.service;

import org.duniter.core.model.SmtpConfig;
import org.duniter.core.test.TestFixtures;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

/**
 * Created by blavenie on 20/04/17.
 */
public class MailServiceTest {

    private MailService  service;

    @Before
    public void setUp() throws UnsupportedEncodingException {
        service = new MailServiceImpl();
        SmtpConfig config = new SmtpConfig();
        config.setSenderName("test");
        config.setSenderAddress("no-reply@duniter.fr");
        config.setSmtpHost("localhost");
        config.setSmtpPort(25);
        service.setSmtpConfig(config);
    }

    @Test
    @Ignore
    public void sendTextEmail() {
        service.sendTextEmail("Test " + System.currentTimeMillis(),
                "a test content",
                "root@localhost");
    }
}

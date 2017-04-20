package org.duniter.core.service;

/*-
 * #%L
 * Duniter4j :: Core Shared
 * %%
 * Copyright (C) 2014 - 2017 EIS
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

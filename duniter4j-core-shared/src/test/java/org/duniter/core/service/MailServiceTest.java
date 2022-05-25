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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.duniter.core.model.SmtpConfig;
import org.duniter.core.test.TestFixtures;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Created by blavenie on 20/04/17.
 */
public class MailServiceTest {

    private MailService  service;

    @Before
    public void setUp() throws UnsupportedEncodingException {
        service = new MailServiceImpl();

        service.setSmtpConfig(SmtpConfig.builder()
            .senderName("test")
            .senderAddress("no-reply@duniter.fr")
            .smtpHost("localhost")
            .smtpPort(25)
            .build());
    }

    @Test
    @Ignore
    public void sendTextEmail() {
        service.sendTextEmail("Test " + System.currentTimeMillis(),
                "a test content",
                "root@localhost");
    }

    @Test
    @Ignore
    public void sendHtmlEmailWithText() throws IOException {

        service.sendHtmlEmailWithText("Test " + System.currentTimeMillis() + " with HTML",
            "a test text content",
            "<b>a test HTML</b> content.",
            "benoit.lavenier@e-is.pro");

        service.sendHtmlEmailWithText("Test " + System.currentTimeMillis() + " with HTML and Image",
            "a test text content",
            "<b>a test HTML</b> content.<br/><img src=\"https://demo.cesium.app/img/logo_128px.png\">",
            "benoit.lavenier@e-is.pro");

        // Send file attachments
        {
            File temp = new File("/tmp/mail-attachment.csv");
            FileUtils.write(temp, "col1;col2\nval1;val2", StandardCharsets.UTF_8);

            service.sendHtmlEmailWithText("Test " + System.currentTimeMillis() + " with HTML and Image + attachment",
                "a test text content",
                "<b>a test HTML</b> content.<br/><img src=\"https://demo.cesium.app/img/logo_128px.png\">",
                Lists.newArrayList(
                    new URL("https://demo.cesium.app/img/logo_128px.png"),
                    new URL("file://" + temp.getAbsolutePath())
                ),
                "benoit.lavenier@e-is.pro");
        }
    }
}

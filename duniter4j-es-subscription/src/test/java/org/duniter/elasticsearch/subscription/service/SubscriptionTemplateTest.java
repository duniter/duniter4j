package org.duniter.elasticsearch.subscription.service;

/*
 * #%L
 * UCoin Java Client :: ElasticSearch Indexer
 * %%
 * Copyright (C) 2014 - 2016 EIS
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

import org.duniter.core.client.model.ModelUtils;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.test.TestResource;
import org.duniter.elasticsearch.subscription.util.stringtemplate.DateRenderer;
import org.duniter.elasticsearch.subscription.util.stringtemplate.I18nRenderer;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupDir;
import org.stringtemplate.v4.StringRenderer;

import java.io.File;
import java.io.FileWriter;
import java.util.Date;

import static org.junit.Assert.assertNotNull;

/**
 * Created by Benoit on 06/05/2015.
 */
public class SubscriptionTemplateTest {
    private static final Logger log = LoggerFactory.getLogger(SubscriptionTemplateTest.class);

    private static final boolean verbose = true;

    //@ClassRule
    public static final TestResource resource = TestResource.create();

    @Test
    public void testHtmlEmail() throws Exception{

        try {
            STGroup group = new STGroupDir("templates", '$', '$');

            group.registerRenderer(Date.class, new DateRenderer());
            group.registerRenderer(String.class, new StringRenderer());
            group.registerRenderer(String.class, new I18nRenderer());

            ST contentEmail = group.getInstanceOf("html_email_content");
            contentEmail.add("issuer", "MyIssuerName");
            contentEmail.add("url", "https://g1.duniter.fr");
            contentEmail.add("senderPubkey", "G2CBgZBPLe6FSFUgpx2Jf1Aqsgta6iib3vmDRA1yLiqU");
            contentEmail.add("senderName", ModelUtils.minifyPubkey("G2CBgZBPLe6FSFUgpx2Jf1Aqsgta6iib3vmDRA1yLiqU"));
            contentEmail.addAggr("events.{description, time}", new Object[]{"My event description", new Date()});
            assertNotNull(contentEmail);

            ST css_logo = group.getInstanceOf("css_logo");
            assertNotNull(css_logo);

            ST htmlTpl = group.getInstanceOf("html");
            assertNotNull(htmlTpl);
            htmlTpl.add("content", contentEmail.render());
            htmlTpl.add("useCss", "true");
            String html = htmlTpl.render();

            if (verbose) {
                System.out.println(html);
            }

            //FileWriter fw = new FileWriter(new File(resource.getResourceDirectory("out"), "page.html"));
            FileWriter fw = new FileWriter(new File("/home/blavenie/git/duniter4j/duniter4j-es-subscription/src/test/resources/test2.html"));
            fw.write(html);
            fw.flush();
            fw.close();


        }
        catch (Exception e) {
            throw new TechnicalException(e);
        }
    }

    @Test
    public void testTextEmail() throws Exception{

        try {
            STGroup group = new STGroupDir("templates", '$', '$');

            group.registerRenderer(Date.class, new DateRenderer());
            group.registerRenderer(String.class, new StringRenderer());
            group.registerRenderer(String.class, new I18nRenderer());

            ST tpl = group.getInstanceOf("text_email");
            tpl.add("issuer", "MyIssuerName");
            tpl.add("url", "https://g1.duniter.fr");
            tpl.add("senderPubkey", "G2CBgZBPLe6FSFUgpx2Jf1Aqsgta6iib3vmDRA1yLiqU");
            tpl.add("senderName", ModelUtils.minifyPubkey("G2CBgZBPLe6FSFUgpx2Jf1Aqsgta6iib3vmDRA1yLiqU"));
            tpl.addAggr("events.{description, time}", new Object[]{"My event description", new Date()});
            assertNotNull(tpl);

            String text = tpl.render();

            if (verbose) {
                System.out.println(text);
            }

        }
        catch (Exception e) {
            throw new TechnicalException(e);
        }
    }
}


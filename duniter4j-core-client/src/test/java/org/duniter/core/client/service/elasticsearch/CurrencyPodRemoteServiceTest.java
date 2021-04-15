package org.duniter.core.client.service.elasticsearch;

/*
 * #%L
 * Duniter4j :: ElasticSearch Indexer
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

import org.duniter.core.client.TestResource;
import org.duniter.core.client.config.Configuration;
import org.duniter.core.client.service.ServiceLocator;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Benoit on 06/05/2015.
 */
public class CurrencyPodRemoteServiceTest {
    private static final Logger log = LoggerFactory.getLogger(CurrencyPodRemoteServiceTest.class);

    @ClassRule
    public static final TestResource resource = TestResource.create();

    private CurrencyPodRemoteService service;
    private Configuration config;

    @Before
    public void setUp() {
        service = ServiceLocator.instance().getCurrencyPodRemoteService();
        config = Configuration.instance();

        // Make sure ES node is alive
        if (!service.isNodeAlive()) {
            log.warn(String.format("Unable to connect to elasticsearch node [%s:%s]. Skipping test.",
                    config.getNodeElasticSearchHost(),
                    config.getNodeElasticSearchPort()));
            Assume.assumeTrue(false);
        }
    }

    @Test
    public void isNodeAlive() {
        boolean isNodeAlive = service.isNodeAlive();
        Assert.assertTrue(isNodeAlive);
    }

}

package org.duniter.core.client.example;

/*-
 * #%L
 * Duniter4j :: Core Client API
 * %%
 * Copyright (C) 2014 - 2017 Duniter Team
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

import org.duniter.core.client.config.Configuration;
import org.duniter.core.client.model.bma.BlockchainBlock;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.client.service.ServiceLocator;

public class Example1 {

    public static void main(String[] args) {

        // Init configuration
        String configFilename = "duniter4j-config.properties";
        Configuration config = new Configuration(configFilename, args);
        Configuration.setInstance(config);


        // Initialize service locator
        ServiceLocator.instance().init();

        // Create a peer, from configuration
        Peer aPeer = Peer.builder()
                .host(config.getNodeHost())
                .port(config.getNodePort())
                .build();

        // Do something fun !
        BlockchainBlock currentBlock = ServiceLocator.instance().getBlockchainRemoteService().getCurrentBlock(aPeer);
        System.out.println(String.format("Hello %s world !", currentBlock.getCurrency()));
    }
}

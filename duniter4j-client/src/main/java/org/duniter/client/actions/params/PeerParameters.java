package org.duniter.client.actions.params;

/*
 * #%L
 * Duniter4j :: Client
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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.duniter.core.client.config.Configuration;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.util.Preconditions;
import org.duniter.core.util.StringUtils;
import org.nuiton.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by blavenie on 22/03/17.
 */
public class PeerParameters {

    private static Logger log = LoggerFactory.getLogger(PeerParameters.class);

    @Parameter(names = {"-p", "--peer"}, description = "Peer address (use format: 'host:port')", descriptionKey = "duniter4j.client.params.peer")
    public String peerStr;

    @Parameter(names = "--ssl", description = "Using SSL connection to node", descriptionKey = "duniter4j.client.params.peer.useSsl")
    public boolean useSsl = false;

    @Parameter(names = "--timeout", description = "HTTP request timeout, in millisecond", descriptionKey = "duniter4j.client.params.peer.timeout")
    public Integer timeout = null;

    private Peer peer = null;

    public void parse() {
        if (StringUtils.isNotBlank(peerStr)) {
            String[] parts = peerStr.split(":");
            if (parts.length > 2) {
                throw new ParameterException(I18n.t("duniter4j.client.params.error.invalidOption", "--peer"));
            }
            String host = parts[0];
            Integer port = parts.length == 2 ? Integer.parseInt(parts[1]) : null;

            Peer.Builder peerBuilder = Peer.newBuilder().setHost(host);
            if (port != null) {
                peerBuilder.setPort(port);
            }
            if (useSsl){
                peerBuilder.setUseSsl(useSsl);
            }
            peer = peerBuilder.build();

            log.info(I18n.t("duniter4j.client.info.peer", peer.getHost(), peer.getPort()));
        }
        else {
            Configuration config = Configuration.instance();
            peer = Peer.newBuilder().setHost(config.getNodeHost())
                    .setPort(config.getNodePort())
                    .build();
            log.info(I18n.t("duniter4j.client.info.peer.fallback", peer.getHost(), peer.getPort()));
        }
    }

    public Peer getPeer() {
        Preconditions.checkNotNull(peer, "Please call parse() before getPeer().");
        return peer;
    }
}

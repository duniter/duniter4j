package org.duniter.core.client.service.bma;

/*
 * #%L
 * UCoin Java :: Core Client API
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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.duniter.core.client.model.bma.EndpointApi;
import org.duniter.core.client.model.bma.NetworkPeering;
import org.duniter.core.client.model.bma.NetworkPeers;
import org.duniter.core.client.model.bma.jackson.JacksonUtils;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.client.model.local.Wallet;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.util.Preconditions;
import org.duniter.core.util.StringUtils;
import org.duniter.core.util.websocket.WebsocketClientEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by eis on 05/02/15.
 */
public class NetworkRemoteServiceImpl extends BaseRemoteServiceImpl implements NetworkRemoteService{

    private static final Logger log = LoggerFactory.getLogger(NetworkRemoteServiceImpl.class);

    public static final String URL_BASE = "/network";

    public static final String URL_PEERING = URL_BASE + "/peering";

    public static final String URL_PEERS = URL_BASE + "/peers";

    public static final String URL_PEERING_PEERS = URL_PEERING + "/peers";

    public static final String URL_PEERING_PEERS_LEAVES = URL_PEERING_PEERS + "?leaves=true";

    public static final String URL_PEERING_PEERS_LEAF = URL_PEERING_PEERS + "?leaf=";

    public static final String URL_WS_PEER = "/ws/peer";

    public NetworkRemoteServiceImpl() {
        super();
    }

    public NetworkPeering getPeering(Peer peer) {
        NetworkPeering result = httpService.executeRequest(peer, URL_PEERING, NetworkPeering.class);
        return result;
    }

    @Override
    public List<Peer> getPeers(Peer peer) {
        return findPeers(peer, null, null, null, null);
    }

    @Override
    public List<String> getPeersLeaves(Peer peer) {
        Preconditions.checkNotNull(peer);

        List<String> result = new ArrayList<>();
        JsonNode jsonNode= httpService.executeRequest(peer, URL_PEERING_PEERS_LEAVES, JsonNode.class);
        jsonNode.get("leaves").forEach(jsonNode1 -> {
            result.add(jsonNode1.asText());
        });
        return result;
    }

    @Override
    public NetworkPeers.Peer getPeerLeaf(Peer peer, String leaf) {
        Preconditions.checkNotNull(peer);
        JsonNode jsonNode = httpService.executeRequest(peer, URL_PEERING_PEERS_LEAF + leaf, JsonNode.class);
        NetworkPeers.Peer result = null;

        try {

            if (jsonNode.has("leaf")) {
                jsonNode = jsonNode.get("leaf");
                if (jsonNode.has("value")) {
                    ObjectMapper objectMapper = JacksonUtils.getThreadObjectMapper();
                    jsonNode = jsonNode.get("value");
                    String json = objectMapper.writeValueAsString(jsonNode);
                    result = objectMapper.readValue(json, NetworkPeers.Peer.class);
                }
            }
        } catch(IOException e) {
            throw new TechnicalException(e);
        }

        return result;
    }

    @Override
    public List<Peer> findPeers(Peer peer, String status, EndpointApi endpointApi, Integer currentBlockNumber, String currentBlockHash) {
        Preconditions.checkNotNull(peer);

        List<Peer> result = new ArrayList<Peer>();

        NetworkPeers remoteResult = httpService.executeRequest(peer, URL_PEERS, NetworkPeers.class);

        for (NetworkPeers.Peer remotePeer: remoteResult.peers) {
            boolean match = (status == null || status.equalsIgnoreCase(remotePeer.status))
                    && (currentBlockNumber == null || currentBlockNumber.equals(parseBlockNumber(remotePeer)))
                    && (currentBlockHash == null || currentBlockHash.equals(parseBlockHash(remotePeer)));

            if (match) {

                for (NetworkPeering.Endpoint endpoint : remotePeer.endpoints) {

                    match = endpointApi == null || (endpoint != null && endpointApi == endpoint.api);

                    if (match && endpoint != null) {
                        Peer childPeer = Peer.newBuilder()
                                .setCurrency(remotePeer.getCurrency())
                                .setPubkey(remotePeer.getPubkey())
                                .setEndpoint(endpoint)
                                .build();
                        result.add(childPeer);
                    }

                }
            }
        }

        return result;
    }


    @Override
    public WebsocketClientEndpoint addPeerListener(String currencyId, WebsocketClientEndpoint.MessageListener listener, boolean autoReconnect) {
        Peer peer = peerService.getActivePeerByCurrencyId(currencyId);
        return addPeerListener(peer, listener, autoReconnect);
    }

    @Override
    public WebsocketClientEndpoint addPeerListener(Peer peer, WebsocketClientEndpoint.MessageListener listener, boolean autoReconnect) {
        Preconditions.checkNotNull(peer);
        Preconditions.checkNotNull(listener);

        // Get (or create) the websocket endpoint
        WebsocketClientEndpoint wsClientEndPoint = getWebsocketClientEndpoint(peer, URL_WS_PEER, autoReconnect);

        // add listener
        wsClientEndPoint.registerListener(listener);

        return wsClientEndPoint;
    }

    @Override
    public String postPeering(Peer peer, NetworkPeering peering) {
        Preconditions.checkNotNull(peering);
        return postPeering(peer, peering.toString());
    }

    @Override
    public String postPeering(Peer peer, String peeringDocument) {
        Preconditions.checkNotNull(peer);
        Preconditions.checkNotNull(peeringDocument);

        // http post /tx/process
        HttpPost httpPost = new HttpPost(getPath(peer, URL_PEERING_PEERS));

        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "Will send peering document: \n------\n%s------",
                    peeringDocument));
        }

        List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
        urlParameters.add(new BasicNameValuePair("peer", peeringDocument));

        try {
            httpPost.setEntity(new UrlEncodedFormEntity(urlParameters));
        } catch (UnsupportedEncodingException e) {
            throw new TechnicalException(e);
        }

        String result = executeRequest(httpPost, String.class);
        if (log.isDebugEnabled()) {
            log.debug("Received from " + URL_PEERING_PEERS + " (POST): " + result);
        }
        return result;
    }

    /* -- Internal methods -- */

    protected Integer parseBlockNumber(NetworkPeers.Peer remotePeer) {
        Preconditions.checkNotNull(remotePeer);

        if (remotePeer.block == null) {
            return null;
        }
        int index = remotePeer.block.indexOf("-");
        if (index == -1) {
            return null;
        }

        String str = remotePeer.block.substring(0, index);
        try {
            return Integer.parseInt(str);
        } catch(NumberFormatException e) {
            return null;
        }
    }

    protected String parseBlockHash(NetworkPeers.Peer remotePeer) {
        Preconditions.checkNotNull(remotePeer);

        if (remotePeer.block == null) {
            return null;
        }
        int index = remotePeer.block.indexOf("-");
        if (index == -1) {
            return null;
        }

        String hash = remotePeer.block.substring(index+1);
        return hash;
    }


}

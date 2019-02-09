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
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.duniter.core.client.config.Configuration;
import org.duniter.core.client.model.bma.*;
import org.duniter.core.client.model.bma.jackson.JacksonUtils;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.util.Preconditions;
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

    public static final String URL_WS2P = URL_BASE + "/ws2p";

    public static final String URL_WS2P_HEADS = URL_WS2P + "/heads";

    private Configuration config;

    public NetworkRemoteServiceImpl() {
        super();
    }

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        config = Configuration.instance();
    }

    public NetworkPeering getPeering(Peer peer) {
        return httpService.executeRequest(peer, URL_PEERING, NetworkPeering.class);
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

        List<Peer> result = Lists.newArrayList();

        NetworkPeers remoteResult = httpService.executeRequest(peer, URL_PEERS, NetworkPeers.class, config.getNetworkLargerTimeout());

        for (NetworkPeers.Peer remotePeer: remoteResult.peers) {
            boolean match = (status == null || status.equalsIgnoreCase(remotePeer.getStatus()))
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
                                .setPeering(remotePeer)
                                .build();
                        result.add(childPeer);
                    }

                }
            }
        }

        return result;
    }

    @Override
    public List<Ws2pHead> getWs2pHeads(Peer peer) {
        Preconditions.checkNotNull(peer);

        NetworkWs2pHeads remoteResult = httpService.executeRequest(peer, URL_WS2P_HEADS, NetworkWs2pHeads.class, config.getNetworkLargerTimeout());

        List<Ws2pHead> result = Lists.newArrayList();

        for (NetworkWs2pHeads.Head remoteWs2pHead: remoteResult.heads) {

            Ws2pHead head = remoteWs2pHead.getMessage();
            if (head != null) {
                head.setSignature(remoteWs2pHead.getSig());

                result.add(head);
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
        HttpPost httpPost = new HttpPost(httpService.getPath(peer, URL_PEERING_PEERS));

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

        String result = httpService.executeRequest(httpPost, String.class);
        if (log.isDebugEnabled()) {
            log.debug("Received from " + URL_PEERING_PEERS + " (POST): " + result);
        }
        return result;
    }

    /* -- Internal methods -- */

    protected Integer parseBlockNumber(NetworkPeers.Peer remotePeer) {
        Preconditions.checkNotNull(remotePeer);

        if (remotePeer.getBlock() == null) {
            return null;
        }
        int index = remotePeer.getBlock().indexOf("-");
        if (index == -1) {
            return null;
        }

        String str = remotePeer.getBlock().substring(0, index);
        try {
            return Integer.parseInt(str);
        } catch(NumberFormatException e) {
            return null;
        }
    }

    protected String parseBlockHash(NetworkPeers.Peer remotePeer) {
        Preconditions.checkNotNull(remotePeer);

        if (remotePeer.getBlock()== null) {
            return null;
        }
        int index = remotePeer.getBlock().indexOf("-");
        if (index == -1) {
            return null;
        }

        String hash = remotePeer.getBlock().substring(index+1);
        return hash;
    }


}

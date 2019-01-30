package org.duniter.core.client.service;

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

import org.apache.http.client.utils.URIBuilder;
import org.duniter.core.beans.Service;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.client.service.exception.PeerConnectionException;
import org.apache.http.client.methods.HttpUriRequest;
import org.duniter.core.util.websocket.WebsocketClientEndpoint;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Created by blavenie on 29/12/15.
 */
public interface HttpService extends Service {

    void connect(Peer peer) throws PeerConnectionException;

    boolean isConnected();

    <T> T executeRequest(HttpUriRequest request, Class<? extends T> resultClass) ;

    <T> T executeRequest(HttpUriRequest request, Class<? extends T> resultClass, Class<?> errorClass);

    <T> T executeRequest(String absolutePath, Class<? extends T> resultClass) ;

    <T> T executeRequest(Peer peer, String absolutePath, Class<? extends T> resultClass);

    <T> T executeRequest(Peer peer, String absolutePath, Class<? extends T> resultClass, int timeout) ;

    String getPath(Peer peer, String... absolutePath);

    String getPath(String... absolutePath);

    URIBuilder getURIBuilder(URI baseUri, String... path);

    WebsocketClientEndpoint getWebsocketClientEndpoint(Peer peer, String path, boolean autoReconnect);

    <T> T readValue(String json, Class<T> clazz) throws IOException;

    <T> T readValue(byte[] json, Class<T> clazz) throws IOException;

    <T> T readValue(InputStream json, Class<T> clazz) throws IOException;
}

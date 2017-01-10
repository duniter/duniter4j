package org.duniter.core.util.websocket;

/*
 * #%L
 * Duniter4j :: Core Shared
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

import com.google.common.collect.Lists;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * ChatServer Client
 *
 * @author Jiji_Sasidharan
 */
@ClientEndpoint
public class WebsocketClientEndpoint implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(WebsocketClientEndpoint.class);

    private Session userSession = null;
    private List<MessageListener> messageListeners = Lists.newArrayList();
    private List<ConnectionListener> connectionListeners = Lists.newArrayList();
    private final URI endpointURI;
    private final boolean autoReconnect;
    private long lastTimeUp = -1;

    public WebsocketClientEndpoint(URI endpointURI) {
        this(endpointURI, true);
    }

    public WebsocketClientEndpoint(URI endpointURI, boolean autoReconnect) {
        this.endpointURI = endpointURI;
        this.autoReconnect = autoReconnect;
        connect();
    }


    @Override
    public void close() throws IOException {
        if (log.isDebugEnabled()) {
            log.debug(String.format("[%s] Closing WebSocket session...", endpointURI));
        }
        userSession.close();
        userSession = null;
    }

    /**
     * Callback hook for Connection open events.
     *
     * @param userSession the userSession which is opened.
     */
    @OnOpen
    public void onOpen(Session userSession) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Opening WebSocket... [%s]", endpointURI));
        }
        this.userSession = userSession;
    }

    /**
     * Callback hook for Connection close events.
     *
     * @param userSession the userSession which is getting closed.
     * @param reason the reason for connection close
     */
    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Closing WebSocket... [%s]", endpointURI));
        }
        this.userSession = null;

        // abnormal close : try to reconnect
        if (reason.getCloseCode() == CloseReason.CloseCodes.CLOSED_ABNORMALLY && autoReconnect)  {
            connect();
        }
    }

    /**
     * Callback hook for Message Events. This method will be invoked when a client send a message.
     *
     * @param message The text message
     */
    @OnMessage
    public void onMessage(final String message) {
        if (CollectionUtils.isNotEmpty(messageListeners)) {
            if (log.isDebugEnabled()) {
                log.debug("[%s] Received message: " + message);
            }

            messageListeners.stream().forEach(messageListener -> {
                try {
                    messageListener.onMessage(message);
                } catch (Exception e) {
                    log.error(String.format("[%s] Error during message handling: %s", endpointURI, e.getMessage()), e);
                }
            });
        }
    }

    /**
     * register message listener
     *
     * @param listener
     */
    public void registerListener(MessageListener listener) {
        synchronized (messageListeners) {
            this.messageListeners.add(listener);
        }
    }

    /**
     * register connection listener
     *
     * @param listener
     */
    public void registerListener(ConnectionListener listener) {
        synchronized (connectionListeners) {
            this.connectionListeners.add(listener);
        }
    }

    /**
     * Send a message.
     *
     * @param message
     */
    public void sendMessage(String message) {
        this.userSession.getAsyncRemote().sendText(message);
    }

    /**
     * Is closed ?
     * @return
     */
    public boolean isClosed() {
        return (userSession == null);
    }

    /**
     * Message listener.
     */
    public interface MessageListener {

        void onMessage(String message);
    }

    /**
     * Connection listener.
     */
    public interface ConnectionListener {

        void onSuccess();

        void onError(Exception e, long lastTimeUp);
    }

    /* -- Internal method */

    private void connect() {
        while(isClosed()) {
            try {
                WebSocketContainer container = ContainerProvider.getWebSocketContainer();
                container.connectToServer(this, endpointURI);
                lastTimeUp = System.currentTimeMillis() / 1000;
                notifyConnectionSuccess();
                return; // stop while
            } catch (Exception e) {
                notifyConnectionError(e);
                if (!this.autoReconnect) throw new TechnicalException(e);
                log.warn(String.format("[%s] Unable to connect [%s]. Retrying in 10s...", endpointURI.toString(), e.getMessage()), e);
            }

            // wait 10s, then try again
            try {
                Thread.sleep(10 * 1000);
            }
            catch(Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void notifyConnectionSuccess() {
        synchronized (connectionListeners) {
            connectionListeners.stream().forEach(connectionListener -> {
                try {
                    connectionListener.onSuccess();
                } catch (Exception e) {
                    log.error(String.format("[%s] Error during ConnectionListener.onSuccess(): %s", endpointURI, e.getMessage()), e);
                }
            });
        }
    }

    private void notifyConnectionError(final Exception error) {
        synchronized (connectionListeners) {
            connectionListeners.stream().forEach(connectionListener -> {
                try {
                    connectionListener.onError(error, lastTimeUp);
                } catch (Exception e) {
                    log.error(String.format("[%s] Error during ConnectionListener.onError(): %s", endpointURI, e.getMessage()), e);
                }
            });
        }
    }
}
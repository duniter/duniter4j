package org.duniter.core.util.websocket;

import com.google.common.collect.Lists;
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
    private List<MessageHandler> messageHandlers = Lists.newArrayList();
    private final URI endpointURI;

    public WebsocketClientEndpoint(URI endpointURI) {
        this.endpointURI = endpointURI;
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, endpointURI);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
    }

    /**
     * Callback hook for Message Events. This method will be invoked when a client send a message.
     *
     * @param message The text message
     */
    @OnMessage
    public void onMessage(String message) {
        synchronized (messageHandlers) {
            if (CollectionUtils.isNotEmpty(messageHandlers)) {
                if (log.isDebugEnabled()) {
                    log.debug("[%s] Received message: " + message);
                }

                for (MessageHandler messageHandler : messageHandlers) {
                    try {
                        messageHandler.handleMessage(message);
                    } catch (Exception e) {
                        log.error(String.format("[%s] Error during message handling: %s", endpointURI, e.getMessage()), e);
                    }
                }
            }
        }
    }

    /**
     * register message handler
     *
     * @param msgHandler
     */
    public void addMessageHandler(MessageHandler msgHandler) {
        synchronized (messageHandlers) {
            this.messageHandlers.add(msgHandler);
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
     * Message handler.
     *
     * @author Jiji_Sasidharan
     */
    public static interface MessageHandler {

        public void handleMessage(String message);
    }
}
package org.duniter.elasticsearch.websocket;

/*
 * #%L
 * Duniter4j :: ElasticSearch Plugin
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

/*
    Copyright 2015 ForgeRock AS

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

import org.duniter.core.exception.TechnicalException;
import org.duniter.elasticsearch.PluginSettings;
import org.duniter.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.glassfish.tyrus.server.Server;

import javax.websocket.DeploymentException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

public class WebSocketServer {

    public static final String WS_PATH = "/ws";
    private final ESLogger log = Loggers.getLogger("duniter.ws");
    private List<Class<?>> endPoints = new ArrayList<>();

    @Inject
    public WebSocketServer(final PluginSettings pluginSettings, ThreadPool threadPool) {
        // If WS enable
        if (pluginSettings.getWebSocketEnable()) {
            // When node started
            threadPool.scheduleOnStarted(() -> {
                // start WS server
                startServer(pluginSettings.getWebSocketHost(),
                        pluginSettings.getWebSocketPort(),
                        getEndPoints());
            });
        }
    }


    public void addEndPoint(Class<?> endPoint) {
        endPoints.add(endPoint);
    }

    /* -- private medthod -- */

    private Class[] getEndPoints() {
        return endPoints.toArray(new Class<?>[endPoints.size()]);
    }

    private void startServer(String host, int port, Class<?>[] endPoints) {

        final Server server = new Server(host, port, WS_PATH, null, endPoints) ;

        try {
            log.info(String.format("Starting Websocket server... [%s:%s%s]", host, port, WS_PATH));
            AccessController.doPrivileged(new PrivilegedAction() {
                @Override
                public Object run() {
                    try {
                        // Tyrus tries to load the server code using reflection. In Elasticsearch 2.x Java
                        // security manager is used which breaks the reflection code as it can't find the class.
                        // This is a workaround for that
                        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
                        server.start();
                        log.info("Websocket server started");
                        return null;
                    } catch (DeploymentException e) {
                        throw new RuntimeException("Failed to start server", e);
                    }
                }
            });
        } catch (Exception e) {
            log.error("Failed to start Websocket server", e);
            throw new TechnicalException(e);
        }
    }

}

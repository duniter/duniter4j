package org.duniter.elasticsearch.user.websocket;

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

//@ServerEndpoint(value = "/event/user/{pubkey}")
public class WebsocketUserEventEndPoint /*implements UserEventListener*/ {

  /*  private static final String PATH_PARAM_PUBKEY = "pubkey";
    private final static Pattern PUBKEY_PATTERN = Pattern.compile(Constants.Regex.PUBKEY);

    private final ESLogger log = Loggers.getLogger("duniter.ws.user.event");
    private Session session;
    private String pubkey;


    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        this.pubkey = session.getPathParameters() != null ? session.getPathParameters().get(PATH_PARAM_PUBKEY) : null;

        if (StringUtils.isBlank(pubkey) || !PUBKEY_PATTERN.matcher(pubkey).matches()) {
            try {
                this.session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "Invalid pubkey"));
            } catch (IOException e) {
                // silent
            }
            return;
        }

        log.info("User [%s] connecting with id [%s]", session.getId());
        UserEventService.registerListener(this);
    }

    @Override
    public void onEvent(UserEvent event) {
        session.getAsyncRemote().sendText("{\"type\":\""+event.getType().name()+"\",\"message\":\"" + event.getMessage() + "\"}");
    }

    @Override
    public String getId() {
        return session == null ? null : session.getId();
    }

    @OnMessage
    public void onMessage(String message) {
        log.info("Received message: "+message);
    }

    @OnClose
    public void onClose(CloseReason reason) {
        log.info("Closing websocket: "+reason);
        UserEventService.unregisterListener(this);
        this.session = null;
    }

    @OnError
    public void onError(Throwable t) {
        log.error("Error on websocket "+(session == null ? null : session.getId()), t);
    }
*/

}

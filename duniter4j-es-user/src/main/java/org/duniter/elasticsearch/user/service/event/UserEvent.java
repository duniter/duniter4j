package org.duniter.elasticsearch.user.service.event;

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

import org.nuiton.i18n.I18n;

import java.util.Locale;

/**
 * Created by blavenie on 29/11/16.
 */
public class UserEvent {

    private final EventType type;

    private final String code;

    private final long time;

    private final String message;

    private final String[] params;

    private final UserEventLink link;

    public UserEvent(EventType type, String code) {
        this(type, code, null, "duniter.event." + code, null);
    }

    public UserEvent(EventType type, String code, String message, String... params) {
        this(type, code, null, message, params);
    }

    public UserEvent(EventType type, String code, UserEventLink link, String message, String... params) {
        this.type = type;
        this.code = code;
        this.params = params;
        this.link = link;
        this.message = message;
        this.time = Math.round(1d * System.currentTimeMillis() / 1000);
    }

    public EventType getType() {
        return type;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getLocalizedMessage(Locale locale) {
        return I18n.l(locale, message, params);
    }

    public String[] getParams() {
        return params;
    }

    public long getTime() {
        return time;
    }

    public enum EventType {
        INFO,
        WARN,
        ERROR
    }
}

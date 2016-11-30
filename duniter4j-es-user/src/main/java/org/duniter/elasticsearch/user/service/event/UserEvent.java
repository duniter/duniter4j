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

    private EventType type;

    private String code;

    private long time;

    private String message;


    private String[] params;

    public UserEvent(EventType type, String code) {
        this(type, code, null);
    }

    public UserEvent(EventType type, String code, String[] params) {
        this.type = type;
        this.code = code;
        this.params = params;
        // default
        this.message = I18n.t("duniter4j.event." + code, params);
        this.time = Math.round(1d * System.currentTimeMillis() / 1000);
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public String getLocalizedMessage(Locale locale) {
        return I18n.l(locale, "duniter4j.event." + code, params);
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String[] getParams() {
        return params;
    }

    public void setParams(String[] params) {
        this.params = params;
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

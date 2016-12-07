package org.duniter.elasticsearch.user.model;

/*
 * #%L
 * Duniter4j :: Core Client API
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.duniter.core.client.model.elasticsearch.Record;
import org.duniter.core.exception.TechnicalException;
import org.nuiton.i18n.I18n;

import java.util.Locale;

/**
 * Created by blavenie on 29/11/16.
 */
public class UserEvent extends Record {

    public enum EventType {
        INFO,
        WARN,
        ERROR
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(UserEvent.EventType type, String code) {
        return new Builder(type, code, null, null);
    }

    public static Builder newBuilder(UserEvent.EventType type, String code, String message, String... params) {
        return new Builder(type, code, message, params);
    }

    public static final String PROPERTY_TYPE="type";
    public static final String PROPERTY_CODE="code";
    public static final String PROPERTY_MESSAGE="message";
    public static final String PROPERTY_PARAMS="params";
    public static final String PROPERTY_LINK="link";
    public static final String PROPERTY_RECIPIENT="recipient";


    private EventType type;

    private String recipient;

    private String code;

    private String message;

    private String[] params;

    private UserEventLink link;

    public UserEvent() {
        super();
    }

    public UserEvent(EventType type, String code, String message, String... params) {
        super();
        this.type = type;
        this.code = code;
        this.message = message;
        this.params = params;
        setTime(getDefaultTime());
    }

    public UserEvent(UserEvent another) {
        super(another);
        this.type = another.getType();
        this.code = another.getCode();
        this.params = another.getParams();
        this.link = (another.getLink() != null) ? new UserEventLink(another.getLink()) : null;
        this.message = another.getMessage();
        this.recipient = another.getRecipient();
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

    public String[] getParams() {
        return params;
    }

    public UserEventLink getLink() {
        return link;
    }

    public String getLocalizedMessage(Locale locale) {
        return I18n.l(locale, this.message, this.params);
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setParams(String[] params) {
        this.params = params;
    }

    public void setLink(UserEventLink link) {
        this.link = link;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    @JsonIgnore
    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        } catch(Exception e) {
            throw new TechnicalException(e);
        }
    }

    @JsonIgnore
    public String toJson(Locale locale) {
        UserEvent copy = new UserEvent(this);
        copy.setMessage(getLocalizedMessage(locale));
        return copy.toJson();
    }

    public static class Builder {

        private UserEvent result;

        private Builder() {
            result = new UserEvent();
        }

        public Builder(UserEvent.EventType type, String code, String message, String... params) {
            result = new UserEvent(type, code, message, params);
        }

        public Builder setMessage(String message, String... params) {
            result.setMessage(message);
            result.setParams(params);
            return this;
        }

        public Builder setRecipient(String recipient) {
            result.setRecipient(recipient);
            return this;
        }

        public Builder setIssuer(String issuer) {
            result.setIssuer(issuer);
            return this;
        }

        public Builder setLink(String index, String type, String id) {
            result.setLink(new UserEventLink(index, type, id));
            return this;
        }

        public Builder setLink(String index, String type, String id, String anchor) {
            result.setLink(new UserEventLink(index, type, id, anchor));
            return this;
        }

        public UserEvent build() {
            if (result.getTime() == null) {
                result.setTime(getDefaultTime());
            }
            return new UserEvent(result);
        }
    }

    private static int getDefaultTime() {
        return Math.round(1f * System.currentTimeMillis() / 1000);
    }
}

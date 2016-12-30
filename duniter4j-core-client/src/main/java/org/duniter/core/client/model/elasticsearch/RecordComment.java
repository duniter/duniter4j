package org.duniter.core.client.model.elasticsearch;

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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Created by blavenie on 01/03/16.
 */
public class RecordComment extends Record {

    public static final String PROPERTY_MESSAGE="message";
    public static final String PROPERTY_RECORD="record";
    public static final String PROPERTY_REPLY_TO="replyTo";

    public static final String PROPERTY_REPLY_TO_JSON="reply_to";

    private String message;
    private String record;
    private String replyTo;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRecord() {
        return record;
    }

    public void setRecord(String record) {
        this.record = record;
    }

    @JsonGetter(PROPERTY_REPLY_TO_JSON)
    public String getReplyTo() {
        return replyTo;
    }

    @JsonSetter(PROPERTY_REPLY_TO_JSON)
    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }
}

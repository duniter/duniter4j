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
public class Message extends Record {

    public static final String PROPERTY_TITLE="title";
    public static final String PROPERTY_CONTENT="content";
    public static final String PROPERTY_RECIPIENT="recipient";
    public static final String PROPERTY_READ_SIGNATURE="read_signature";

    private String title;
    private String content;
    private String recipient;
    private String readSignature;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    @JsonGetter(PROPERTY_READ_SIGNATURE)
    public String getReadSignature() {
        return readSignature;
    }

    @JsonSetter(PROPERTY_READ_SIGNATURE)
    public void setReadSignature(String readSignature) {
        this.readSignature = readSignature;
    }

}

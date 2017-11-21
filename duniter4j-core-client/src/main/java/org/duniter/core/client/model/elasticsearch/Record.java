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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.duniter.core.client.model.local.LocalEntity;

/**
 * Created by blavenie on 01/03/16.
 */
public class Record implements LocalEntity<String> {

    public static final String PROPERTY_VERSION="version";
    public static final String PROPERTY_ISSUER="issuer";
    public static final String PROPERTY_HASH="hash";
    public static final String PROPERTY_SIGNATURE="signature";
    public static final String PROPERTY_TIME="time";

    private Integer version;
    private String id;
    private String issuer;
    private String hash;
    private String signature;
    private Long time;

    public Record() {
    }

    public Record(Record another) {
        this.version = another.getVersion();
        this.id = another.getId();
        this.issuer = another.getIssuer();
        this.hash = another.getHash();
        this.signature = another.getSignature();
        this.time = another.getTime();
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @JsonIgnore
    public String getId() {
        return id;
    }

    @JsonIgnore
    public void setId(String id) {
        this.id = id;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }


    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

}

package org.duniter.elasticsearch.user.model;

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

/**
 * Created by blavenie on 29/11/16.
 */
public class UserEventLink {

    private String index;

    private String type;

    private String id;

    private String anchor;

    public UserEventLink() {
    }

    public UserEventLink(String index, String type, String id) {
        this(index, type, id, null);
    }

    public UserEventLink(String index, String type, String id, String anchor) {
        this.index = index;
        this.type = type;
        this.id = id;
        this.anchor = anchor;
    }

    public UserEventLink(UserEventLink another) {
        this.index = another.getIndex();
        this.type = another.getType();
        this.id = another.getId();
        this.anchor = another.getAnchor();
    }

    public String getIndex() {
        return index;
    }

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public String getAnchor() {
        return anchor;
    }
}

package org.duniter.elasticsearch.model;

/*
 * #%L
 * UCoin Java Client :: Core API
 * %%
 * Copyright (C) 2014 - 2015 EIS
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


import com.fasterxml.jackson.databind.JsonNode;

import java.io.Serializable;
import java.util.Iterator;

/**
 * Created by eis on 05/02/15.
 */
public class SearchResponse implements Serializable {

    protected JsonNode node;

    public SearchResponse(JsonNode response) {
        this.node = response;
    }

    public Hits getHits() {
        return new Hits(node.get("hits"));
    }

    public class Hits implements Iterator<Hit>{

        protected JsonNode node;
        private Iterator<JsonNode> hits;
        Hits(JsonNode node) {
            this.node = node;
            this.hits = node == null ? null : node.get("hits").iterator();
        }

        public int getTotal() {
            return node == null ? 0 : node.get("total").asInt(0);
        }

        public boolean hasNext() {
            return hits != null && hits.hasNext();
        }
        public Hit next() {
            return hits == null ? null : new Hit(hits.next());
        }
    }

    public class Hit {

        private JsonNode node;
        Hit(JsonNode node) {
            this.node = node;
        }

        public String getId() {
            return node.get("_id").asText();
        }

        public JsonNode getSource() {
            return node.get("_source");
        }

    }
}
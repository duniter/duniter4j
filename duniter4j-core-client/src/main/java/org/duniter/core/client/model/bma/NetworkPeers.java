package org.duniter.core.client.model.bma;

/*
 * #%L
 * UCoin Java :: Core Client API
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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.base.Joiner;

import java.io.Serializable;

/**
 * Created by eis on 05/02/15.
 */
public class NetworkPeers implements Serializable {

    public Peer[] peers;

    public String toString() {
        return Joiner.on(",").join(peers);
    }

    public static class Peer extends NetworkPeering implements Serializable {
        public Long firstDown;
        public Long lastTry;
        public Long lastContact;
        public String hash;

        @JsonGetter("first_down")
        public Long getFirstDown() {
            return firstDown;
        }

        @JsonSetter("first_down")
        public void setFirstDown(Long firstDown) {
            this.firstDown = firstDown;
        }

        @JsonGetter("last_try")
        public Long getLastTry() {
            return lastTry;
        }

        @JsonSetter("last_try")
        public void setLastTry(Long lastTry) {
            this.lastTry = lastTry;
        }

        @JsonGetter("lastContact")
        public Long getLastContact() {
            return lastContact;
        }

        @JsonSetter("lastContact")
        public void setLastContact(Long lastContact) {
            this.lastContact = lastContact;
        }

        @JsonIgnore
        public String getHash() {
            return hash;
        }

        @JsonIgnore
        public void setHash(String hash) {
            this.hash = hash;
        }

        @Override
        public String getRaw() {
            return super.getRaw();
        }
    }
}

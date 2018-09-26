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

import java.io.Serializable;

/**
 * Created by eis on 05/02/15.
 */
public class NetworkPeers implements Serializable {

    public Peer[] peers;

    public String toString() {
        String s = "";
        for(Peer peer : peers) {
            s += peer.toString() + "\n";
        }
        return s;
    }

    public static class Peer implements Serializable {
        public String version;
        public String currency;
        public String status;
        public Long statusTS;
        public String block;
        public String signature;
        public String pubkey;
        public Long firstDown;
        public Long lastTry;
        public String raw;
        public NetworkPeering.Endpoint[] endpoints;

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        @JsonGetter("statusTS")
        public Long getStatusTS() {
            return statusTS;
        }

        @JsonSetter("statusTS")
        public void setStatusTS(Long statusTS) {
            this.statusTS = statusTS;
        }

        public String getBlock() {
            return block;
        }

        public void setBlock(String block) {
            this.block = block;
        }

        public String getSignature() {
            return signature;
        }

        public void setSignature(String signature) {
            this.signature = signature;
        }

        public String getPubkey() {
            return pubkey;
        }

        public void setPubkey(String pubkey) {
            this.pubkey = pubkey;
        }

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

        public NetworkPeering.Endpoint[] getEndpoints() {
            return endpoints;
        }

        public void setEndpoints(NetworkPeering.Endpoint[] endpoints) {
            this.endpoints = endpoints;
        }

        @JsonIgnore
        public String getRaw() {
            return raw;
        }

        public void setRaw(String raw) {
            this.raw = raw;
        }

        @Override
        public String toString() {
            String s = "version=" + version + "\n" +
                    "currency=" + currency + "\n" +
                    "pubkey=" + pubkey + "\n" +
                    "status=" + status + "\n" +
                    "block=" + block + "\n";
            for(NetworkPeering.Endpoint endpoint: endpoints) {
                if (endpoint != null) {
                    s += endpoint.toString() + "\n";
                }
            }
            return s;
        }
    }
}

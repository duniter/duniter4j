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

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

/**
 * Created by eis on 05/02/15.
 */
public class NetworkPeering implements Serializable {
    private String version;
    private String currency;
    private String block;
    private String signature;
    private String status;

    private String raw;

    private String pubkey;

    public Endpoint[] endpoints;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @JsonIgnore
    public String getRaw() {
        return raw;
    }

    public void setRaw(String raw) {
        this.raw = raw;
    }

    public Endpoint[] getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(Endpoint[] endpoints) {
        this.endpoints = endpoints;
    }

    public String toString() {
        String s = "version=" + version + "\n" +
                "currency=" + currency + "\n" +
                "pubkey=" + pubkey + "\n" +
                "signature=" + signature + "\n" +
                "status=" + status + "\n" +
                "block=" + block + "\n";
        for(Endpoint endpoint : endpoints) {
            s += endpoint.toString() + "\n";
        }
        return s;

    }

    public static class Endpoint implements Serializable {
        public EndpointApi api;
        public String dns;
        public String ipv4;
        public String ipv6;
        public Integer port;
        public String id;

        public EndpointApi getApi() {
            return api;
        }

        public void setApi(EndpointApi api) {
            this.api = api;
        }

        public String getDns() {
            return dns;
        }

        public void setDns(String dns) {
            this.dns = dns;
        }

        public String getIpv4() {
            return ipv4;
        }

        public void setIpv4(String ipv4) {
            this.ipv4 = ipv4;
        }

        public String getIpv6() {
            return ipv6;
        }

        public void setIpv6(String ipv6) {
            this.ipv6 = ipv6;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @Override
        public String toString() {
            String s = "api=" + api.name() + "\n" +
                    (id != null ? ("id=" + id + "\n") : "" ) +
                    "dns=" + dns + "\n" +
                    "ipv4=" + ipv4 + "\n" +
                    "ipv6=" + ipv6 + "\n" +
                    "port=" + port + "\n";
            return s;
        }
    }


}

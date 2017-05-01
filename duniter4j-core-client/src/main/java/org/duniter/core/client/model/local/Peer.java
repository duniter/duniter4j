package org.duniter.core.client.model.local;

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


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Joiner;
import org.duniter.core.client.model.bma.EndpointApi;
import org.duniter.core.client.model.bma.NetworkPeering;
import org.duniter.core.util.Preconditions;
import org.duniter.core.util.StringUtils;
import org.duniter.core.util.http.InetAddressUtils;

import java.io.Serializable;
import java.util.StringJoiner;

public class Peer implements LocalEntity<String>, Serializable {



    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private String api;
        private String dns;
        private String ipv4;
        private String ipv6;
        private Integer port;
        private Boolean useSsl;
        private String pubkey;
        private String hash;
        private String currency;

        public Builder() {

        }

        public Builder setApi(String api) {
            this.api = api;
            return this;
        }

        public Builder setDns(String dns) {
            this.dns = dns;
            return this;
        }

        public Builder setIpv4(String ipv4) {
            this.ipv4 = ipv4;
            return this;
        }

        public Builder setIpv6(String ipv6) {
            this.ipv6 = ipv6;
            return this;
        }

        public Builder setPort(int port) {
            this.port = port;
            return this;
        }

        public Builder setUseSsl(boolean useSsl) {
            this.useSsl = useSsl;
            return this;
        }

        public Builder setCurrency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder setPubkey(String pubkey) {
            this.pubkey = pubkey;
            return this;
        }

        public Builder setHash(String hash) {
            this.hash = hash;
            return this;
        }

        public Builder setHost(String host) {
            Preconditions.checkNotNull(host);
            if (InetAddressUtils.isIPv4Address(host)) {
                this.ipv4 = host;
            }
            else if (InetAddressUtils.isIPv6Address(host)) {
                this.ipv6 = host;
            }
            else {
                this.dns = host;
            }
            return this;
        }

        public Builder setEndpoint(NetworkPeering.Endpoint source) {
            Preconditions.checkNotNull(source);
            if (source.api != null) {
               setApi(source.api.name());
            }
            if (StringUtils.isNotBlank(source.dns)) {
               setDns(source.dns);
            }
            if (StringUtils.isNotBlank(source.ipv4)) {
               setIpv4(source.ipv4);
            }
            if (StringUtils.isNotBlank(source.ipv6)) {
               setIpv6(source.ipv6);
            }
            if (StringUtils.isNotBlank(source.ipv6)) {
               setHost(source.ipv6);
            }
            if (source.port != null) {
               setPort(source.port);
            }
            return this;
        }

        public Peer build() {
            int port = this.port != null ? this.port : 80;
            boolean useSsl = this.useSsl != null ? this.useSsl :
                    (port == 443 || this.api == EndpointApi.BMAS.name());
            String api = this.api != null ? this.api : EndpointApi.BASIC_MERKLED_API.name();
            Peer ep = new Peer(api, dns, ipv4, ipv6, port, useSsl);
            if (StringUtils.isNotBlank(this.currency)) {
                ep.setCurrency(this.currency);
            }
            if (StringUtils.isNotBlank(this.pubkey)) {
                ep.setPubkey(this.pubkey);
            }
            if (StringUtils.isNotBlank(this.hash)) {
                ep.setHash(this.hash);
            }
            return ep;
        }

    }


    private String id;

    private String api;
    private String dns;
    private String ipv4;
    private String ipv6;

    private String url;
    private String host;
    private String pubkey;

    private String hash;
    private String currency;

    private Stats stats = new Stats();

    private int port;
    private boolean useSsl;

    public Peer() {
        // default constructor, need for de-serialization
    }

    /**
     * @deprecated Use Builder instead
     * @param host Can be a ipv4, ipv6 or a dns
     * @param port any port, or null (default: 80)
     */
    @Deprecated
    public Peer(String host, Integer port) {
        this.api = EndpointApi.BASIC_MERKLED_API.name();
        if (InetAddressUtils.isIPv4Address(host)) {
            this.ipv4 = host;
        }
        if (InetAddressUtils.isIPv6Address(host)) {
            this.ipv6 = host;
        }
        else {
            this.dns = host;
        }
        this.port = port != null ? port : 80;
        this.useSsl = (port == 443 || this.api == EndpointApi.BMAS.name());
        init();
    }

    public Peer(String api, String dns, String ipv4, String ipv6, int port, boolean useSsl) {
        this.api = api;
        this.dns = StringUtils.isNotBlank(dns) ? dns : null;
        this.ipv4 = StringUtils.isNotBlank(ipv4) ? ipv4 : null;
        this.ipv6 = StringUtils.isNotBlank(ipv6) ? ipv6 : null;
        this.port = port;
        this.useSsl = useSsl;
        init();
    }

    protected void init() {
        // If SSL: prefer DNS name (should be defined in SSL certificate)
        // else (if define) use ipv4 (if NOT local IP)
        // else (if define) use dns
        // else (if define) use ipv6
        host = ((port == 443 || useSsl) && dns != null) ? dns :
                (ipv4 != null && InetAddressUtils.isNotLocalIPv4Address(ipv4) ? ipv4 :
                    (dns != null ? dns :
                        (ipv6 != null ? "[" + ipv6 + "]" : "")));
        // Use local IPv4 if no other host found
        if (StringUtils.isBlank(host) && ipv4 != null && InetAddressUtils.isIPv4Address(ipv4)) {
            host = ipv4;
        }
        String protocol = (port == 443 || useSsl) ? "https" : "http";
        this.url = protocol + "://" + host + (port != 80 ? (":" + port) : "");
    }

    @JsonIgnore
    public String getId() {
        return id;
    }

    @JsonIgnore
    public void setId(String id) {
        this.id  = id;
    }

    @JsonIgnore
    public String getHost() {
        return this.host; // computed in init()
    }

    @JsonIgnore
    public String getUrl() {
        return this.url; // computed in init()
    }

    @JsonIgnore
    public String computeKey()  {
        return Joiner.on('-').skipNulls().join(pubkey, dns, ipv4, ipv6, port, useSsl);
    }

    public String getApi() {
        return api;
    }

    public String getDns() {
        return dns;
    }

    public void setDns(String dns) {
        this.dns = dns;
        init();
    }

    public String getIpv4() {
        return ipv4;
    }

    public void setIpv4(String ipv4) {
        this.ipv4 = ipv4;
        init();
    }

    public String getIpv6() {
        return ipv6;
    }

    public void setIpv6(String ipv6) {
        this.ipv6 = ipv6;
        init();
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
        init();
    }

    public boolean isUseSsl() {
        return useSsl;
    }

    public void setUseSsl(boolean useSsl) {
        this.useSsl = useSsl;
        init();
    }

    public String getPubkey() {
        return pubkey;
    }

    public void setPubkey(String pubkey) {
        this.pubkey = pubkey;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Stats getStats() {
        return stats;
    }

    public void setStats(Stats stats) {
        this.stats = stats;
    }

    public String toString() {
        StringJoiner joiner = new StringJoiner(" ");
        if (api != null) {
            joiner.add(api);
        }
        if (dns != null) {
            joiner.add(dns);
        }
        if (ipv4 != null) {
            joiner.add(ipv4);
        }
        if (ipv6 != null) {
            joiner.add(ipv6);
        }
        if (port != 80) {
            joiner.add(String.valueOf(port));
        }
        return joiner.toString();
    }

    public enum PeerStatus {
        UP,
        DOWN,
        ERROR
    }

    public static class Stats {
        private String version;
        private PeerStatus status = PeerStatus.UP; // default
        private Integer blockNumber;
        private String blockHash;
        private String error;
        private Long medianTime;
        private Integer hardshipLevel;
        private boolean isMainConsensus = false;
        private boolean isForkConsensus = false;
        private Double consensusPct = 0d;
        private String uid;

        public Stats() {

        }

        public PeerStatus getStatus() {
            return status;
        }

        @JsonIgnore
        public boolean isReacheable() {
            return status != null && status == PeerStatus.UP;
        }

        public void setStatus(PeerStatus status) {
            this.status = status;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public Integer getBlockNumber() {
            return blockNumber;
        }

        public void setBlockNumber(Integer blockNumber) {
            this.blockNumber = blockNumber;
        }

        public String getBlockHash() {
            return blockHash;
        }

        public void setBlockHash(String blockHash) {
            this.blockHash = blockHash;
        }

        public Long getMedianTime() {
            return medianTime;
        }

        public void setMedianTime(Long medianTime) {
            this.medianTime = medianTime;
        }

        public boolean isMainConsensus() {
            return isMainConsensus;
        }

        public void setMainConsensus(boolean mainConsensus) {
            this.isMainConsensus = mainConsensus;
        }

        public boolean isForkConsensus() {
            return isForkConsensus;
        }

        public void setForkConsensus(boolean forkConsensus) {
            this.isForkConsensus = forkConsensus;
        }

        public Double getConsensusPct() {
            return consensusPct;
        }

        public void setConsensusPct(Double consensusPct) {
            this.consensusPct = consensusPct;
        }

        public Integer getHardshipLevel() {
            return hardshipLevel;
        }

        public void setHardshipLevel(Integer hardshipLevel) {
            this.hardshipLevel = hardshipLevel;
        }

        public String getUid() {
            return uid;
        }

        public void setUid(String uid) {
            this.uid = uid;
        }
    }
}

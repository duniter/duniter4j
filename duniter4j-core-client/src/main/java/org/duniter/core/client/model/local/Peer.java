package org.duniter.core.client.model.local;

/*
 * #%L
 * Duniter4j :: Core API
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
import com.google.common.base.Preconditions;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.apache.commons.lang3.StringUtils;
import org.duniter.core.client.model.bma.EndpointApi;
import org.duniter.core.client.model.bma.NetworkPeering;
import org.duniter.core.model.IEntity;
import org.duniter.core.util.http.InetAddressUtils;

import java.util.StringJoiner;

@Data
@FieldNameConstants
public class Peer implements IEntity<String> {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String api;
        private String dns;
        private String ipv4;
        private String ipv6;
        private Integer port;
        private String epId;
        private Boolean useSsl;
        private String pubkey;
        private String hash;
        private String currency;
        private String path;

        private Peering peering;
        private Stats stats;

        public Builder() {

        }

        public Builder api(String api) {
            this.api = api;
            return this;
        }

        public Builder dns(String dns) {
            this.dns = dns;
            return this;
        }

        public Builder ipv4(String ipv4) {
            this.ipv4 = ipv4;
            return this;
        }

        public Builder ipv6(String ipv6) {
            this.ipv6 = ipv6;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder useSsl(boolean useSsl) {
            this.useSsl = useSsl;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder pubkey(String pubkey) {
            this.pubkey = pubkey;
            return this;
        }

        public Builder hash(String hash) {
            this.hash = hash;
            return this;
        }

        public Builder epId(String epId) {
            this.epId = epId;
            return this;
        }

        public Builder host(String host) {
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

        public Builder endpoint(NetworkPeering.Endpoint source) {
            Preconditions.checkNotNull(source);
            if (source.api != null) {
               api(source.api);
            }
            if (StringUtils.isNotBlank(source.id)) {
                epId(source.id);
            }
            if (StringUtils.isNotBlank(source.dns)) {
               dns(source.dns);
            }
            if (StringUtils.isNotBlank(source.ipv4)) {
               ipv4(source.ipv4);
            }
            if (StringUtils.isNotBlank(source.ipv6)) {
               ipv6(source.ipv6);
            }
            if (StringUtils.isNotBlank(source.ipv6)) {
               host(source.ipv6);
            }
            if (source.port != null) {
               port(source.port);
            }
            if (StringUtils.isNotBlank(source.path)) {
                path(source.path);
            }
            return this;
        }

        public Builder peering(NetworkPeering remotePeering) {
            this.peering = this.peering != null ? this.peering : new Peering();

            this.pubkey = remotePeering.getPubkey();

            this.peering.setVersion(remotePeering.getVersion());
            this.peering.setSignature(remotePeering.getSignature());

            String raw = remotePeering.getRaw();
            if (StringUtils.isBlank(raw)) {
                raw = remotePeering.toUnsignedRaw();
            }
            this.peering.setRaw(raw);

            // Block number+hash
            if (remotePeering.getBlock() != null) {
                String[] blockParts = remotePeering.getBlock().split("-");
                if (blockParts.length == 2) {
                    this.peering.setBlockNumber(Integer.parseInt(blockParts[0]));
                    this.peering.setBlockHash(blockParts[1]);
                }
            }

            return this;
        }

        public Builder stats(NetworkPeering remotePeering) {
            this.stats = this.stats != null ? this.stats : new Stats();

            // Block number+hash
            if (remotePeering.getBlock() != null) {
                String[] blockParts = remotePeering.getBlock().split("-");
                if (blockParts.length == 2) {
                    this.stats.setBlockNumber(Integer.parseInt(blockParts[0]));
                    this.stats.setBlockHash(blockParts[1]);
                }
            }

            // Update peer status UP/DOWN
            if ("UP".equalsIgnoreCase(remotePeering.getStatus())) {
                stats.setStatus(Peer.PeerStatus.UP);

                // FIXME: Duniter 1.7 return lastUpTime in ms. Check if this a bug or not
                stats.setLastUpTime((long)Math.round(System.currentTimeMillis() / 1000));
            }
            else {
                stats.setStatus(Peer.PeerStatus.DOWN);
            }

            return this;
        }


        public void path(String path) {
            this.path = path;
        }

        public Peer build() {
            int port = this.port != null ? this.port : 80;
            String api = this.api != null ? this.api : EndpointApi.BASIC_MERKLED_API.label();
            boolean useSsl = this.useSsl != null ? this.useSsl :
                    (port == 443 || EndpointApi.BMAS.label().equals(this.api));
            Peer ep = new Peer(api, dns, ipv4, ipv6, port, useSsl);
            if (StringUtils.isNotBlank(this.epId)) {
                ep.setEpId(this.epId);
            }
            if (StringUtils.isNotBlank(this.currency)) {
                ep.setCurrency(this.currency);
            }
            if (StringUtils.isNotBlank(this.pubkey)) {
                ep.setPubkey(this.pubkey);
            }
            if (StringUtils.isNotBlank(this.hash)) {
                ep.setHash(this.hash);
            }
            if (StringUtils.isNotBlank(this.path)) {
                ep.setPath(this.path);
            }
            // Peering
            if (this.peering != null) {
                ep.setPeering(this.peering);
            }
            // Stats
            if (this.stats != null) {
                ep.setStats(this.stats);
            }
            return ep;
        }

    }

    private String api;
    private String epId;
    private String dns;
    private String ipv4;
    private String ipv6;
    private String path;

    private String url;
    private String pubkey;

    private String host;

    private String hash;
    private String currency;

    private Stats stats = new Stats();
    private Peering peering = new Peering();

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
        this.api = EndpointApi.BASIC_MERKLED_API.label();
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
        this.useSsl = (port == 443 || EndpointApi.BMAS.label().equals(this.api));
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
                (ipv4 != null && InetAddressUtils.isInternetIPv4Address(ipv4) ? ipv4 :
                    (dns != null ? dns :
                        (ipv6 != null ? "[" + ipv6 + "]" : "")));
        // Use local IPv4 if no other host found
        if (StringUtils.isBlank(host) && ipv4 != null && InetAddressUtils.isIPv4Address(ipv4)) {
            host = ipv4;
        }
        String protocol = ((port == 443 || useSsl) ? "https" : "http");
        this.url = protocol + "://" + host + (port != 80 ? (":" + port) : "") + (StringUtils.isNotBlank(path) ? path : "");
    }

    @JsonIgnore
    public String getId() {
        return hash;
    }

    @JsonIgnore
    public void setId(String hash) {
        this.hash  = hash;
    }

    @JsonIgnore
    public String getHost() {
        return this.host; // computed in init()
    }

    @JsonIgnore
    public String getUrl() {
        return this.url; // computed in init()
    }

    public String getApi() {
        return api;
    }

    public void setApi(String api) {
        this.api = api;
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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
        init();
    }

    public String getEpId() {
        return epId;
    }

    public void setEpId(String epId) {
        this.epId = epId;
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

    public Peering getPeering() {
        return peering;
    }

    public void setPeering(Peering peering) {
        this.peering = peering;
    }

    public String toString() {
        StringJoiner joiner = new StringJoiner(" ");
        if (api != null) {
            joiner.add(api);
        }
        if (epId != null) {
            joiner.add(epId);
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
        if (StringUtils.isNotBlank(path)) {
            joiner.add(path);
        }
        return joiner.toString();
    }



    public enum PeerStatus {
        UP,
        DOWN,
        ERROR
    }

    @Data
    @FieldNameConstants
    public static class Peering {

        private Integer version;
        private String signature;
        private Integer blockNumber;
        private String blockHash;
        private String raw;

    }

    @Data
    @FieldNameConstants
    public static class Stats {

        private String software;
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
        private Long lastUpTime;
        private Long firstDownTime;


        @JsonIgnore
        public boolean isReacheable() {
            return status != null && status == PeerStatus.UP;
        }


    }
}

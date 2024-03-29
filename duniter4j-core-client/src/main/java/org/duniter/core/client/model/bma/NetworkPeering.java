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
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.duniter.core.util.CollectionUtils;
import org.duniter.core.util.Preconditions;
import org.duniter.core.util.StringUtils;

import java.io.Serializable;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Stream;

/**
 * Created by eis on 05/02/15.
 */
@Data
@FieldNameConstants
public class NetworkPeering implements Serializable {
    private Integer version;
    private String currency;
    private String block;
    private String signature;
    private String status;
    private String pubkey;

    private String raw;

    public Endpoint[] endpoints;

    public String toString() {
        // Use raw, if exists
        String raw = this.raw != null ? this.raw : toUnsignedRaw();

        // Append signature (if any)
        if (StringUtils.isBlank(signature)) {
            return raw;
        }

        return new StringBuilder()
                .append(raw)
                .append(signature).append("\n")
                .toString();
    }

    public String toUnsignedRaw() {

        StringBuilder sb = new StringBuilder();
        // Version
        sb.append("Version: ").append(Protocol.VERSION).append("\n");
        // Type
        sb.append("Type: ").append(Protocol.TYPE_PEER).append("\n");
        // Type
        sb.append("Currency: ").append(currency).append("\n");
        // PublicKey
        sb.append("PublicKey: ").append(pubkey).append("\n");
        // Block
        sb.append("Block: ").append(block).append("\n");
        // Endpoints
        sb.append("Endpoints:\n");
        if (CollectionUtils.isNotEmpty(endpoints)) {
            Stream.of(endpoints)
                    .filter(Objects::nonNull) // can be null
                    .forEach(ep -> sb.append(ep.toString()).append("\n"));
        }
        return sb.toString();
    }

    public String toSignedRaw() {
        Preconditions.checkNotNull(this.signature, "Invalid peer document. Missing 'signature'");
        return new StringBuilder()
                .append(toUnsignedRaw())
                .append(signature).append("\n")
                .toString();
    }

    @Data
    @FieldNameConstants
    public static class Endpoint implements Serializable {
        public String api;
        public String dns;
        public String ipv4;
        public String ipv6;
        public Integer port;
        public String id;
        public String path;
        public Boolean useSsl;
        public String raw;

        @Override
        public String toString() {
            if (raw != null) return raw;

            StringJoiner joiner = new StringJoiner(" ");
            // API
            if (api != null) {
                joiner.add(api);
            }
            // SSL
            if (useSsl != null && useSsl.booleanValue()) {
                joiner.add("S"); // useSsl ? used by GVA or GVASUB
            }
            // Id (use for WS2P)
            if (StringUtils.isNotBlank(id)) {
                joiner.add(id);
            }
            // DNS
            if (StringUtils.isNotBlank(dns)) {
                joiner.add(dns);
            }
            // IPv4
            if (StringUtils.isNotBlank(ipv4)) {
                joiner.add(ipv4);
            }
            // IPv6
            if (StringUtils.isNotBlank(ipv6)) {
                joiner.add(ipv6);
            }
            // Port
            if (port != null) {
                joiner.add(String.valueOf(port));
            }
            // path
            if (StringUtils.isNotBlank(path)) {
                joiner.add(path);
            }
            return joiner.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Endpoint) {
                return Objects.equals(((Endpoint) obj).api, api)
                        && Objects.equals(((Endpoint) obj).useSsl, useSsl)
                        && Objects.equals(((Endpoint) obj).id, id)
                        && Objects.equals(((Endpoint) obj).dns, dns)
                        && Objects.equals(((Endpoint) obj).ipv4, ipv4)
                        && Objects.equals(((Endpoint) obj).ipv6, ipv6)
                        && Objects.equals(((Endpoint) obj).port, port)
                        && Objects.equals(((Endpoint) obj).path, path);
            }
            return super.equals(obj);
        }
    }


}

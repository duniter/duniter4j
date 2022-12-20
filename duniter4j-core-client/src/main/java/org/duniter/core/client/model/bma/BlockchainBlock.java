package org.duniter.core.client.model.bma;

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



import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * A block from the blockchain.
 * 
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 * @since 1.0
 */
@JsonIgnoreProperties(ignoreUnknown=true)
@Data
@FieldNameConstants
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BlockchainBlock implements Serializable {

    private static final long serialVersionUID = -5598140972293452669L;
    
    private Integer version;
    private Long nonce;
    private Integer number;
    private Integer powMin;
	private Long time;
    private Long medianTime;
    private Integer membersCount;
    private BigInteger monetaryMass;
    private Integer unitbase;
    private Integer issuersCount;
    private Integer issuersFrame;
    private Integer issuersFrameVar;
    private String currency;
    private String issuer;
    private String hash;
    private String parameters;
    private String previousHash;
    private String previousIssuer;
    private String innerHash;
    private BigInteger dividend;
    private Identity[] identities;
    private Joiner[] joiners;
    private Joiner[] leavers;
    private Joiner[] actives;
    private Revoked[] revoked;
    private String[] excluded;
    private Certification[] certifications;
    private Transaction[] transactions;
    private String signature;
    private String raw;

    @JsonGetter("inner_hash")
    public String getInnerHash() {
        return innerHash;
    }

    @JsonSetter("inner_hash")
    public void setInnerHash(String inner_hash) {
        this.innerHash = inner_hash;
    }


    public String toString() {
        String s = "version=" + version;
        s += "\nnonce=" + nonce;
        s += "\ninnerHash=" + innerHash;
        s += "\nnumber=" + number;
        s += "\npowMin" + powMin;
        s += "\ntime=" + time;
        s += "\nmedianTime=" + medianTime;
        s += "\nmembersCount=" + membersCount;
        s += "\nmonetaryMass=" + monetaryMass;
        s += "\ncurrency=" + currency;
        s += "\nissuer=" + issuer;
        s += "\nsignature=" + signature;
        s += "\nhash=" + hash;
        s += "\nparameters=" + parameters;
        s += "\npreviousHash=" + previousHash;
        s += "\npreviousIssuer=" + previousIssuer;
        s += "\ndividend=" + dividend;
        s += "\nmembersChanges:";
        s += "\nidentities:";
        if (identities != null) {
            for (Identity i : identities) {
                s += "\n\t" + i.toString();
            }
        }
        s += "\njoiners:";
        if (joiners != null) {
            for (Joiner j : joiners) {
                s += "\n\t" + j.toString();
            }
        }
        s += "\nactives:";
        if (actives != null) {
            for (Joiner a : actives) {
                s += "\n\t" + a.toString();
            }
        }
        s += "\nleavers:";
        if (leavers != null) {
            for (Joiner l : leavers) {
                s += "\n\t" + l.toString();
            }
        }
        s += "\nrevoked:";
        if (leavers != null) {
            for (Revoked r : revoked) {
                s += "\n\t" + r.toString();
            }
        }
        s += "\nexcluded:";
        if (excluded != null) {
            for (String e : excluded) {
                s += "\n\t" + e;
            }
        }
        s += "\ncertifications:";
        if (certifications != null) {
            for (Certification c : certifications) {
                s += "\n\t" + c.toString();
            }
        }

        return s;
    }

    @JsonDeserialize
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Identity implements Serializable {

        private static final long serialVersionUID = 8080689271400316984L;

        private String publicKey;
        private String signature;
        private String blockUid;
        private String userId;


        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder()
                    .append(publicKey)
                    .append(":").append(signature)
                    .append(":").append(blockUid)
                    .append("").append(userId);

            return sb.toString();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Joiner implements Serializable {

        private static final long serialVersionUID = 8448049949323699700L;

        private String publicKey;
        private String signature;
        private String userId;
        private String membershipBlockUid;
        private String idtyBlockUid;

        @Override
        public String toString() {

            StringBuilder sb = new StringBuilder()
                    .append(publicKey)
                    .append(":").append(signature)
                    .append(":").append(membershipBlockUid)
                    .append(":").append(idtyBlockUid)
                    .append(":").append(userId);

            return sb.toString();
        }
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Revoked implements Serializable {

        private String pubkey;
        private String signature;

        @Override
        public String toString() {

            StringBuilder sb = new StringBuilder()
                    .append(pubkey)
                    .append(":").append(signature);

            return sb.toString();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Certification implements Serializable {

        private String fromPubkey;
        private String toPubkey;
        private String blockId;
        private String signature;

        @Override
        public String toString() {

            StringBuilder sb = new StringBuilder()
                    .append(fromPubkey)
                    .append(":").append(toPubkey)
                    .append(":").append(blockId)
                    .append(":").append(signature);

            return sb.toString();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Transaction implements Serializable {
        private static final long serialVersionUID = 1L;

        private String[] signatures;
        private int version;
        private String currency;
        private String[] issuers;
        private String[] inputs;
        private String[] unlocks;
        private String[] outputs;
        private long time;
        private long locktime;
        private String blockstamp;
        private long blockstampTime;
        private String comment;
        private long blockNumber;

        @JsonGetter("block_number")
        public long getBlockNumber() {
            return blockNumber;
        }

        @JsonSetter("block_number")
        public void setBlockNumber(long blockNumber) {
            this.blockNumber = blockNumber;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("\nsignatures:");
            if (signatures != null) {
                for (String e : signatures) {
                    sb.append("\n\t").append(e);
                }
            }
            sb.append("\nversion: ").append(version);
            sb.append("\ncurrency: ").append(currency);
            sb.append("\nissuers:");
            if (issuers != null) {
                for (String e : issuers) {
                    sb.append("\n\t").append(e);
                }
            }
            sb.append("\ninputs:");
            if (inputs != null) {
                for (String e : inputs) {
                    sb.append("\n\t").append(e);
                }
            }
            sb.append("\nunlocks:");
            if (unlocks != null) {
                for (String e : unlocks) {
                    sb.append("\n\t").append(e);
                }
            }
            sb.append("\noutputs:");
            if (outputs != null) {
                for (String e : outputs) {
                    sb.append("\n\t").append(e);
                }
            }
            return sb.toString();
        }
    }
}

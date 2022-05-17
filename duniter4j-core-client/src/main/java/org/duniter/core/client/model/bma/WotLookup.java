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
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.io.Serializable;
@Data
@FieldNameConstants
public class WotLookup {

    private boolean partial;
    private Result[] results;

    public String toString() {
        String s = "";
        for (Result result : results) {
            s = "pubkey=" + result.pubkey;
            for (Uid uid : result.uids) {
                s += "\nuid=" + uid.uid;
                s += "\ntimestamp=" + uid.meta.timestamp;
                s += "self=" + uid.self;
            }
        }
        return s;
    }

    @Data
    @FieldNameConstants
    public static class Result implements Serializable {

        private static final long serialVersionUID = -39452685440482106L;

        private String pubkey;
        private Uid[] uids;
        private SignedSignature[] signed;

    }

    @Data
    @FieldNameConstants
    public static class Uid {

        public interface JsonFields {
            String REVOCATION_SIG="revocation_sig";
            String REVOKED_ON="revoked_on";
        }
        private String uid;
        private Meta meta;
        private String self;
        private Boolean revoked;
        private Long revokedOn;
        private String revocationSig;
        private OtherSignature[] others;


        @JsonGetter(JsonFields.REVOCATION_SIG)
        public String getRevocationSig() {
            return revocationSig;
        }

        @JsonSetter(JsonFields.REVOCATION_SIG)
        public void setRevocationSig(String revocationSig) {
            this.revocationSig = revocationSig;
        }

        @JsonGetter(JsonFields.REVOKED_ON)
        public Long getRevokedOn() {
            return revokedOn;
        }

        @JsonSetter(JsonFields.REVOKED_ON)
        public void setRevokedOn(Long revokedOn) {
            this.revokedOn = revokedOn;
        }
    }

    @Data
    @FieldNameConstants
    public static class Meta implements Serializable {

        public interface JsonFields {
            String BLOCK_HASH="block_hash";
            String BLOCK_NUMBER="block_number";
        }

        private String timestamp;
        private String blockHash;
        private Long blockNumber;


        @JsonGetter(JsonFields.BLOCK_HASH)
        public String getBlockHash() {
            return blockHash;
        }

        @JsonSetter(JsonFields.BLOCK_HASH)
        public void setBlockHash(String blockHash) {
            this.blockHash = blockHash;
        }
        @JsonGetter(JsonFields.BLOCK_NUMBER)
        public Long getBlockNumber() {
            return blockNumber;
        }

        @JsonSetter(JsonFields.BLOCK_NUMBER)
        public void setBlockNumberH(Long blockNumber) {
            this.blockNumber = blockNumber;
        }

    }

    @Data
    @FieldNameConstants
    public static class OtherSignature {

        private String pubkey;
        private Meta meta;
        private String signature;
        private String[] uids;
        private boolean isMember;
        private boolean wasMember;
    }

    @Data
    @FieldNameConstants
    public static class SignedSignature {
        public interface JsonFields {
            String CERT_TIME="cert_time";
        }

        private String uid;
        private String pubkey;
        private Meta meta;
        private CertTime cerTime;
        private String signature;
        private boolean isMember;
        private boolean wasMember;

        @JsonGetter(JsonFields.CERT_TIME)
        public CertTime getCerTime() {
            return cerTime;
        }

        @JsonSetter(JsonFields.CERT_TIME)
        public void setCerTime(CertTime cerTime) {
            this.cerTime = cerTime;
        }
    }

    @Data
    @FieldNameConstants
    public static class CertTime implements Serializable {
        public interface JsonFields {
            String BLOCK_HASH="block_hash";
        }

        private Long block;
        private String blockHash;

        @JsonGetter(JsonFields.BLOCK_HASH)
        public String getBlockHash() {
            return blockHash;
        }

        @JsonSetter(JsonFields.BLOCK_HASH)
        public void setBlockHash(String blockHash) {
            this.blockHash = blockHash;
        }
    }
}

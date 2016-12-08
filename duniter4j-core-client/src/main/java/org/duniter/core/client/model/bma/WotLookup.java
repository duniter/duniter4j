package org.duniter.core.client.model.bma;

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


import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.io.Serializable;

public class WotLookup {

    private boolean partial;
    private Result[] results;

    public boolean isPartial() {
        return partial;
    }

    public void setPartial(boolean partial) {
        this.partial = partial;
    }

    public Result[] getResults() {
        return results;
    }

    public void setResults(Result[] results) {
        this.results = results;
    }

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

    public static class Result implements Serializable {

        private static final long serialVersionUID = -39452685440482106L;

        private String pubkey;
        private Uid[] uids;
        private SignedSignature[] signed;

        public String getPubkey() {
            return pubkey;
        }

        public void setPubkey(String pubkey) {
            this.pubkey = pubkey;
        }

        public Uid[] getUids() {
            return uids;
        }

        public void setUids(Uid[] uids) {
            this.uids = uids;
        }

        public SignedSignature[] getSigned() {
            return signed;
        }

        public void setSigned(SignedSignature[] signed) {
            this.signed = signed;
        }
    }

    public static class Uid {

        private String uid;
        private Meta meta;
        private String self;
        private Boolean revoked;
        private Long revokedOn;
        private String revocationSig;
        private OtherSignature[] others;

        public Uid(){

        }

        public String getUid() {
            return uid;
        }

        public void setUid(String uid) {
            this.uid = uid;
        }

        public Meta getMeta() {
            return meta;
        }

        public void setMeta(Meta meta) {
            this.meta = meta;
        }

        public String getSelf() {
            return self;
        }

        public void setSelf(String self) {
            this.self = self;
        }

        public OtherSignature[] getOthers() {
            return others;
        }

        public void setOthers(OtherSignature[] others) {
            this.others = others;
        }

        public Boolean getRevoked() {
            return revoked;
        }

        public void setRevoked(Boolean revoked) {
            this.revoked = revoked;
        }

        @JsonGetter("revocation_sig")
        public String getRevocationSig() {
            return revocationSig;
        }

        @JsonSetter("revocation_sig")
        public void setRevocationSig(String revocationSig) {
            this.revocationSig = revocationSig;
        }

        @JsonGetter("revoked_on")
        public Long getRevokedOn() {
            return revokedOn;
        }

        @JsonSetter("revoked_on")
        public void setRevokedOn(Long revokedOn) {
            this.revokedOn = revokedOn;
        }
    }


    public static class Meta implements Serializable {
        private String timestamp;
        private String blockHash;
        private Long blockNumber;

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        @JsonGetter("block_hash")
        public String getBlockHash() {
            return blockHash;
        }

        @JsonSetter("block_hash")
        public void setBlockHash(String blockHash) {
            this.blockHash = blockHash;
        }
        @JsonGetter("block_number")
        public Long getBlockNumber() {
            return blockNumber;
        }

        @JsonSetter("block_number")
        public void setBlockNumberH(Long blockNumber) {
            this.blockNumber = blockNumber;
        }

    }

    public static class OtherSignature {

        private String pubkey;
        private Meta meta;
        private String signature;
        private String[] uids;
        private boolean isMember;
        private boolean wasMember;

        public String getPubkey() {
            return pubkey;
        }

        public void setPubkey(String pubkey) {
            this.pubkey = pubkey;
        }

        public Meta getMeta() {
            return meta;
        }

        public void setMeta(Meta meta) {
            this.meta = meta;
        }

        public String getSignature() {
            return signature;
        }

        public void setSignature(String signature) {
            this.signature = signature;
        }

        public String[] getUids() {
            return uids;
        }

        public void setUids(String[] uids) {
            this.uids = uids;
        }

        @JsonGetter("isMember")
        public boolean isMember() {
            return isMember;
        }

        @JsonSetter("isMember")
        public void setMember(boolean member) {
            isMember = member;
        }

        @JsonGetter("wasMember")
        public boolean wasMember() {
            return wasMember;
        }

        public void setWasMember(boolean wasMember) {
            this.wasMember = wasMember;
        }
    }

    public static class SignedSignature {

        private String uid;
        private String pubkey;
        private Meta meta;
        private CertTime cerTime;
        private String signature;
        private boolean isMember;
        private boolean wasMember;

        public String getUid() {
            return uid;
        }

        public void setUid(String uid) {
            this.uid = uid;
        }

        public String getPubkey() {
            return pubkey;
        }

        public void setPubkey(String pubkey) {
            this.pubkey = pubkey;
        }

        public Meta getMeta() {
            return meta;
        }

        public void setMeta(Meta meta) {
            this.meta = meta;
        }

        public String getSignature() {
            return signature;
        }

        public void setSignature(String signature) {
            this.signature = signature;
        }

        @JsonGetter("isMember")
        public boolean isMember() {
            return isMember;
        }

        public void setIsMember(boolean isMember) {
            this.isMember = isMember;
        }

        @JsonGetter("wasMember")
        public boolean wasMember() {
            return wasMember;
        }

        public void setWasMember(boolean wasMember) {
            this.wasMember = wasMember;
        }

        @JsonGetter("cert_time")
        public CertTime getCerTime() {
            return cerTime;
        }

        @JsonSetter("cert_time")
        public void setCerTime(CertTime cerTime) {
            this.cerTime = cerTime;
        }
    }

    public static class CertTime implements Serializable {
        private Long block;
        private String blockHash;

        public Long getBlock() {
            return block;
        }

        public void setBlock(Long block) {
            this.block = block;
        }

        @JsonGetter("block_hash")
        public String getBlockHash() {
            return blockHash;
        }

        @JsonSetter("block_hash")
        public void setBlockHash(String blockHash) {
            this.blockHash = blockHash;
        }
    }
}

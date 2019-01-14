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

import java.io.Serializable;
import java.math.BigInteger;

/**
 * A block from the blockchain.
 * 
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 * @since 1.0
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class BlockchainBlock implements Serializable {

    public static final String PROPERTY_NUMBER = "number";
    public static final String PROPERTY_HASH = "hash";
    public static final String PROPERTY_DIVIDEND = "dividend";
    public static final String PROPERTY_IDENTITIES = "identities";
    public static final String PROPERTY_JOINERS = "joiners";
    public static final String PROPERTY_ACTIVES = "actives";
    public static final String PROPERTY_LEAVERS = "leavers";
    public static final String PROPERTY_REVOKED = "revoked";
    public static final String PROPERTY_EXCLUDED = "excluded";
    public static final String PROPERTY_MEDIAN_TIME = "medianTime";


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


//  raw": "Version: 1\nType: Block\nCurrency: zeta_brouzouf\nNonce: 8233\nNumber: 1\nDate: 1416589860\nConfirmedDate: 1416589860\nIssuer: HnFcSms8jzwngtVomTTnzudZx7SHUQY8sVE1y8yBmULk\nPreviousHash: 00006CD96A01378465318E48310118AC6B2F3625\nPreviousIssuer: HnFcSms8jzwngtVomTTnzudZx7SHUQY8sVE1y8yBmULk\nMembersCount: 4\nIdentities:\nJoiners:\nActives:\nLeavers:\nExcluded:\nCertifications:\nTransactions:\n"
    private String raw;

    public Integer getVersion() {
        return version;
    }
    public void setVersion(Integer version) {
        this.version = version;
    }
    public Long getNonce() {
        return nonce;
    }
    public void setNonce(Long nonce) {
        this.nonce = nonce;
    }

    public Integer getPowMin() {
        return powMin;
    }

    public void setPowMin(Integer powMin) {
        this.powMin = powMin;
    }

    public Integer getNumber() {
		return number;
	}
	public void setNumber(Integer number) {
		this.number = number;
	}
    public Long getTime() {
        return time;
    }
    public void setTime(Long time) {
        this.time = time;
    }
    public Long getMedianTime() {
        return medianTime;
    }
    public void setMedianTime(Long medianTime) {
        this.medianTime = medianTime;
    }
    public Integer getMembersCount() {
        return membersCount;
    }
    public void setMembersCount(Integer membersCount) {
        this.membersCount = membersCount;
    }

    public BigInteger getMonetaryMass() {
        return monetaryMass;
    }

    public void setMonetaryMass(BigInteger monetaryMass) {
        this.monetaryMass = monetaryMass;
    }

    public String getCurrency() {
        return currency;
    }
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    public String getIssuer() {
        return issuer;
    }
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
    public String getSignature() {
        return signature;
    }
    public void setSignature(String signature) {
        this.signature = signature;
    }
    public String getHash() {
        return hash;
    }
    public void setHash(String hash) {
        this.hash = hash;
    }
    public String getParameters() {
        return parameters;
    }
    public void setParameters(String parameters) {
        this.parameters = parameters;
    }
    public String getPreviousHash() {
        return previousHash;
    }
    public void setPreviousHash(String previousHash) {
        this.previousHash = previousHash;
    }
    public String getPreviousIssuer() {
        return previousIssuer;
    }
    public void setPreviousIssuer(String previousIssuer) {
        this.previousIssuer = previousIssuer;
    }
    public BigInteger getDividend() {
        return dividend;
    }
    public void setDividend(BigInteger dividend) {
        this.dividend = dividend;
    }
    public Identity[] getIdentities() {
        return identities;
    }
    public void setIdentities(Identity[] identities) {
        this.identities = identities;
    }
    public Joiner[] getJoiners() {
        return joiners;
    }
    public void setJoiners(Joiner[] joiners) {
        this.joiners = joiners;
    }

    public Integer getUnitbase() {
        return unitbase;
    }

    public void setUnitbase(Integer unitbase) {
        this.unitbase = unitbase;
    }

    @JsonGetter("inner_hash")
    public String getInnerHash() {
        return innerHash;
    }

    @JsonSetter("inner_hash")
    public void setInnerHash(String inner_hash) {
        this.innerHash = inner_hash;
    }

    public Joiner[] getLeavers() {
        return leavers;
    }

    public void setLeavers(Joiner[] leavers) {
        this.leavers = leavers;
    }

    public Joiner[] getActives() {
        return actives;
    }

    public void setActives(Joiner[] actives) {
        this.actives = actives;
    }

    public Revoked[] getRevoked() {
        return revoked;
    }

    public void setRevoked(Revoked[] revoked) {
        this.revoked = revoked;
    }

    public String[] getExcluded() {
        return excluded;
    }

    public void setExcluded(String[] excluded) {
        this.excluded = excluded;
    }

    public Certification[] getCertifications() {
        return certifications;
    }

    public void setCertifications(Certification[] certifications) {
        this.certifications = certifications;
    }

    public Transaction[] getTransactions() {
        return transactions;
    }

    public void setTransactions(Transaction[] transactions) {
        this.transactions = transactions;
    }

    public Integer getIssuersCount() {
        return issuersCount;
    }

    public void setIssuersCount(Integer issuersCount) {
        this.issuersCount = issuersCount;
    }

    public Integer getIssuersFrame() {
        return issuersFrame;
    }

    public void setIssuersFrame(Integer issuersFrame) {
        this.issuersFrame = issuersFrame;
    }

    public Integer getIssuersFrameVar() {
        return issuersFrameVar;
    }

    public void setIssuersFrameVar(Integer issuersFrameVar) {
        this.issuersFrameVar = issuersFrameVar;
    }

    public String getRaw() {
        return raw;
    }

    public void setRaw(String raw) {
        this.raw = raw;
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
    public static class Identity implements Serializable {

        private static final long serialVersionUID = 8080689271400316984L;

        private String publicKey;

        private String signature;

        private String blockUid;

        private String userId;

        public String getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(String publicKey) {
            this.publicKey = publicKey;
        }

        public String getSignature() {
            return signature;
        }

        public void setSignature(String signature) {
            this.signature = signature;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String uid) {
            this.userId = uid;
        }


        public String getBlockUid() {
            return blockUid;
        }

        public void setBlockUid(String blockUid) {
            this.blockUid = blockUid;
        }

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

    public static class Joiner extends Identity {

        private static final long serialVersionUID = 8448049949323699700L;

        private String publicKey;

        private String signature;

        private String userId;

        private String membershipBlockUid;

        private String idtyBlockUid;

        public String getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(String pubkey) {
            this.publicKey = pubkey;
        }

        public String getSignature() {
            return signature;
        }

        public void setSignature(String signature) {
            this.signature = signature;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String uid) {
            this.userId = uid;
        }

        public String getMembershipBlockUid() {
            return membershipBlockUid;
        }

        public void setMembershipBlockUid(String membershipBlockUid) {
            this.membershipBlockUid = membershipBlockUid;
        }

        public String getIdtyBlockUid() {
            return idtyBlockUid;
        }

        public void setIdtyBlockUid(String idtyBlockUid) {
            this.idtyBlockUid = idtyBlockUid;
        }

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


    public static class Revoked implements Serializable {
        private String pubkey;
        private String signature;


        public String getPubkey() {
            return pubkey;
        }

        public void setPubkey(String pubkey) {
            this.pubkey = pubkey;
        }
        public String getSignature() {
            return signature;
        }
        public void setSignature(String signature) {
            this.signature = signature;
        }

        @Override
        public String toString() {

            StringBuilder sb = new StringBuilder()
                    .append(pubkey)
                    .append(":").append(signature);

            return sb.toString();
        }
    }

    public static class Certification implements Serializable {
        private String fromPubkey;
        private String toPubkey;
        private String blockId;
        private String signature;

        public String getFromPubkey() {
            return fromPubkey;
        }

        public void setFromPubkey(String fromPubkey) {
            this.fromPubkey = fromPubkey;
        }

        public String getToPubkey() {
            return toPubkey;
        }

        public void setToPubkey(String toPubkey) {
            this.toPubkey = toPubkey;
        }

        public String getSignature() {
            return signature;
        }
        public void setSignature(String signature) {
            this.signature = signature;
        }

        public String getBlockId() {
            return blockId;
        }

        public void setBlockId(String blockId) {
            this.blockId = blockId;
        }

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

        public String[] getSignatures() {
            return signatures;
        }

        public void setSignatures(String[] signatures) {
            this.signatures = signatures;
        }

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public String[] getIssuers() {
            return issuers;
        }

        public void setIssuers(String[] issuers) {
            this.issuers = issuers;
        }

        public String[] getInputs() {
            return inputs;
        }

        public void setInputs(String[] inputs) {
            this.inputs = inputs;
        }

        public String[] getUnlocks() {
            return unlocks;
        }

        public void setUnlocks(String[] unlocks) {
            this.unlocks = unlocks;
        }

        public String[] getOutputs() {
            return outputs;
        }

        public void setOutputs(String[] outputs) {
            this.outputs = outputs;
        }

        public long getTime() {
            return time;
        }

        public void setTime(long time) {
            this.time = time;
        }

        public long getBlockstampTime() {
            return blockstampTime;
        }

        public void setBlockstampTime(long blockstampTime) {
            this.blockstampTime = blockstampTime;
        }

        public long getLocktime() {
            return locktime;
        }

        public void setLocktime(long locktime) {
            this.locktime = locktime;
        }

        public String getBlockstamp() {
            return blockstamp;
        }

        public void setBlockstamp(String blockstamp) {
            this.blockstamp = blockstamp;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

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

package org.duniter.core.client.model.local;

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

import java.io.Serializable;
import java.util.Collection;

import org.duniter.core.client.model.bma.WotCertification;
import org.duniter.core.util.ObjectUtils;
import org.duniter.core.util.crypto.CryptoUtils;
import org.duniter.core.util.crypto.KeyPair;

/**
 * A wallet is a user account
 * Created by eis on 13/01/15.
 */
public class Wallet extends KeyPair implements LocalEntity<Long>, Serializable {


    private Long id;
    private Long accountId;
    private String currency;
    private String name;
    private Long credit;
    private Identity identity;
    private Double creditAsUD;
    private long blockNumber = -1;
    private long txBlockNumber = -1;
    private Collection<WotCertification> certifications;

    /**
     * Use for UI, when some properties has not been displayed yet
     */
    private boolean isDirty = false;

    public Wallet() {
        super(null, null);
        this.identity = new Identity();
    }

    public Wallet(String currency, String uid, byte[] pubKey, byte[] secKey) {
        super(pubKey, secKey);
        this.currency = currency;
        this.identity = new Identity();
        this.identity.setPubkey(pubKey == null ? null : CryptoUtils.encodeBase58(pubKey));
        this.identity.setUid(uid);
    }

    public Wallet(String currency, String uid, String pubKey, String secKey) {
        super(CryptoUtils.decodeBase58(pubKey), secKey == null ? null : CryptoUtils.decodeBase58(secKey));
        this.currency = currency;
        this.identity = new Identity();
        this.identity.setPubkey(pubKey);
        this.identity.setUid(uid);
    }

    public Wallet(String currency, byte[] secKey, Identity identity) {
        super(CryptoUtils.decodeBase58(identity.getPubkey()), secKey);
        this.currency = currency;
        this.identity = identity;
    }

    public Identity getIdentity() {
        return identity;
    }

    public void setIdentity(Identity identity) {
        this.identity = identity;
    }

    public String getPubKeyHash() {
        return identity.getPubkey();
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public boolean isAuthenticate() {
        return secretKey != null && identity != null && identity.getPubkey() != null;
    }

    public boolean isSelfSend() {
        return identity.getTimestamp() != null;
    }

    public String getCurrencyId() {
        return currency;
    }

    public void setCurrencyId(String currencyId) {
        this.currency = currencyId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getCredit() {
        return credit;
    }

    public void setCredit(Long credit) {
        this.credit = credit;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String toString() {
        return name;
    }

    public String getUid() {
        return identity.getUid();
    }

    public void setUid(String uid) {
        identity.setUid(uid);
    }

    public String getCertTimestamp() {
        return identity.getTimestamp();
    }

    public void setCertTimestamp(String timestamp) {
        identity.setTimestamp(timestamp);
    }

    public void setMember(Boolean isMember) {
        identity.setMember(isMember);
    }

    public Boolean getIsMember() {
        return identity.getIsMember();
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void setDirty(boolean isDirty) {
        this.isDirty = isDirty;
    }

    public Double getCreditAsUD() {
        return creditAsUD;
    }

    public void setCreditAsUD(Double creditAsUD) {
        this.creditAsUD = creditAsUD;
    }

    public Collection<WotCertification> getCertifications() {
        return certifications;
    }

    public void setCertifications(Collection<WotCertification> certifications) {
        this.certifications = certifications;
    }

    public long getBlockNumber() {
        return blockNumber;
    }

    public void setBlockNumber(long blockNumber) {
        this.blockNumber = blockNumber;
    }

    public long getTxBlockNumber() {
        return txBlockNumber;
    }

    public void setTxBlockNumber(long txBlockNumber) {
        this.txBlockNumber = txBlockNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Wallet) {
            return ObjectUtils.equals(id, ((Wallet)o).id)
                    && ObjectUtils.equals(getPubKeyHash(), ((Wallet)o).getPubKeyHash())
                    && ObjectUtils.equals(currency, ((Wallet)o).currency);
        }
        return super.equals(o);
    }
}

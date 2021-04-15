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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.duniter.core.client.model.bma.WotCertification;
import org.duniter.core.model.IEntity;
import org.duniter.core.util.ObjectUtils;
import org.duniter.core.util.crypto.CryptoUtils;
import org.duniter.core.util.crypto.KeyPair;

/**
 * A wallet is a user account
 * Created by eis on 13/01/15.
 */
@Data
public class Wallet extends KeyPair implements IEntity<Long>, Serializable {

    private Long id;
    private Long accountId;
    private String currency;
    private String name;
    private Long credit;
    private Identity identity = new Identity();
    private Double creditAsUD;
    private long blockNumber = -1;
    private long txBlockNumber = -1;
    private Collection<WotCertification> certifications;

    public Wallet() {
        super();
    }

    public Wallet(String currency, String uid, byte[] pubKey, byte[] secKey) {
        super(pubKey, secKey);
        this.currency = currency;
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

    @JsonIgnore
    public String getPubKeyHash() {
        return identity.getPubkey();
    }

    @JsonIgnore
    public boolean isAuthenticate() {
        return secretKey != null && identity != null && identity.getPubkey() != null;
    }

    @JsonIgnore
    public boolean isSelfSend() {
        return identity.getTimestamp() != null;
    }

    @JsonIgnore
    public String getUid() {
        return identity.getUid();
    }

    public void setUid(String uid) {
        identity.setUid(uid);
    }

    @JsonIgnore
    public String getCertTimestamp() {
        return identity.getTimestamp();
    }

    public void setCertTimestamp(String timestamp) {
        identity.setTimestamp(timestamp);
    }

    @JsonIgnore
    public Boolean getIsMember() {
        return identity.getIsMember();
    }

    public void setIsMember(Boolean isMember) {
        identity.setIsMember(isMember);
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

    public String toString() {
        return name != null ? name : identity.getPubkey();
    }

}

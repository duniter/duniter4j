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

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

/**
 * A wallet's movement (DU or transfer)
 * @author
 */
public class Movement implements LocalEntity<Long>, Serializable {

    public static final String PROPERTY_MEDIAN_TIME = "medianTime";
    public static final String PROPERTY_BLOCK_NUMBER= "blockNumber";
    public static final String PROPERTY_BLOCK_HASH = "blockHash";
    public static final String PROPERTY_DIVIDEND = "dividend";
    public static final String PROPERTY_IS_UD = "isUD";
    public static final String PROPERTY_ISSUER = "issuer";
    public static final String PROPERTY_RECIPIENT = "recipient";
    public static final String PROPERTY_AMOUNT = "amount";
    public static final String PROPERTY_UNITBASE = "unitbase";
    public static final String PROPERTY_COMMENT = "comment";
    public static final String PROPERTY_TX_VERSION = "txVersion";

    private Long id;
    private long walletId;
    private Long medianTime;
    private Integer blockNumber;
    private String blockHash;
    private String issuer;
    private String recipient;
    private long amount;
    private int unitbase;
    private long dividend;
    private boolean isUD = false;
    private String comment;
    private String txVersion;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public long getWalletId() {
        return walletId;
    }

    public void setWalletId(long walletId) {
        this.walletId = walletId;
    }

    public int getUnitbase() {
        return unitbase;
    }

    public void setUnitbase(int unitbase) {
        this.unitbase = unitbase;
    }

    public String getBlockHash() {
        return blockHash;
    }

    public void setBlockHash(String blockHash) {
        this.blockHash = blockHash;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public Long getMedianTime() {
        return medianTime;
    }

    public void setMedianTime(Long medianTime) {
        this.medianTime = medianTime;
    }

    public Integer getBlockNumber() {
        return blockNumber;
    }

    public void setBlockNumber(Integer blockNumber) {
        this.blockNumber = blockNumber;
    }

    @JsonIgnore
    public boolean isUD() {
        return isUD;
    }

    public void setUD(boolean isUD) {
        this.isUD = isUD;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @JsonIgnore
    public boolean isValidate() {
        return blockNumber != null;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getRecipient() {
        return recipient;
    }

    public long getDividend() {
        return dividend;
    }

    public void setDividend(long dividend) {
        this.dividend = dividend;
    }
}

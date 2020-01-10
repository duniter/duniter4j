package org.duniter.core.client.model.bma;

import java.io.Serializable;

public class WotPendingMembership implements Serializable {
		private static final long serialVersionUID = 1L;


	public static final String PROPERTY_PUBKEY = "pubkey";
	public static final String PROPERTY_UID = "uid";
	public static final String PROPERTY_VERSION = "version";
	public static final String PROPERTY_CURRENCY = "currency";
	public static final String PROPERTY_MEMBERSHIP = "membership";
	public static final String PROPERTY_BLOCK_NUMBER = "blockNumber";
	public static final String PROPERTY_BLOCK_HASH = "blockHash";
	public static final String PROPERTY_WRITTEN = "written";

	private String pubkey;
	private String uid;
	private String version;
	private String currency;
	private String membership;
	private Integer blockNumber;
	private String blockHash;
	private Boolean written;

	public String getPubkey() {
		return pubkey;
	}
	public void setPubkey(String pubkey) {
		this.pubkey = pubkey;
	}
	public String getUid() {
		return uid;
	}
	public void setUid(String uid) {
		this.uid = uid;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getCurrency() {
		return currency;
	}
	public void setCurrency(String currency) {
		this.currency = currency;
	}
	public String getMembership() {
		return membership;
	}
	public void setMembership(String membership) {
		this.membership = membership;
	}
	public long getBlockNumber() {
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

	public Boolean getWritten() {
		return written;
	}

	public void setWritten(Boolean written) {
		this.written = written;
	}
}
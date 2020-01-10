package org.duniter.core.client.model.bma;

import java.io.Serializable;

public class WotPendingMembership implements Serializable {
		private static final long serialVersionUID = 1L;

		private String pubkey;
		private String uid;
		private String version;
		private String currency;
		private String membership;
		private long blockNumber;
		private String blockHash;

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
		public void setBlockNumber(long blockNumber) {
			this.blockNumber = blockNumber;
		}
		public String getBlockHash() {
			return blockHash;
		}
		public void setBlockHash(String blockHash) {
			this.blockHash = blockHash;
		}
	}
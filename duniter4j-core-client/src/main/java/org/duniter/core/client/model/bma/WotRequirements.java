package org.duniter.core.client.model.bma;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.io.Serializable;

public class WotRequirements implements Serializable {
	private static final long serialVersionUID = 1L;

	private String pubkey;
	private String uid;
	private String sig;
	private Meta meta;
	private String revocation_sig;
	private Boolean revoked;
	private Long revoked_on;
	private Boolean expired;
	private Boolean outdistanced;
	private Boolean isSentry;
	private Boolean wasMember;

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

	public String getSig() {
		return sig;
	}

	public void setSig(String sig) {
		this.sig = sig;
	}

	public Meta getMeta() {
		return meta;
	}

	public void setMeta(Meta meta) {
		this.meta = meta;
	}

	public String getRevocation_sig() {
		return revocation_sig;
	}

	public void setRevocation_sig(String revocation_sig) {
		this.revocation_sig = revocation_sig;
	}

	public Boolean getRevoked() {
		return revoked;
	}

	public void setRevoked(Boolean revoked) {
		this.revoked = revoked;
	}

	public Long getRevoked_on() {
		return revoked_on;
	}

	public void setRevoked_on(Long revoked_on) {
		this.revoked_on = revoked_on;
	}

	public Boolean getExpired() {
		return expired;
	}

	public void setExpired(Boolean expired) {
		this.expired = expired;
	}

	public Boolean getOutdistanced() {
		return outdistanced;
	}

	public void setOutdistanced(Boolean outdistanced) {
		this.outdistanced = outdistanced;
	}

	public Boolean getSentry() {
		return isSentry;
	}

	public void setSentry(Boolean sentry) {
		isSentry = sentry;
	}

	public Boolean getWasMember() {
		return wasMember;
	}

	public void setWasMember(Boolean wasMember) {
		this.wasMember = wasMember;
	}

	public static class Meta implements Serializable {
		private String timestamp;

		public String getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(String timestamp) {
			this.timestamp = timestamp;
		}
	}
}
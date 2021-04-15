package org.duniter.core.client.model.bma;

/*-
 * #%L
 * Duniter4j :: Core Client API
 * %%
 * Copyright (C) 2014 - 2021 Duniter Team
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


	private Certification[] certifications;

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

	public Boolean getIsSentry() {
		return isSentry;
	}

	public void setIsSentry(Boolean sentry) {
		isSentry = sentry;
	}

	public Boolean getWasMember() {
		return wasMember;
	}

	public void setWasMember(Boolean wasMember) {
		this.wasMember = wasMember;
	}


	public Certification[] getCertifications() {
		return certifications;
	}

	public void setCertifications(Certification[] certifications) {
		this.certifications = certifications;
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

	public static class Certification implements Serializable {
		private long timestamp;
		private String from;
		private String to;
		private String sig;
		private long expiresIn;

		public long getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(long timestamp) {
			this.timestamp = timestamp;
		}

		public String getFrom() {
			return from;
		}

		public void setFrom(String from) {
			this.from = from;
		}

		public String getTo() {
			return to;
		}

		public void setTo(String to) {
			this.to = to;
		}

		public String getSig() {
			return sig;
		}

		public void setSig(String sig) {
			this.sig = sig;
		}

		public long getExpiresIn() {
			return expiresIn;
		}

		public void setExpiresIn(long expiresIn) {
			this.expiresIn = expiresIn;
		}
	}
}

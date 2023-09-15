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
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.io.Serializable;

@Data
@FieldNameConstants
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

	private Long membershipPendingExpiresIn;

	private Long membershipExpiresIn;

	private Certification[] certifications;

	@Data
	@FieldNameConstants
	public static class Meta implements Serializable {
		private String timestamp;
	}

	@Data
	@FieldNameConstants
	public static class Certification implements Serializable {
		private long timestamp;
		private String from;
		private String to;
		private String sig;
		private long expiresIn;
	}
}

package org.duniter.core.client.model.bma;

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

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.duniter.core.client.model.BaseIdentity;

import java.io.Serializable;

@Data
@FieldNameConstants
public class BlockchainMemberships extends BaseIdentity {
	private static final long serialVersionUID = -5631089862725952431L;

	private long sigDate;
	private Membership[] memberships;

	@Data
	@FieldNameConstants
	public static class Membership implements Serializable {
		private static final long serialVersionUID = 1L;

		private String version;
		private String currency;
		private String membership;
		private long blockNumber;
		private String blockHash;

	}
}

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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants
public class TxHistory {

    private String currency;

    private String pubkey;

    private History history;

	@Data
	@FieldNameConstants
	public static class History {

		private Movement[] sent;

		private Movement[] received;

		private Movement[] sending;

		private Movement[] receiving;
	}

	@Data
	@FieldNameConstants
	public static class Movement {

		public interface JsonFields {
			String BLOCK_NUMBER = "block_number";
		}

		private String version;

		private String[] issuers;

		private String[] inputs;

		private String[] outputs;

		private String comment;

		private String[] signatures;

		private String hash;

		private int blockNumber;

		private long time;

		@JsonGetter(JsonFields.BLOCK_NUMBER)
		public int getBlockNumber() {
			return blockNumber;
		}

		@JsonSetter(JsonFields.BLOCK_NUMBER)
		public void setBlockNumber(int blockNumber) {
			this.blockNumber = blockNumber;
		}

	}
}

package org.duniter.core.client.model.bma;

/*
 * #%L
 * UCoin Java Client :: Core API
 * %%
 * Copyright (C) 2014 - 2015 EIS
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
import java.util.List;

public class TxSource {

	private String currency;
	
	private String pubkey;
	    
    private Source[] sources;

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getPubkey() {
		return pubkey;
	}

	public void setPubkey(String pubkey) {
		this.pubkey = pubkey;
	}

	public Source[] getSources() {
		return sources;
	}

	public void setSources(Source[] sources) {
		this.sources = sources;
	}

	public class Source implements Serializable, Cloneable {

		private static final long serialVersionUID = 8084087351543574142L;

		private String type;
		private String identifier;
		private String noffset;
		private long amount;
		private int base;


		@Override
		public Object clone() throws CloneNotSupportedException {

			Source clone = (Source)super.clone();
			clone.type = type;
			clone.identifier = identifier;
			clone.noffset = noffset;
			clone.amount = amount;
			clone.base = base;
			return clone;
		}

		/**
		 * Source type : <ul>
		 * <li><code>D</code> : Universal Dividend</li>
		 * <li><code>T</code> : Transaction</li>
		 * </ul>
		 * @return
		 */
		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getIdentifier() {
			return identifier;
		}

		public void setIdentifier(String identifier) {
			this.identifier = identifier;
		}

		public String getNoffset() {
			return noffset;
		}

		public void setNoffset(String noffset) {
			this.noffset = noffset;
		}

		public long getAmount() {
			return amount;
		}

		public void setAmount(long amount) {
			this.amount = amount;
		}

		public int getBase() {
			return base;
		}

		public void setBase(int base) {
			this.base = base;
		}
	}


}

package org.duniter.core.client.model.local;

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


import org.duniter.core.client.model.BasicIdentity;

public class Identity extends BasicIdentity {

    private static final long serialVersionUID = -7451079677730158794L;

    private String timestamp = null;

    private Boolean isMember = null;

    private Long currencyId;

    /**
     * The timestamp value of the signature date (a BLOCK_UID)
     * @return
     */
    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Indicate whether the certification is written in the blockchain or not.
     */
    public Boolean getIsMember() {
        return isMember;
    }

    public void setMember(Boolean isMember) {
        this.isMember = isMember;
    }

    public Long getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(Long currencyId) {
        this.currencyId = currencyId;
    }
}

package org.duniter.core.client.model.local;

/*
 * #%L
 * Duniter4j :: Core API
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


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.duniter.core.client.model.BaseIdentity;

@Data
@FieldNameConstants
public class Identity extends BaseIdentity {

    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Fields extends BaseIdentity.Fields {}

    private String currency;

    // The timestamp value of the signature date (a BLOCK_UID)
    private String timestamp = null;

    // Indicate whether the certification is written in the blockchain or not.
    private Boolean isMember = null;
    private Boolean wasMember = null;

    @JsonIgnore
    public String getTimestamp() {
        return timestamp;
    }

    @JsonIgnore
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @JsonIgnore
    public String getCurrency() {
        return currency;
    }

    @JsonIgnore
    public void setCurrency(String currency) {
        this.currency = currency;
    }
}

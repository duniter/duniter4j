package org.duniter.core.client.model.local;

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

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.duniter.core.client.model.bma.BlockchainParameters;

/**
 * Created by eis on 05/02/15.
 */
@Data
@Builder
@FieldNameConstants
@NoArgsConstructor
@AllArgsConstructor
public class Currency implements ICurrency {
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Fields {}

    private String id;
    private BlockchainParameters parameters;
    private String firstBlockSignature;
    private Integer membersCount;
    private Long dividend;
    private Integer unitbase;

    @JsonIgnore
    @Deprecated
    public Long getLastUD() {
        return dividend;
    }

    @JsonIgnore
    @Deprecated
    public void setLastUD(long lastUD) {
        this.dividend = lastUD;
    }

    public String toString() {
        return id;
    }
}
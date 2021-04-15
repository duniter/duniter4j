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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.duniter.core.model.IEntity;

import java.io.Serializable;

/**
 * A wallet's movement (DU or transfer)
 * @author
 */
@FieldNameConstants
@Data
@Builder
public class Movement implements IEntity<Long>, Serializable {

    private Long id;
    private long walletId;
    private Long medianTime;
    private Integer blockNumber;
    private String blockHash;
    private String issuer;
    private String recipient;
    private long amount;
    private int unitbase;
    private long dividend;
    private boolean isUD = false;
    private String comment;
    private String txVersion;

    @JsonIgnore
    public boolean isUD() {
        return isUD;
    }

    public void setUD(boolean isUD) {
        this.isUD = isUD;
    }

    @JsonIgnore
    public boolean isValidate() {
        return blockNumber != null;
    }

}

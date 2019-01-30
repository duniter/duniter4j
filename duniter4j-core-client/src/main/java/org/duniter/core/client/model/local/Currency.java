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
import org.duniter.core.client.model.bma.BlockchainParameters;

/**
 * Created by eis on 05/02/15.
 */
public class Currency implements LocalEntity<String>, Serializable {

    private String id;
    private Integer membersCount;
    private String firstBlockSignature;
    private Long lastUD;
    private BlockchainParameters parameters;

    public Currency() {
    }

    public Currency(String id,
                    String firstBlockSignature,
                    int membersCount,
                    BlockchainParameters parameters) {
        this.id = id;
        this.firstBlockSignature = firstBlockSignature;
        this.membersCount = membersCount;
        this.parameters = parameters;
    }

    @JsonIgnore
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getMembersCount() {
        return membersCount;
    }

    public String getFirstBlockSignature() {
        return firstBlockSignature;
    }


    public void setMembersCount(Integer membersCount) {
        this.membersCount = membersCount;
    }

    public void setFirstBlockSignature(String firstBlockSignature) {
        this.firstBlockSignature = firstBlockSignature;
    }

    public Long getLastUD() {
        return lastUD;
    }

    public void setLastUD(Long lastUD) {
        this.lastUD = lastUD;
    }

    public BlockchainParameters getParameters() {
        return parameters;
    }

    public void setParameters(BlockchainParameters parameters) {
        this.parameters = parameters;
    }

    public String toString() {
        return id;
    }
}
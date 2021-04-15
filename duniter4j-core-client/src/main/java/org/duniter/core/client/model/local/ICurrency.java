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
import lombok.Data;
import org.duniter.core.client.model.bma.BlockchainParameters;
import org.duniter.core.model.IEntity;

import java.io.Serializable;

/**
 * Created by eis on 05/02/15.
 */
public interface ICurrency extends IEntity<String>, Serializable {

    interface Fields extends IEntity.Fields {
        String FIRST_BLOCK_SIGNATURE = "firstBlockSignature";
        String MEMBER_COUNT = "membersCount";
        String DIVIDEND = "dividend";
        String PARAMETERS = "parameters";
        String UNITBASE = "unitbase";
    }

    BlockchainParameters getParameters();
    void setParameters(BlockchainParameters parameters);

    String getFirstBlockSignature();
    void setFirstBlockSignature(String signature);

    Integer getMembersCount();
    void setMembersCount(Integer memberCount);

    Long getDividend();
    void setDividend(Long dividend);

    Integer getUnitbase();
    void setUnitbase(Integer unitBase);

}
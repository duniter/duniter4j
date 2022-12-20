package org.duniter.core.client.model;

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

import java.io.Serializable;

/**
 * Basic information on a identity.
 * 
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 * @since 1.0
 *
 */
@Data
@FieldNameConstants
public abstract class BaseIdentity implements Serializable {

    private static final long serialVersionUID = 8080689271400316984L;

    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Fields {}

    private String pubkey;

    private String signature;

    private String uid;

    @JsonIgnore
    @Deprecated
    public String getSelf() {
        return signature;
    }

    @JsonIgnore
    @Deprecated
    public void setSelf(String signature) {
        this.signature = signature;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder()
        .append("uid=").append(uid)
        .append(",pubkey=").append(pubkey)
        .append(",signature").append(signature);

        return sb.toString();
    }
}

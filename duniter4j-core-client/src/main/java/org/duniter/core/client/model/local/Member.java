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
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.duniter.core.client.model.BaseIdentity;
import org.duniter.core.model.IEntity;

@Data
@FieldNameConstants
public class Member extends Identity implements IEntity<String> {

    public static class Fields extends Identity.Fields {}

    private static final long serialVersionUID = 8448049949323699700L;

    @JsonIgnore
    public String getId() {
        return getPubkey();
    }

    @JsonIgnore
    public void setId(String pubkey) {
        setPubkey(pubkey);
    }

}

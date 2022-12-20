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
import com.google.common.base.Joiner;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.io.Serializable;

/**
 * Created by eis on 05/02/15.
 */
@Data
@FieldNameConstants
public class NetworkPeers implements Serializable {

    public Peer[] peers;

    public String toString() {
        return Joiner.on(",").join(peers);
    }

    @Data
    @FieldNameConstants
    public static class Peer extends NetworkPeering implements Serializable {

        public interface JsonFields {
            String FIRST_DOWN = "first_down";
            String  LAST_TRY = "last_try";
        }
        public Long firstDown;
        public Long lastTry;
        public String hash;

        @JsonGetter(JsonFields.FIRST_DOWN)
        public Long getFirstDown() {
            return firstDown;
        }

        @JsonSetter(JsonFields.FIRST_DOWN)
        public void setFirstDown(Long firstDown) {
            this.firstDown = firstDown;
        }

        @JsonGetter(JsonFields.LAST_TRY)
        public Long getLastTry() {
            return lastTry;
        }

        @JsonSetter(JsonFields.LAST_TRY)
        public void setLastTry(Long lastTry) {
            this.lastTry = lastTry;
        }

        @JsonIgnore
        public String getHash() {
            return hash;
        }

        @JsonIgnore
        public void setHash(String hash) {
            this.hash = hash;
        }

        @Override
        public String getRaw() {
            return super.getRaw();
        }
    }
}

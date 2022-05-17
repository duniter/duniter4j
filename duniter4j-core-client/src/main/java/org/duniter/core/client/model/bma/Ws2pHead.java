package org.duniter.core.client.model.bma;

/*
 * #%L
 * Duniter4j :: Core Client API
 * %%
 * Copyright (C) 2014 - 2017 EIS
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
import com.google.common.base.Joiner;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.io.Serializable;

/**
 * Created by blavenie on 22/01/19.
 */
@Data
@FieldNameConstants
public class Ws2pHead implements Serializable {

    public Integer version;
    public String pubkey;
    public String block;
    public String ws2pid;
    public String software;
    public String softwareVersion;
    public String powPrefix;

    public String signature;

    public AccessConfig privateConfig = new AccessConfig();
    public AccessConfig publicConfig = new AccessConfig();


    @Data
    @FieldNameConstants
    public static class AccessConfig {
        public boolean useTor;
        private String mode;
    }

    @Override
    public String toString() {
        return Joiner.on(':').skipNulls().join(new Object[]{
                getPrefix(), "HEAD", version, pubkey, block, ws2pid, software, softwareVersion, powPrefix
        });
    }

    @JsonIgnore
    protected String getPrefix() {
        StringBuilder sb = new StringBuilder();
        sb.append("WS2P");

        // Private access
        if (getPrivateConfig() != null) {
            sb.append("O");
            if (getPrivateConfig().isUseTor()) {
                sb.append("T");
            } else {
                sb.append("C");
            }

            if (getPrivateConfig().getMode() != null) {
                switch (getPrivateConfig().getMode()) {
                    case "all":
                        sb.append("A");
                        break;
                    case "mixed":
                        sb.append("M");
                        break;
                    case "strict":
                        sb.append("S");
                        break;
                }
            }
        }

        // Public access
        if (getPublicConfig() != null) {
            sb.append("I");

            if (getPublicConfig().isUseTor()) {
                sb.append("T");
            }
            else {
                sb.append("C");
            }
        }
        return sb.toString();
    }
}
package org.duniter.core.client.model.bma;

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


import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.duniter.core.client.model.local.Identity;

import java.io.Serializable;

/**
 * A list of certifications done to user, or by user
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 * @since 1.0
 *
 */
@Data
@FieldNameConstants
public class WotCertification implements Serializable{
    
    private static final long serialVersionUID = 8568496827055074607L;

    private String pubkey;
    private String uid;
    private String sigDate;
    private boolean isMember;
    private Certification[] certifications;


    @Data
    @FieldNameConstants
    public static class Certification extends Identity {

        private static final long serialVersionUID = 2204517069552693026L;

        public interface JsonFields {
            String CERT_TIME = "cert_time";
        }
        private CertTime certTime;

        private String sigDate;

        /**
         * Indicate whether the certification is written in the blockchain or not.
         */
        private Written written;

        private boolean wasMember;

        @JsonGetter(JsonFields.CERT_TIME)
        public CertTime getCertTime() {
            return certTime;
        }

        @JsonSetter(JsonFields.CERT_TIME)
        public void setCertTime(CertTime certTime) {
            this.certTime = certTime;
        }

    }

    @Data
    @FieldNameConstants
    public static class CertTime implements Serializable {

        private static final long serialVersionUID = -358639516878884523L;

        private int block = -1;
        private long medianTime = -1;

    }

    @Data
    @FieldNameConstants
    public static class Written implements Serializable{

        private long number = -1;
        private String hash = "";

    }
}

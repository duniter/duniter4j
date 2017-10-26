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
import org.duniter.core.client.model.local.Identity;

import java.io.Serializable;

/**
 * A list of certifications done to user, or by user
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 * @since 1.0
 *
 */
public class WotCertification implements Serializable{
    
    private static final long serialVersionUID = 8568496827055074607L;

    private String pubkey;

    private String uid;

    private String sigDate;

    private boolean isMember;

    private Certification[] certifications;

    public Certification[] getCertifications() {
        return certifications;
    }

    public void setCertifications(Certification[] certifications) {
        this.certifications = certifications;
    }

    public String getPubkey() {
        return pubkey;
    }

    public void setPubkey(String pubkey) {
        this.pubkey = pubkey;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    @JsonGetter("is_member")
    public boolean isMember() {
        return isMember;
    }

    public void setIsMember(boolean isMember) {
        this.isMember = isMember;
    }

    public String getSigDate() {
        return sigDate;
    }

    public void setSigDate(String sigDate) {
        this.sigDate = sigDate;
    }

    public static class Certification extends Identity {

        private static final long serialVersionUID = 2204517069552693026L;

        private CertTime certTime;

        private String sigDate;

        /**
         * Indicate whether the certification is written in the blockchain or not.
         */
        private Written written;

        private boolean wasMember;

        @JsonGetter("cert_time")
        public CertTime getCertTime() {
            return certTime;
        }

        @JsonSetter("cert_time")
        public void setCertTime(CertTime certTime) {
            this.certTime = certTime;
        }

        /**
         * Indicate whether the certification is written in the blockchain or not.
         */
        public Written getWritten() {
            return written;
        }

        public void setWritten(Written written) {
            this.written = written;
        }

        public String getSigDate() {
            return sigDate;
        }

        public void setSigDate(String sigDate) {
            this.sigDate = sigDate;
        }

        @JsonGetter("wasMember")
        public boolean wasMember() {
            return wasMember;
        }

        public void setWasMember(boolean wasMember) {
            this.wasMember = wasMember;
        }
    }

    public static class CertTime implements Serializable {

        private static final long serialVersionUID = -358639516878884523L;

        private int block = -1;

        private long medianTime = -1;

        public int getBlock() {
            return block;
        }

        public void setBlock(int block) {
            this.block = block;
        }

        public long getMedianTime() {
            return medianTime;
        }

        public void setMedianTime(long medianTime) {
            this.medianTime = medianTime;
        }

    }

    public static class Written implements Serializable{

        private long number = -1;

        private String hash = "";

        public long getNumber() {
            return number;
        }

        public void setNumber(long number) {
            this.number = number;
        }

        public String getHash() {
            return hash;
        }

        public void setHash(String hash) {
            this.hash = hash;
        }
    }
}

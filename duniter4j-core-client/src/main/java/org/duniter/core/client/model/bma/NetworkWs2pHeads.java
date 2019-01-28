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

import java.io.Serializable;

/**
 * Created by blavenie on 22/01/19.
 */
public class NetworkWs2pHeads {

    public NetworkWs2pHeads.Head[] heads;

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(NetworkWs2pHeads.Head head : heads) {
            sb.append(head.toString()).append("\n");
        }
        return sb.toString();
    }

    public static class Head implements Serializable {
        public Ws2pHead message;
        public String sig;
        public String messageV2;
        public String sigV2;
        public Integer step;

        public Ws2pHead getMessage() {
            return message;
        }

        public void setMessage(Ws2pHead message) {
            this.message = message;
        }

        public String getSig() {
            return sig;
        }

        public void setSig(String sig) {
            this.sig = sig;
        }

        public String getMessageV2() {
            return messageV2;
        }

        public void setMessageV2(String messageV2) {
            this.messageV2 = messageV2;
        }

        public String getSigV2() {
            return sigV2;
        }

        public void setSigV2(String sigV2) {
            this.sigV2 = sigV2;
        }

        public Integer getStep() {
            return step;
        }

        public void setStep(Integer step) {
            this.step = step;
        }

        @Override
        public String toString() {
            String s = "message=" + message + "\n" +
                    "sig=" + sig+ "\n" +
                    "step=" + step;
            return s;
        }
    }
}
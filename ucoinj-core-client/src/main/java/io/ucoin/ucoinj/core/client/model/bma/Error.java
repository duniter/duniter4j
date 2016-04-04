package io.ucoin.ucoinj.core.client.model.bma;

import java.io.Serializable;

/**
 * Created by blavenie on 31/03/16.
 */
public class Error implements Serializable {

    private static final long serialVersionUID = -5598140972293478469L;

    private int ucode;
    private String message;

    public int getUcode() {
        return ucode;
    }

    public void setUcode(int ucode) {
        this.ucode = ucode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String toString() {
        return "ucode=" + ucode
         + "\nmessage=" + message;
    }
}

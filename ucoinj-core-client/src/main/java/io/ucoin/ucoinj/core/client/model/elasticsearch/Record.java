package io.ucoin.ucoinj.core.client.model.elasticsearch;

/**
 * Created by blavenie on 01/03/16.
 */
public class Record {

    public static final String PROPERTY_ISSUER="issuer";
    public static final String PROPERTY_HASH="hash";
    public static final String PROPERTY_SIGNATURE="signature";

    private String issuer;
    private String hash;
    private String signature;

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}

package org.duniter.elasticsearch.model;

import java.io.Serializable;

public class Peer implements Serializable {

    private String currency;
    private String host;
    private int port;
    private String path;
    private String url;

    public Peer() {
        // default constructor, need for de-serialization
    }

    public Peer(String host, int port) {
        this(host, port, null);
    }

    public Peer(String host, int port, String path) {
        this.host = host;
        this.port = port;
        this.url = initUrl(host, port, path);
        this.path = path;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUrl() {
        return url;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setPort(int port) {
        this.port = port;
        this.url = initUrl(host, port, path);
    }

    public void setHost(String host) {
        this.host = host;
        this.url = initUrl(host, port, path);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
        this.url = initUrl(host, port, path);
    }

    public String toString() {
        return new StringBuilder().append(host)
                .append(":")
                .append(port)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (currency != null && o instanceof Peer) {
            if (!currency.equals(((Peer) o).getCurrency())) {
                return false;
            }
            if (!getUrl().equals(((Peer) o).getUrl())) {
                return false;
            }
        }
        return super.equals(o);
    }

    /* -- Internal methods -- */

    protected String initUrl(String host, int port, String path) {
        return String.format("%s://%s:%s%s",
                port == 443 ? "https" : "http",
                host, port,
                (path != null) ? path : "");
    }
}

package org.duniter.elasticsearch.model;

import org.duniter.core.client.model.elasticsearch.Record;

public class SynchroExecution extends Record {

    public static final String PROPERTY_CURRENCY = "currency";
    public static final String PROPERTY_PEER = "peer";
    public static final String PROPERTY_API = "api";
    public static final String PROPERTY_RESULT = "result";
    public static final String PROPERTY_EXECUTION_TIME = "executionTime";


    private String currency;
    private String peer;
    private String api;
    private Long executionTime;
    private SynchroResult result;

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPeer() {
        return peer;
    }

    public void setPeer(String peer) {
        this.peer = peer;
    }

    public SynchroResult getResult() {
        return result;
    }

    public void setResult(SynchroResult result) {
        this.result = result;
    }

    public String getApi() {
        return api;
    }

    public void setApi(String api) {
        this.api = api;
    }

    public Long getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(Long executionTime) {
        this.executionTime = executionTime;
    }
}

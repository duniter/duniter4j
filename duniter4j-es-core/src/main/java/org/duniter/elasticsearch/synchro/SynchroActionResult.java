package org.duniter.elasticsearch.synchro;

public interface SynchroActionResult {

    void addInsert();
    void addUpdate();
    void addDelete();
    void addInvalidSignature();
    void addInvalidTime();

    long getInserts();
    long getUpdates();
    long getDeletes();
    long getInvalidSignatures();
    long getInvalidTimes();
}

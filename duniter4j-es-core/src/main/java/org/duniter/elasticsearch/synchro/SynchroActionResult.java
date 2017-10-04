package org.duniter.elasticsearch.synchro;

public interface SynchroActionResult {

    void addInsert();
    void addUpdate();
    void addDelete();
    void addInvalidSignature();

    long getInserts();
    long getUpdates();
    long getDeletes();
    long getInvalidSignatures();
}

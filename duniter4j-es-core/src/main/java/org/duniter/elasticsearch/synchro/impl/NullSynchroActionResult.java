package org.duniter.elasticsearch.synchro.impl;

import org.duniter.elasticsearch.synchro.SynchroActionResult;

public class NullSynchroActionResult implements SynchroActionResult {

    @Override
    public void addInsert(){
    }

    @Override
    public void addUpdate() {
    }

    @Override
    public void addDelete() {
    }

    @Override
    public void addInvalidSignature() {
    }

    @Override
    public void addInvalidTime() {

    }

    @Override
    public long getInserts() {
        return 0;
    }

    @Override
    public long getUpdates() {
        return 0;
    }

    @Override
    public long getDeletes() {
        return 0;
    }

    @Override
    public long getInvalidSignatures() {
        return 0;
    }

    @Override
    public long getInvalidTimes() {
        return 0;
    }
}

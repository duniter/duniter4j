package org.duniter.elasticsearch.synchro.impl;

import org.duniter.core.util.PrimitiveIterators;
import org.duniter.elasticsearch.synchro.SynchroActionResult;

public class SynchroActionResultImpl implements SynchroActionResult {

    private final PrimitiveIterators.OfLong insertHits = PrimitiveIterators.newLongSequence();
    private final PrimitiveIterators.OfLong updateHits = PrimitiveIterators.newLongSequence();
    private final PrimitiveIterators.OfLong deleteHits = PrimitiveIterators.newLongSequence();
    private final PrimitiveIterators.OfLong invalidSignatureHits = PrimitiveIterators.newLongSequence();

    @Override
    public void addInsert() {
        insertHits.nextLong();
    }

    @Override
    public void addUpdate() {
        updateHits.nextLong();
    }

    @Override
    public void addDelete() {
        deleteHits.nextLong();
    }

    @Override
    public void addInvalidSignature() {
        invalidSignatureHits.nextLong();
    }

    @Override
    public long getInserts() {
        return insertHits.current();
    }
    @Override
    public long getUpdates() {
        return updateHits.current();
    }
    @Override
    public long getDeletes() {
        return deleteHits.current();
    }
    @Override
    public long getInvalidSignatures() {
        return invalidSignatureHits.current();
    }
}

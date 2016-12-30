package org.duniter.elasticsearch.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by blavenie on 30/12/16.
 */
public class SynchroResult {

    private long insertTotal = 0;
    private long updateTotal = 0;
    private long deleteTotal = 0;
    private Map<String, Long> insertHits = new HashMap<>();
    private Map<String, Long> updateHits = new HashMap<>();
    private Map<String, Long> deleteHits = new HashMap<>();

    public void addInserts(String index, String type, long nbHits) {
        insertHits.put(index + "/" + type, getInserts(index, type) + nbHits);
        insertTotal += nbHits;
    }

    public void addUpdates(String index, String type, long nbHits) {
        updateHits.put(index + "/" + type, getUpdates(index, type) + nbHits);
        updateTotal += nbHits;
    }

    public void addDeletes(String index, String type, long nbHits) {
        deleteHits.put(index + "/" + type, getDeletes(index, type) + nbHits);
        deleteTotal += nbHits;
    }

    public long getInserts(String index, String type) {
        return insertHits.getOrDefault(index + "/" + type, 0l);
    }

    public long getUpdates(String index, String type) {
        return updateHits.getOrDefault(index + "/" + type, 0l);
    }

    public long getDeletes(String index, String type) {
        return deleteHits.getOrDefault(index + "/" + type, 0l);
    }

    public long getInserts() {
        return insertTotal;
    }

    public long getUpdates() {
        return updateTotal;
    }

    public long getDeletes() {
        return deleteTotal;
    }

    public long getTotal() {
        return insertTotal + updateTotal + deleteTotal;
    }


    public String toString() {
        return new StringBuilder()
            .append("inserts [").append(insertTotal).append("]")
            .append(", updates [").append(updateTotal).append("]")
            .append(", deletes [").append(deleteTotal).append("]")
            .toString();
    }
}

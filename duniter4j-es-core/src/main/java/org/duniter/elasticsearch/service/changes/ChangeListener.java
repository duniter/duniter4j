package org.duniter.elasticsearch.service.changes;

public interface ChangeListener {
    String getId();
    void onChanges(String message);
}
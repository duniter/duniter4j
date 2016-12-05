package org.duniter.elasticsearch.user.service.event;

public interface UserEventListener {
    String getId();
    String getPubkey();
    void onEvent(UserEvent event);
}
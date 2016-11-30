package org.duniter.elasticsearch.user.service.event;

public interface UserEventListener {
    String getId();
    void onEvent(UserEvent event);
}
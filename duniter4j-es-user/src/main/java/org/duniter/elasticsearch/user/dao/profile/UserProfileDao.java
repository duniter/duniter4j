package org.duniter.elasticsearch.user.dao.profile;

import org.duniter.elasticsearch.user.dao.RecordDao;

public interface UserProfileDao<T extends UserProfileDao> extends RecordDao<T> {

    String TYPE = "profile";

    String create(final String issuer, final String json);
}

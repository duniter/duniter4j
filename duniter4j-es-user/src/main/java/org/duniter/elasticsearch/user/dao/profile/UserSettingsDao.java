package org.duniter.elasticsearch.user.dao.profile;

import org.duniter.elasticsearch.user.dao.RecordDao;

public interface UserSettingsDao<T extends UserSettingsDao> extends RecordDao<T> {

    String TYPE = "settings";

    String create(final String issuer, final String json);
}

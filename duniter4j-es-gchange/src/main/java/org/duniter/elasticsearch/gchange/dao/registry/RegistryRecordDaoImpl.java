package org.duniter.elasticsearch.gchange.dao.registry;

import org.duniter.elasticsearch.gchange.PluginSettings;
import org.duniter.elasticsearch.gchange.dao.AbstractRecordDaoImpl;
import org.elasticsearch.common.inject.Inject;

/**
 * Created by blavenie on 03/04/17.
 */
public class RegistryRecordDaoImpl extends AbstractRecordDaoImpl implements RegistryRecordDao {

    @Inject
    public RegistryRecordDaoImpl(PluginSettings pluginSettings) {
        super(RegistryIndexDao.INDEX, pluginSettings);
    }
}

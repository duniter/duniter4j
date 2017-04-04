package org.duniter.elasticsearch.gchange.dao.registry;

import org.duniter.elasticsearch.gchange.PluginSettings;
import org.duniter.elasticsearch.gchange.dao.AbstractCommentDaoImpl;
import org.elasticsearch.common.inject.Inject;

/**
 * Created by blavenie on 03/04/17.
 */
public class RegistryCommentDaoImpl extends AbstractCommentDaoImpl implements RegistryCommentDao {


    @Inject
    public RegistryCommentDaoImpl(PluginSettings pluginSettings) {
        super(RegistryIndexDao.INDEX, pluginSettings);
    }
}

package org.duniter.elasticsearch.gchange.dao.registry;

import org.duniter.elasticsearch.dao.IndexDao;

/**
 * Created by blavenie on 03/04/17.
 */
public interface RegistryIndexDao extends IndexDao<RegistryIndexDao> {
    String INDEX = "registry";
    String CATEGORY_TYPE = "category";
}

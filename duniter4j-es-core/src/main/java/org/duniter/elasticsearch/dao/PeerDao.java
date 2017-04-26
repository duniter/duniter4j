package org.duniter.elasticsearch.dao;

import org.duniter.elasticsearch.dao.impl.PeerDaoImpl;

/**
 * Created by blavenie on 26/04/17.
 */
public interface PeerDao extends org.duniter.core.client.dao.PeerDao, TypeDao<PeerDaoImpl>{

    String TYPE = "peer";

}

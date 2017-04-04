package org.duniter.elasticsearch.dao;

import org.duniter.elasticsearch.dao.handler.StringReaderHandler;

/**
 * Created by blavenie on 30/03/17.
 */

public interface IndexDao<T extends IndexDao> {

    T createIndexIfNotExists();

    T deleteIndex();

    String getIndex();

    boolean existsIndex();
}

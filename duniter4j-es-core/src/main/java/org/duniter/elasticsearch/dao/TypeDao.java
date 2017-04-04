package org.duniter.elasticsearch.dao;

import org.elasticsearch.common.xcontent.XContentBuilder;

import java.util.Map;

/**
 * Created by blavenie on 30/03/17.
 */

public interface TypeDao<T extends TypeDao> {

    XContentBuilder createTypeMapping();

    String getType();
}

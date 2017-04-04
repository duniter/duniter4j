package org.duniter.elasticsearch.dao;

import org.duniter.elasticsearch.dao.handler.StringReaderHandler;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.util.Map;

/**
 * Created by blavenie on 03/04/17.
 */
public interface IndexTypeDao<T extends IndexTypeDao> {

    T createIndexIfNotExists();

    T deleteIndex();

    String getIndex();

    boolean existsIndex();

    XContentBuilder createTypeMapping();

    String getType();

    boolean isExists(String docId);

    Object getFieldById(String docId, String fieldName);

    Map<String, Object> getFieldsById(String docId, String... fieldNames);

    <B> B getTypedFieldById(String docId, String fieldName);

    Map<String, Object> getMandatoryFieldsById(String docId, String... fieldNames);

    void bulkFromClasspathFile(String classpathFile);

    void bulkFromClasspathFile(String classpathFile, StringReaderHandler handler);
}

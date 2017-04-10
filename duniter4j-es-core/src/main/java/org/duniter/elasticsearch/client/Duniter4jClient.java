package org.duniter.elasticsearch.client;

import org.duniter.core.beans.Bean;
import org.duniter.core.client.model.local.LocalEntity;
import org.duniter.elasticsearch.dao.handler.StringReaderHandler;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.SearchHit;

import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

/**
 * Created by blavenie on 03/04/17.
 */
public interface Duniter4jClient extends Bean, Client {

    boolean existsIndex(String index);

    void deleteIndexIfExists(String indexName);

    Object getFieldById(String index, String type, String docId, String fieldName);

    Map<String, Object> getFieldByIds(String index, String type, Set<String> ids, String fieldName);

    Map<String, Object> getFieldsById(String index, String type, String docId, String... fieldNames);

    <T> T getTypedFieldById(String index, String type, String docId, String fieldName);

    Map<String, Object> getMandatoryFieldsById(String index, String type, String docId, String... fieldNames);

    String indexDocumentFromJson(String index, String type, String json);

    void updateDocumentFromJson(String index, String type, String id, String json);

    void checkSameDocumentField(String index, String type, String id, String fieldName, String expectedvalue) throws ElasticsearchException;

    void checkSameDocumentIssuer(String index, String type, String id, String expectedIssuer);

    boolean isDocumentExists(String index, String type, String id) throws ElasticsearchException;

    void checkDocumentExists(String index, String type, String id) throws ElasticsearchException;

    /**
     * Retrieve a document by id (safe mode)
     * @param docId
     * @return
     */
    <T extends Object> T getSourceByIdOrNull(String index, String type, String docId, Class<T> classOfT, String... fieldNames);

    /**
     * Retrieve a document by id
     * @param docId
     * @return
     */
    <T extends Object> T getSourceById(String index, String type, String docId, Class<T> classOfT, String... fieldNames);

    <C extends LocalEntity<String>> C readSourceOrNull(SearchHit searchHit, Class<? extends C> clazz);

    void bulkFromClasspathFile(String classpathFile, String indexName, String indexType);

    void bulkFromClasspathFile(String classpathFile, String indexName, String indexType, StringReaderHandler handler);

    void bulkFromFile(File file, String indexName, String indexType);

    void bulkFromFile(File file, String indexName, String indexType, StringReaderHandler handler);

    void bulkFromStream(InputStream is, String indexName, String indexType);

    void bulkFromStream(InputStream is, String indexName, String indexType, StringReaderHandler handler);

    void flushDeleteBulk(final String index, final String type, BulkRequestBuilder bulkRequest);

    void safeExecuteRequest(ActionRequestBuilder<?, ?, ?> request, boolean wait);
}

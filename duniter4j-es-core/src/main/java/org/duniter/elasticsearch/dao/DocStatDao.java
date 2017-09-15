package org.duniter.elasticsearch.dao;

import org.duniter.elasticsearch.model.DocStat;
import org.elasticsearch.action.index.IndexRequestBuilder;

import javax.annotation.Nullable;

/**
 * Created by blavenie on 13/09/17.
 */
public interface DocStatDao extends IndexTypeDao<DocStatDao>{
    String INDEX = "docstat";
    String TYPE = "record";

    long countDoc(String index, @Nullable String type);

    IndexRequestBuilder prepareIndex(DocStat stat);

}

package org.duniter.elasticsearch.exception;

import org.elasticsearch.ElasticsearchException;

/**
 * Created by blavenie on 28/07/16.
 */
public abstract class DuniterElasticsearchException extends ElasticsearchException {


    public DuniterElasticsearchException(Throwable cause) {
        super(cause);
    }

    public DuniterElasticsearchException(String msg, Object... args) {
        super(msg, args);
    }

    public DuniterElasticsearchException(String msg, Throwable cause, Object... args) {
        super(msg, args, cause);
    }

}

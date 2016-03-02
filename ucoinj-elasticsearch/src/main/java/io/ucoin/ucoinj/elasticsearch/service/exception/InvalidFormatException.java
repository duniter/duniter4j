package io.ucoin.ucoinj.elasticsearch.service.exception;

import io.ucoin.ucoinj.core.exception.BusinessException;

/**
 * Created by blavenie on 01/03/16.
 */
public class InvalidFormatException extends BusinessException {
    public InvalidFormatException() {
        super();
    }

    public InvalidFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidFormatException(String message) {
        super(message);
    }

    public InvalidFormatException(Throwable cause) {
        super(cause);
    }
}

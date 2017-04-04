package org.duniter.core.beans;

import org.duniter.core.exception.TechnicalException;

/**
 * Created by blavenie on 31/03/17.
 */
public class BeanCreationException extends TechnicalException {

    public BeanCreationException() {
    }

    public BeanCreationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public BeanCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public BeanCreationException(String message) {
        super(message);
    }

    public BeanCreationException(Throwable cause) {
        super(cause);
    }
}

package org.duniter.core.client.service.bma;

import org.duniter.core.exception.TechnicalException;

/**
 * Created by blavenie on 20/04/17.
 */
public class BmaTechnicalException extends TechnicalException {

    public BmaTechnicalException() {
        super();
    }

    public BmaTechnicalException(int code, String message) {
        super(message);
        setCode(code);
    }

    public BmaTechnicalException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public BmaTechnicalException(String message, Throwable cause) {
        super(message, cause);
    }

    public BmaTechnicalException(String message) {
        super(message);
    }

    public BmaTechnicalException(Throwable cause) {
        super(cause);
    }

    @Override
    public String getMessage() {
        if (getCode() != -1) {
            return super.getMessage() + String.format(" (code: %s)", getCode());
        }
        return super.getMessage();

    }
}

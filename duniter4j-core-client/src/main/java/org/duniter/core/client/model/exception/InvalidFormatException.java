package org.duniter.core.client.model.exception;

import org.duniter.core.exception.TechnicalException;

/**
 * Created by blavenie on 24/07/17.
 */
public class InvalidFormatException extends TechnicalException {

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

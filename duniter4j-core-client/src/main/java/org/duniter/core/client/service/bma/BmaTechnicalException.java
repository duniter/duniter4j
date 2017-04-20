package org.duniter.core.client.service.bma;

/*-
 * #%L
 * Duniter4j :: Core Client API
 * %%
 * Copyright (C) 2014 - 2017 EIS
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

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

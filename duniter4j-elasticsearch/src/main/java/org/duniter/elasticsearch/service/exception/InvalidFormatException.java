package org.duniter.elasticsearch.service.exception;

/*
 * #%L
 * Duniter4j :: ElasticSearch Plugin
 * %%
 * Copyright (C) 2014 - 2016 EIS
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

import org.duniter.core.exception.BusinessException;

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

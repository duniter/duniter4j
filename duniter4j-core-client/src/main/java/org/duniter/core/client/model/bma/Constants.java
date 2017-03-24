package org.duniter.core.client.model.bma;

/*
 * #%L
 * Duniter4j :: Core Client API
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

/**
 * Created by blavenie on 11/11/16.
 */
public interface Constants {

    interface Regex {
        String USER_ID = "[A-Za-z0-9_-]+";
        String CURRENCY_NAME = "[A-Za-z0-9_-]";
        String PUBKEY = "[123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz]{43,44}";
    }

    interface HttpStatus {
        int SC_TOO_MANY_REQUESTS = 429;
    }

    interface Config {
        int TOO_MANY_REQUEST_RETRY_TIME = 500; // 500 ms
        int MAX_SAME_REQUEST_COUNT = 5; // 5 requests before to get 429 error
    }
}

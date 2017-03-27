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
 * Created by blavenie on 31/03/16.
 */
public interface ErrorCode {

    int ALREADY_UP_TO_DATE = 1004;
    int NO_MATCHING_IDENTITY = 2001;
    int UID_ALREADY_USED = 2003;
    int WRONG_SIGNATURE_MEMBERSHIP = 2006;
    int MEMBERSHRIP_ALREADY_SEND = 2007;
}

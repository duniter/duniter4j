package org.duniter.core.client.model.elasticsearch;

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
 * Helper class
 * Created by blavenie on 01/03/16.
 */
public final class Records {

    // Common fields
    public static final String PROPERTY_ISSUER="issuer";
    public static final String PROPERTY_HASH="hash";
    public static final String PROPERTY_SIGNATURE="signature";
    public static final String PROPERTY_VERSION="version";
    public static final String PROPERTY_TIME="time";
    public static final String PROPERTY_CREATION_TIME="creationTime";

    // Read marker
    public static final String PROPERTY_READ_SIGNATURE="read_signature";

    // Location
    public static final String PROPERTY_ADDRESS="address";
    public static final String PROPERTY_CITY="city";
    public static final String PROPERTY_GEO_POINT="geoPoint";

    // record
    public static final String PROPERTY_TITLE="title";
    public static final String PROPERTY_DESCRIPTION="description";

    // Avatar & pictures
    public static final String PROPERTY_AVATAR="avatar";
    public static final String PROPERTY_PICTURES="pictures";
    public static final String PROPERTY_PICTURES_COUNT="picturesCount";

    // Socials & tags
    public static final String PROPERTY_SOCIALS="socials";
    public static final String PROPERTY_TAGS="tags";

    // Other
    public static final String PROPERTY_CATEGORY="category";
    public static final String PROPERTY_CONTENT="content";



}

package org.duniter.core.util.url;

/*-
 * #%L
 * Duniter4j :: Core Shared
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

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by blavenie on 08/04/17.
 */
public final class URLs {

    static {
        init();
    }

    private void URLs() {
        // helper class
    }

    public static URL newClasspathURL(String classpathResource, ClassLoader cl) throws MalformedURLException {
        final URL resourceUrl = cl.getResource(classpathResource);
        return resourceUrl;
    }

    protected static void init() {
        // Extend default JRE protocols (add classpath://)
       // System.setProperty("java.protocol.handler.pkgs", "org.duniter.core.util.protocols");

    }

    public static URL getClasspathResourceURL(String aClasspathFile, ClassLoader cl) throws MalformedURLException {
        final URL resourceUrl = cl.getResource(aClasspathFile);
        if (resourceUrl == null) {
            throw new TechnicalException("File not found : " + aClasspathFile);
        }
        return resourceUrl;
    }

    public static URL getParentURL(URL resourceUrl) throws MalformedURLException {
        String filePath = resourceUrl.getPath();
        return new URL(filePath.substring(0, filePath.lastIndexOf('/')));
    }
}

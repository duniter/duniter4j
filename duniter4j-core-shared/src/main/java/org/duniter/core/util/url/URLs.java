package org.duniter.core.util.url;

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

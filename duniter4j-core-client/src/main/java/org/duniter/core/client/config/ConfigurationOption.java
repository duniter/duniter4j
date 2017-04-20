package org.duniter.core.client.config;

/*
 * #%L
 * Tutti :: Persistence
 * $Id: TuttiConfigurationOption.java 1441 2013-12-09 20:13:47Z tchemit $
 * $HeadURL: http://svn.forge.codelutin.com/svn/tutti/trunk/tutti-persistence/src/main/java/fr/ifremer/tutti/TuttiConfigurationOption.java $
 * %%
 * Copyright (C) 2012 - 2013 Ifremer
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

import org.nuiton.config.ConfigOptionDef;
import org.nuiton.version.Version;

import java.io.File;
import java.net.URL;
import java.util.Locale;

import static org.nuiton.i18n.I18n.n;

/**
 * All application configuration options.
 * 
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 * @since 1.0
 */
public enum ConfigurationOption implements ConfigOptionDef {

    // ------------------------------------------------------------------------//
    // -- READ-ONLY OPTIONS ---------------------------------------------------//
    // ------------------------------------------------------------------------//

    BASEDIR(
            "duniter4j.basedir",
            n("duniter4j.config.option.basedir.description"),
            "${user.home}/.config/duniter4j",
            File.class),

    DATA_DIRECTORY(
            "duniter4j.data.directory",
            n("duniter4j.config.option.data.directory.description"),
            "${duniter4j.basedir}/data",
            File.class),

    I18N_DIRECTORY(
            "duniter4j.i18n.directory",
            n("duniter4j.config.option.i18n.directory.description"),
            "${duniter4j.data.directory}/i18n",
            File.class),

    TMP_DIRECTORY(
            "duniter4j.tmp.directory",
            n("duniter4j.config.option.tmp.directory.description"),
            "${duniter4j.data.directory}/temp",
            File.class),

    CACHE_DIRECTORY(
            "duniter4j.cache.directory",
            n("duniter4j.config.option.cache.directory.description"),
            "${duniter4j.data.directory}/cache",
            File.class),

    VERSION(
            "duniter4j.version",
            n("duniter4j.config.option.version.description"),
            "1.0",
            Version.class),

    SITE_URL(
            "duniter4j.site.url",
            n("duniter4j.config.option.site.url.description"),
            "https://github.com/duniter/duniter4j",
            URL.class),

    ORGANIZATION_NAME(
            "duniter4j.organizationName",
            n("duniter4j.config.option.organizationName.description"),
            "e-is.pro",
            String.class),

    INCEPTION_YEAR(
            "duniter4j.inceptionYear",
            n("duniter4j.config.option.inceptionYear.description"),
            "2011",
            Integer.class),

    USER_SALT(
            "duniter4j.salt",
            n("duniter4j.config.option.salt.description"),
            "",
            String.class),

    USER_PASSWD(
            "duniter4j.passwd",
            n("duniter4j.config.option.passwd.description"),
            "",
            String.class),

    // ------------------------------------------------------------------------//
    // -- DATA CONSTANTS --------------------------------------------------//
    // ------------------------------------------------------------------------//

    // ------------------------------------------------------------------------//
    // -- READ-WRITE OPTIONS --------------------------------------------------//
    // ------------------------------------------------------------------------//

    I18N_LOCALE(
            "duniter4j.i18n.locale",
            n("duniter4j.config.option.i18n.locale.description"),
            Locale.FRANCE.getCountry(),
            Locale.class,
            false),

    NODE_CURRENCY(
            "duniter4j.node.blockchain",
            n("duniter4j.config.option.node.currency.description"),
            "meta_brouzouf",
            String.class,
            false),

    NODE_PROTOCOL(
            "duniter4j.node.protocol",
            n("duniter4j.config.option.node.protocol.description"),
            "http",
            String.class,
            false),

    NODE_HOST(
            "duniter4j.node.host",
            n("duniter4j.config.option.node.host.description"),
            "g1.duniter.org",
            String.class,
            false),

    NODE_PORT(
            "duniter4j.node.port",
            n("duniter4j.config.option.node.port.description"),
            "10901",
            Integer.class,
            false),

    NODE_URL(
            "duniter4j.node.url",
            n("duniter4j.config.option.node.port.description"),
            "${duniter4j.node.protocol}://${duniter4j.node.host}:${duniter4j.node.port}",
            URL.class,
            false),

    NETWORK_TIMEOUT(
            "duniter4j.network.timeout",
            n("duniter4j.config.option.network.timeout.description"),
            "5000", // = 5 s
            Integer.class,
            false),

    NETWORK_MAX_CONNECTIONS(
            "duniter4j.network.maxConnections",
            n("duniter4j.config.option.network.maxConnections.description"),
            "100",
            Integer.class,
            false),

    NETWORK_MAX_CONNECTIONS_PER_ROUTE(
            "duniter4j.network.maxConnectionsPerHost",
            n("duniter4j.config.option.network.maxConnectionsPerHost.description"),
            "5",
            Integer.class,
            false),


    NETWORK_CACHE_TIME_IN_MILLIS (
            "duniter4j.network.cacheTimeInMillis",
            "duniter4j.config.option.network.cacheTimeInMillis.description",
            "10000",  // = 10 s
            Integer.class,
            false),

    NODE_ELASTICSEARCH_PROTOCOL(
            "duniter4j.node.elasticsearch.protocol",
            n("duniter4j.config.option.node.elasticsearch.protocol.description"),
            "http",
            String.class,
            false),

    NODE_ELASTICSEARCH_HOST(
            "duniter4j.node.elasticsearch.host",
            n("duniter4j.config.option.node.elasticsearch.host.description"),
            "localhost",
            String.class,
            false),

    NODE_ELASTICSEARCH_PORT(
            "duniter4j.node.elasticsearch.port",
            n("duniter4j.config.option.node.elasticsearch.port.description"),
            "9200",
            Integer.class,
            false),

    NODE_ELASTICSEARCH_URL(
            "duniter4j.node.elasticsearch.url",
            n("duniter4j.config.option.node.elasticsearch.url.description"),
            "${duniter4j.node.elasticsearch.protocol}://${duniter4j.node.elasticsearch.host}:${duniter4j.node.elasticsearch.port}",
            URL.class,
            false)
    ;

    /** Configuration key. */
    private final String key;

    /** I18n key of option description */
    private final String description;

    /** Type of option */
    private final Class<?> type;

    /** Default value of option. */
    private String defaultValue;

    /** Flag to not keep option value on disk */
    private boolean isTransient;

    /** Flag to not allow option value modification */
    private boolean isFinal;

    ConfigurationOption(String key,
            String description,
            String defaultValue,
            Class<?> type,
            boolean isTransient) {
        this.key = key;
        this.description = description;
        this.defaultValue = defaultValue;
        this.type = type;
        this.isTransient = isTransient;
        this.isFinal = isTransient;
    }

    ConfigurationOption(String key,
            String description,
            String defaultValue,
            Class<?> type) {
        this(key, description, defaultValue, type, true);
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public Class<?> getType() {
        return type;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }

    @Override
    public boolean isTransient() {
        return isTransient;
    }

    @Override
    public boolean isFinal() {
        return isFinal;
    }

    @Override
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public void setTransient(boolean newValue) {
        // not used
    }

    @Override
    public void setFinal(boolean newValue) {
        // not used
    }
}

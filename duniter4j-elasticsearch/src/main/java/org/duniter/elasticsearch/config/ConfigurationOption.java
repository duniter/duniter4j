package org.duniter.elasticsearch.config;

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
import org.nuiton.util.Version;

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
public enum ConfigurationOption  implements ConfigOptionDef {


    // ------------------------------------------------------------------------//
    // -- READ-ONLY OPTIONS ---------------------------------------------------//
    // ------------------------------------------------------------------------//


    BASEDIR(
            "duniter4j.basedir",
            n("duniter4j.config.option.basedir.description"),
            "${user.home}/.config/duniter-es",
            File.class),

    DATA_DIRECTORY(
            "duniter4j.data.directory",
            n("duniter4j.config.option.data.directory.description"),
            "${duniter4j.basedir}/data",
            File.class),

    TEMP_DIRECTORY(
            "duniter4j.temp.directory",
            n("duniter4j.config.option.temp.directory.description"),
            "${duniter4j.basedir}/temp",
            File.class),

    PLUGINS_DIRECTORY(
            "duniter4j.plugins.directory",
            n("duniter4j.config.option.plugins.directory.description"),
            "${duniter4j.basedir}/plugins",
            File.class),

    LAUNCH_MODE(
            "duniter4j.launch.mode",
            n("duniter4j.config.option.launch.mode.description"),
            "dev",
            String.class),

    I18N_DIRECTORY(
            "duniter4j.i18n.directory",
            n("duniter4j.config.option.i18n.directory.description"),
            "${duniter4j.basedir}/i18n",
            File.class),

    VERSION(
            "duniter4j.version",
            n("duniter4j.config.option.version.description"),
            "1.0",
            Version.class),

    // ------------------------------------------------------------------------//
    // -- READ-WRITE OPTIONS ---------------------------------------------------//
    // ------------------------------------------------------------------------//

    I18N_LOCALE(
            "duniter4j.i18n.locale",
            n("duniter4j.config.option.i18n.locale.description"),
            Locale.FRANCE.getCountry(),
            Locale.class,
            false),

    NODE_BMA_HOST(
            "duniter4j.node.host",
            n("duniter4j.config.option.node.host.description"),
            "metab.ucoin.io",
            String.class,
            false),

    NODE_BMA_PORT(
            "duniter4j.node.port",
            n("duniter4j.config.option.node.port.description"),
            "9201",
            Integer.class,
            false),

    NODE_BMA_URL(
            "duniter4j.node.url",
            n("duniter4j.config.option.node.port.description"),
            "${duniter4j.node.protocol}://${duniter4j.node.host}:${duniter4j.node.port}",
            URL.class,
            false),

    HOST(
            "duniter4j.elasticsearch.host",
            n("duniter4j.config.option.elasticsearch.host.description"),
            "localhost",
            String.class,
            false),

    PORT(
            "duniter4j.elasticsearch.port",
            n("duniter4j.config.option.node.elasticsearch.port.description"),
            "9300",
            Integer.class,
            false),

    NETWORK_HOST(
            "duniter4j.elasticsearch.network.host",
            n("duniter4j.config.option.elasticsearch.network.host.description"),
            "_local_",
            String.class,
            false),

    DAEMON(
            "duniter4j.elasticsearch.daemon",
            n("duniter4j.config.option.node.elasticsearch.daemon.description"),
            "false",
            Boolean.class,
            false),

    EMBEDDED_ENABLE(
            "duniter4j.elasticsearch.embedded.enable",
            n("duniter4j.config.option.elasticsearch.embedded.enable.description"),
            "false",
            Boolean.class,
            false),

    LOCAL_ENABLE(
            "duniter4j.elasticsearch.local",
            n("duniter4j.config.option.elasticsearch.local.description"),
            "false",
            Boolean.class,
            false),

    HTTP_ENABLE(
            "duniter4j.elasticsearch.http.enable",
            n("duniter4j.config.option.node.elasticsearch.http.enable.description"),
            "true",
            Boolean.class,
            false),

    CLUSTER_NAME(
            "duniter4j.elasticsearch.cluster.name",
            n("duniter4j.config.option.elasticsearch.cluster.name.description"),
            "duniter4j-elasticsearch",
            String.class,
            false),

    INDEX_BULK_ENABLE(
            "duniter4j.elasticsearch.bulk.enable",
            n("duniter4j.config.option.elasticsearch.bulk.enable.description"),
            "true",
            Boolean.class,
            false),

    INDEX_BULK_SIZE(
            "duniter4j.elasticsearch.bulk.size",
            n("duniter4j.config.option.elasticsearch.bulk.size.description"),
            "1000",
            Integer.class,
            false),

    INDEX_STRING_ANALYZER(
            "duniter4j.elasticsearch.string.analyzer",
            n("duniter4j.config.option.elasticsearch.string.analyze.description"),
            "french",
            String.class,
            false),

    TASK_EXECUTOR_QUEUE_CAPACITY(
            "duniter4j.elasticsearch.tasks.queueCapacity",
            n("duniter4j.config.option.tasks.queueCapacity.description"),
            "50",
            Integer.class,
            false),

    TASK_EXECUTOR_TIME_TO_IDLE(
            "duniter4j.elasticsearch.tasks.timeToIdle",
            "duniter4j.elasticsearch.tasks.timeToIdle.description",
            "180", // 180s = 3min
            Integer.class,
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

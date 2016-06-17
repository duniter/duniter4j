package org.duniter.elasticsearch;

/*
 * #%L
 * UCoin Java Client :: Core API
 * %%
 * Copyright (C) 2014 - 2015 EIS
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


import org.apache.commons.io.FileUtils;
import org.duniter.core.client.config.Configuration;
import org.duniter.core.client.config.ConfigurationOption;
import org.elasticsearch.common.component.Lifecycle;
import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.component.LifecycleListener;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.nuiton.config.ApplicationConfig;
import org.nuiton.i18n.I18n;
import org.nuiton.i18n.init.DefaultI18nInitializer;
import org.nuiton.i18n.init.UserI18nInitializer;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * Access to configuration options
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 * @since 1.0
 */
public class PluginSettings {
    /** Logger. */
    private ESLogger log = ESLoggerFactory.getLogger(PluginSettings.class.getName());

    private org.elasticsearch.common.settings.Settings settings;

    /**
     * Delegate application config.
     */
    protected final ApplicationConfig applicationConfig;

    @Inject
    public PluginSettings(org.elasticsearch.common.settings.Settings settings) {
        this.settings = settings;
        this.applicationConfig = new ApplicationConfig();

        // Cascade the application config to the client module
        org.duniter.core.client.config.Configuration clientConfig = new org.duniter.core.client.config.Configuration(applicationConfig);
        org.duniter.core.client.config.Configuration.setInstance(clientConfig);

        String baseDir = settings.get("es.path.home");
        applicationConfig.setOption(ConfigurationOption.BASEDIR.getKey(), baseDir);
        applicationConfig.setOption(ConfigurationOption.NODE_HOST.getKey(), getNodeBmaHost());
        applicationConfig.setOption(ConfigurationOption.NODE_PORT.getKey(), String.valueOf(getNodeBmaPort()));
        applicationConfig.setOption(ConfigurationOption.NODE_PROTOCOL.getKey(), getNodeBmaPort() == 443 ? "https" : "http");

        //initI18n();
    }

    public String getNodeBmaHost() {
        return settings.get("duniter.host", "cgeek.fr");
    }

    public int getNodeBmaPort() {
        return settings.getAsInt("duniter.port", 9330);
    }

    public boolean isIndexBulkEnable() {
        return settings.getAsBoolean("duniter.bulk.enable", true);
    }

    public int getIndexBulkSize() {
        return settings.getAsInt("duniter.bulk.size", 1000);
    }

    public String getIndexStringAnalyzer() {
        return settings.get("duniter.string.analyzer", "english");
    }

    public File getTempDirectory() {
        return Configuration.instance().getTempDirectory();
    }

    public boolean isDevMode() {
        return settings.getAsBoolean("duniter.dev.enable", false);
    }

    /* */

    protected void initI18n() throws IOException {
        Configuration config = Configuration.instance();

        // --------------------------------------------------------------------//
        // init i18n
        // --------------------------------------------------------------------//

        File i18nDirectory = new File(Configuration.instance().getDataDirectory(), "i18n");
        if (i18nDirectory.exists()) {
            // clean i18n cache
            FileUtils.cleanDirectory(i18nDirectory);
        }

        FileUtils.forceMkdir(i18nDirectory);

        if (log.isDebugEnabled()) {
            log.debug("I18N directory: " + i18nDirectory);
        }

        Locale i18nLocale = config.getI18nLocale();

        if (log.isInfoEnabled()) {
            log.info(String.format("Starts i18n with locale [%s] at [%s]",
                    i18nLocale, i18nDirectory));
        }
        I18n.init(new UserI18nInitializer(
                        i18nDirectory, new DefaultI18nInitializer(getI18nBundleName())),
                i18nLocale);
    }

    protected String getI18nBundleName() {
        return "duniter4j-elasticsearch-i18n";
    }
}

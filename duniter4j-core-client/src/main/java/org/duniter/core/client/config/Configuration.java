package org.duniter.core.client.config;

/*
 * #%L
 * Duniter4j :: Core API
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


import com.google.common.base.Charsets;
import lombok.extern.slf4j.Slf4j;
import org.duniter.core.client.util.spring.ConfigurableEnvironments;
import org.duniter.core.exception.TechnicalException;
import org.nuiton.config.*;
import org.nuiton.version.Version;
import org.nuiton.version.VersionBuilder;
import org.springframework.core.env.ConfigurableEnvironment;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import static org.nuiton.i18n.I18n.t;

/**
 * Access to configuration options
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 * @since 1.0
 */
@Slf4j
public class Configuration  {

    protected static String[] args = null;

    /**
     * <p>remember app args.</p>
     */
    public static void setArgs(String[] sourceArgs) {
        args = sourceArgs;
    }

    /**
     * Delegate application config.
     */
    protected final ApplicationConfig applicationConfig;

    private static Configuration instance;

    public static Configuration instance() {
        return instance;
    }

    public static void setInstance(Configuration instance) {
        Configuration.instance = instance;
    }

    protected final String[] optionKeyToNotSave;

    protected File configFile;

    public Configuration(ApplicationConfig applicationConfig) {
        super();
        this.applicationConfig = applicationConfig;
        this.optionKeyToNotSave = null;

        // Override application version
        initVersion(applicationConfig);
    }

    public Configuration(ConfigurableEnvironment env,
                                String... args) {
        this(env, "application.fake.properties", args);
    }

    public Configuration(String file,
                         String... args) {
        this(null, file, args);
    }

    protected Configuration(ConfigurableEnvironment env,
                         String file,
                         String... args) {
        super();
        // load all default options
        Set<ApplicationConfigProvider> providers = getProviders();
        Properties defaults = getDefaults(providers, env);

        // Create Nuiton config instance
        this.applicationConfig = new ApplicationConfig(ApplicationConfigInit.forAllScopesWithout(
            ApplicationConfigScope.HOME
        ).setDefaults(defaults));
        this.applicationConfig.setEncoding(Charsets.UTF_8.name());
        this.applicationConfig.setConfigFileName(file);

        // Load actions
        for (ApplicationConfigProvider provider : providers) {
            applicationConfig.loadActions(provider.getActions());
        }
        
        // Define Alias
        addAlias(applicationConfig);

        // Override application version
        initVersion(applicationConfig);

        // get allOfToList transient and final option keys
        Set<String> optionToSkip =
                ApplicationConfigHelper.getTransientOptionKeys(providers);

        if (log.isDebugEnabled()) {
            log.debug("Option that won't be saved: " + optionToSkip);
        }
        optionKeyToNotSave = optionToSkip.toArray(new String[optionToSkip.size()]);

        try {
            applicationConfig.parse(args);

        } catch (ArgumentsParserException e) {
            throw new TechnicalException(t("duniter4j.config.parse.error"), e);
        }

        // Prepare base dir
        fixBaseDir(applicationConfig);
    }

    protected static Set<ApplicationConfigProvider> getProviders() {
        // get allOfToList config providers
        return ApplicationConfigHelper.getProviders(null,
                        null,
                        null,
                        true);
    }

    protected Properties getDefaults(Set<ApplicationConfigProvider> providers, ConfigurableEnvironment env) {

        // Populate defaults from providers
        final Properties defaults = new Properties();
        providers.forEach(provider -> Arrays.stream(provider.getOptions())
            .filter(configOptionDef -> configOptionDef.getDefaultValue() != null)
            .forEach(configOptionDef -> defaults.setProperty(configOptionDef.getKey(), configOptionDef.getDefaultValue())));

        // Set options from env if provided
        if (env != null) {
            return ConfigurableEnvironments.readProperties(env, defaults);
        }

        return defaults;
    }


    /**
     * Override the version default option, from the MANIFEST implementation version (if any)
     * @param applicationConfig
     */
    protected void initVersion(ApplicationConfig applicationConfig) {
        // Override application version
        String implementationVersion = this.getClass().getPackage().getSpecificationVersion();
        if (implementationVersion != null) {
            applicationConfig.setDefaultOption(
                    ConfigurationOption.VERSION.getKey(),
                    implementationVersion);
        }
    }

    /**
     * Add alias to the given ApplicationConfig. <p/>
     * This method could be override to add specific alias
     * 
     * @param applicationConfig
     */
    protected void addAlias(ApplicationConfig applicationConfig) {
        applicationConfig.addAlias("-h", "--option", ConfigurationOption.NODE_HOST.getKey());
        applicationConfig.addAlias("--host", "--option", ConfigurationOption.NODE_HOST.getKey());
        applicationConfig.addAlias("-p", "--option", ConfigurationOption.NODE_PORT.getKey());
        applicationConfig.addAlias("--port", "--option", ConfigurationOption.NODE_PORT.getKey());
        applicationConfig.addAlias("-c", "--option", ConfigurationOption.NODE_CURRENCY.getKey());
        applicationConfig.addAlias("--salt", "--option", ConfigurationOption.USER_SALT.getKey());
        applicationConfig.addAlias("--passwd", "--option", ConfigurationOption.USER_PASSWD.getKey());
    }

    protected void fixBaseDir(ApplicationConfig applicationConfig) {
        // TODO Review this, this is very dirty to do this...
        File appBasedir = applicationConfig.getOptionAsFile(
                ConfigurationOption.BASEDIR.getKey());

        if (appBasedir == null) {
            appBasedir = new File("");
        }
        if (!appBasedir.isAbsolute()) {
            appBasedir = new File(appBasedir.getAbsolutePath());
        }
        if (appBasedir.getName().equals("..")) {
            appBasedir = appBasedir.getParentFile().getParentFile();
        }
        if (appBasedir.getName().equals(".")) {
            appBasedir = appBasedir.getParentFile();
        }
        if (log.isInfoEnabled()) {
            log.info("Application basedir: " + appBasedir);
        }
        applicationConfig.setOption(
                ConfigurationOption.BASEDIR.getKey(),
                appBasedir.getAbsolutePath());
    }

    public File getConfigFile() {
        if (configFile == null) {
            File dir = getBasedir();
            if (dir == null || !dir.exists()) {
                dir = new File(applicationConfig.getUserConfigDirectory());
            }
            configFile = new File(dir, applicationConfig.getConfigFileName());
        }
        return configFile;
    }

    /** @return {@link ConfigurationOption#BASEDIR} value */
    public File getBasedir() {
        File result = applicationConfig.getOptionAsFile(ConfigurationOption.BASEDIR.getKey());
        return result;
    }

    /** @return {@link ConfigurationOption#DATA_DIRECTORY} value */
    public File getDataDirectory() {
        File result = applicationConfig.getOptionAsFile(ConfigurationOption.DATA_DIRECTORY.getKey());
        return result;
    }

    public ApplicationConfig getApplicationConfig() {
        return applicationConfig;
    }

    public File getTempDirectory() {
        return applicationConfig.getOptionAsFile(ConfigurationOption.TMP_DIRECTORY.getKey());
    }
    
    public File getCacheDirectory() {
        return applicationConfig.getOptionAsFile(ConfigurationOption.CACHE_DIRECTORY.getKey());
    }

    public Version getVersion() {
        String versionStr = applicationConfig.getOption(ConfigurationOption.VERSION.getKey());
        return VersionBuilder.create(versionStr).build();
    }

    public File getI18nDirectory() {
        return applicationConfig.getOptionAsFile(
                ConfigurationOption.I18N_DIRECTORY.getKey());
    }

    public Locale getI18nLocale() {
        return applicationConfig.getOptionAsLocale(
                ConfigurationOption.I18N_LOCALE.getKey());
    }

    public void setI18nLocale(Locale locale) {
        applicationConfig.setOption(ConfigurationOption.I18N_LOCALE.getKey(), locale.toString());
    }

    public String getNodeCurrency() {
        return applicationConfig.getOption(ConfigurationOption.NODE_CURRENCY.getKey());
    }

    public String getNodeHost() {
        return applicationConfig.getOption(ConfigurationOption.NODE_HOST.getKey());
    }
   
    public int getNodePort() {
        return applicationConfig.getOptionAsInt(ConfigurationOption.NODE_PORT.getKey());
    }
    
    public URL getNodeUrl() {
        return applicationConfig.getOptionAsURL(ConfigurationOption.NODE_URL.getKey());
    }

    public int getNetworkTimeout() {
        return applicationConfig.getOptionAsInt(ConfigurationOption.NETWORK_TIMEOUT.getKey());
    }

    public int getNetworkLargerTimeout() {
        return Math.max(30000, getNetworkTimeout());
    }

    public int getNetworkMaxTotalConnections() {
        return applicationConfig.getOptionAsInt(ConfigurationOption.NETWORK_MAX_CONNECTIONS.getKey());
    }

    public int getNetworkMaxConnectionsPerRoute() {
        return applicationConfig.getOptionAsInt(ConfigurationOption.NETWORK_MAX_CONNECTIONS_PER_ROUTE.getKey());
    }

    public int getNetworkCacheTimeInMillis() {
        return Integer.parseInt(ConfigurationOption.NETWORK_CACHE_TIME_IN_MILLIS.getDefaultValue());
    }

    public int getPeerUpMaxAge()  {
        return applicationConfig.getOptionAsInt(ConfigurationOption.NETWORK_PEER_UP_MAX_AGE.getKey());
    }

    public String getCesiumPlusPodHost() {
        return applicationConfig.getOption(ConfigurationOption.CESIUM_PLUS_POD_HOST.getKey());
    }
    public int getCesiumPlusPodPort() {
        return applicationConfig.getOptionAsInt(ConfigurationOption.CESIUM_PLUS_POD_PORT.getKey());
    }

    public URL getCesiumPlusPodUrl() {
        // Force SSL for 443 port
        if (getCesiumPlusPodPort() == 443) {
            try {
                return new URL(applicationConfig.getOption(ConfigurationOption.CESIUM_PLUS_POD_URL.getKey())
                        .replaceAll("http://", "https://"));
            } catch(MalformedURLException e) {
                return applicationConfig.getOptionAsURL(ConfigurationOption.CESIUM_PLUS_POD_URL.getKey());
            }
        }
        else {
            return applicationConfig.getOptionAsURL(ConfigurationOption.CESIUM_PLUS_POD_URL.getKey());
        }
    }
}

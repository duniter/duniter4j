package org.duniter.client;

/*
 * #%L
 * Duniter4j :: Client
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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.google.common.collect.Lists;
import org.duniter.client.actions.NetworkAction;
import org.duniter.client.actions.TransactionAction;
import org.apache.commons.io.FileUtils;
import org.duniter.core.client.config.Configuration;
import org.duniter.core.client.config.ConfigurationOption;
import org.duniter.core.client.service.ServiceLocator;
import org.duniter.core.util.StringUtils;
import org.nuiton.i18n.I18n;
import org.nuiton.i18n.init.DefaultI18nInitializer;
import org.nuiton.i18n.init.UserI18nInitializer;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by blavenie on 22/03/17.
 */
@Parameters(resourceBundle = "i18n.duniter4j-client")
public class Main {

    @Parameter(names = "-debug", description = "Debug mode", descriptionKey = "duniter4j.params.debug")
    private boolean debug = false;

    @Parameter(names = "--help", help = true)
    private boolean help;

    @Parameter(names = "--basedir", hidden = true)
    private File basedir;

    @Parameter(names = "--config", description = "Configuration file path", descriptionKey="duniter4j.params.config" )
    private String configFilename = "duniter-client.config";

    public static void main(String ... args) {
        Main main = new Main();
        main.run(args);
    }

    protected void run(String ... args) {

        Map<String, Runnable> actions = new HashMap<>();
        actions.put("network", new NetworkAction());
        actions.put("transaction", new TransactionAction());

        // Parsing args
        JCommander jc = new JCommander(this);
        actions.entrySet().stream().forEach(entry -> jc.addCommand(entry.getKey(), entry.getValue()));
        try {
            jc.parse(args);

            jc.getParameters().stream().forEach(param -> {
              if (param.getParameter().password()
                      && param.getParameter().required()
                      && param.getParameter().echoInput()
                      && !param.isAssigned()) {
                  System.out.print(param.getParameter().getParameter().description());
                  //var17.addValue(new String(var11));
              }
            });
            //jc.parse(args);
        }
        catch(ParameterException e) {
            System.err.println(e.getMessage());
            System.err.println("Try --help for usage");
            //jc.usage();
            System.exit(-1);
        }

        // Usage, if help or no command
        String actionName = jc.getParsedCommand();
        if (StringUtils.isBlank(actionName)) {
            jc.usage();
            // Return error code, if not help
            if (!help) System.exit(-1);
            return;
        }

        // Set log level
        // TODO

        // Init configuration
        initConfiguration(configFilename);

        // Init i18n
        try {
            initI18n();
        } catch(IOException e) {
            System.out.println("Unable to initialize translations");
            System.exit(-1);
        }

        // Set a default account id, then load cache
        ServiceLocator.instance().getDataContext().setAccountId(0);

        // Initialize service locator
        ServiceLocator.instance().init();

        Runnable action = actions.get(actionName);
        action.run();
    }


    protected String getI18nBundleName() {
        return "duniter4j-client-i18n";
    }

    /* -- -- */

    /**
     * Convenience methods that could be override to initialize other configuration
     *
     * @param configFilename
     */
    protected void initConfiguration(String configFilename) {
        String[] configArgs = getConfigArgs();
        Configuration config = new Configuration(configFilename, configArgs);
        Configuration.setInstance(config);
    }

    protected void initI18n() throws IOException {
        Configuration config = Configuration.instance();

        // --------------------------------------------------------------------//
        // init i18n
        // --------------------------------------------------------------------//
        File i18nDirectory = config.getI18nDirectory();
        if (i18nDirectory.exists()) {
            // clean i18n cache
            FileUtils.cleanDirectory(i18nDirectory);
        }

        FileUtils.forceMkdir(i18nDirectory);

        if (debug) {
            System.out.println("INFO - I18N directory: " + i18nDirectory);
        }

        Locale i18nLocale = config.getI18nLocale();

        // Fix locale
        if (i18nLocale.equals(Locale.FRENCH)) {
            i18nLocale = Locale.FRANCE;
        }
        else if (i18nLocale.equals(Locale.ENGLISH)) {
            i18nLocale = Locale.UK;
        }

        if (debug) {
            System.out.println(String.format("INFO - Starts i18n with locale [%s] at [%s]",
                    i18nLocale, i18nDirectory));
        }
        I18n.init(new UserI18nInitializer(
                        i18nDirectory, new DefaultI18nInitializer(getI18nBundleName())),
                i18nLocale);
    }

    protected String[] getConfigArgs() {
        List<String> configArgs = Lists.newArrayList();

        if (basedir != null) {
            configArgs.addAll(Lists.newArrayList(
                "--option", ConfigurationOption.BASEDIR.getKey(), basedir.getAbsolutePath()));
        }
        return configArgs.toArray(new String[configArgs.size()]);
    }

}

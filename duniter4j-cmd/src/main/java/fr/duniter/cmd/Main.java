package fr.duniter.cmd;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.collect.Lists;
import fr.duniter.cmd.actions.NetworkAction;
import fr.duniter.cmd.actions.SentMoneyAction;
import org.apache.commons.io.FileUtils;
import org.duniter.core.client.config.Configuration;
import org.duniter.core.client.service.ServiceLocator;
import org.duniter.core.util.StringUtils;
import org.nuiton.i18n.I18n;
import org.nuiton.i18n.init.DefaultI18nInitializer;
import org.nuiton.i18n.init.UserI18nInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by blavenie on 22/03/17.
 */
public class Main {

    @Parameter(names = "-debug", description = "Debug mode", arity = 1)
    private boolean debug = false;

    @Parameter(names = "--help", help = true)
    private boolean help;

    @Parameter(names = "-config", description = "Configuration file path")
    private String configFilename = "duniter-cmd.config";

    public static void main(String ... args) {
        Main main = new Main();
        main.run(args);
    }

    protected void run(String ... args) {

        Map<String, Runnable> actions = new HashMap<>();
        actions.put("network", new NetworkAction());
        actions.put("send", new SentMoneyAction());

        // Parsing args
        JCommander jc = new JCommander(this);
        actions.entrySet().stream().forEach(entry -> jc.addCommand(entry.getKey(), entry.getValue()));
        try {
            jc.parse(args);
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
        return "duniter4j-core-client-i18n";
    }

    /* -- -- */

    /**
     * Convenience methods that could be override to initialize other configuration
     *
     * @param configFilename
     * @param configArgs
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
        File i18nDirectory = new File(config.getDataDirectory(), "i18n");
        if (i18nDirectory.exists()) {
            // clean i18n cache
            FileUtils.cleanDirectory(i18nDirectory);
        }

        FileUtils.forceMkdir(i18nDirectory);

        if (debug) {
            System.out.println("I18N directory: " + i18nDirectory);
        }

        Locale i18nLocale = config.getI18nLocale();

        if (debug) {
            System.out.println(String.format("Starts i18n with locale [%s] at [%s]",
                    i18nLocale, i18nDirectory));
        }
        I18n.init(new UserI18nInitializer(
                        i18nDirectory, new DefaultI18nInitializer(getI18nBundleName())),
                i18nLocale);
    }

    protected String[] getConfigArgs() {
        List<String> configArgs = Lists.newArrayList();
        /*configArgs.addAll(Lists.newArrayList(
                "--option", ConfigurationOption.BASEDIR.getKey(), getResourceDirectory().getAbsolutePath()));*/
        return configArgs.toArray(new String[configArgs.size()]);
    }

}

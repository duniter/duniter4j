package org.duniter.elasticsearch.user;

import org.duniter.core.exception.TechnicalException;
import org.nuiton.config.ApplicationConfig;
import org.nuiton.config.ArgumentsParserException;

import static org.nuiton.i18n.I18n.t;

/**
 * Created by blavenie on 13/09/17.
 */
public class TestConfiguration {

    private ApplicationConfig applicationConfig;

    public TestConfiguration(String configFileName) {
        applicationConfig = new ApplicationConfig();
        applicationConfig.setConfigFileName(configFileName);

        try {
            applicationConfig.parse(new String[]{});

        } catch (ArgumentsParserException e) {
            throw new TechnicalException(t("duniter4j.config.parse.error"), e);
        }
    }

    public String getDataSyncHost() {
        return applicationConfig.getOption("duniter4j.data.sync.host");
    }

    public int getDataSyncPort() {
        return applicationConfig.getOptionAsInt("duniter4j.data.sync.port");
    }
}

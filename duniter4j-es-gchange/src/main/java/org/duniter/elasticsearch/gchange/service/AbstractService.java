package org.duniter.elasticsearch.gchange.service;

import org.duniter.core.service.CryptoService;
import org.duniter.elasticsearch.gchange.PluginSettings;
import org.elasticsearch.client.Client;

/**
 * Created by blavenie on 10/01/17.
 */
public abstract class AbstractService extends org.duniter.elasticsearch.user.service.AbstractService {

    protected PluginSettings pluginSettings;

    public AbstractService(String loggerName, Client client, PluginSettings pluginSettings) {
        this(loggerName, client, pluginSettings, null);
    }

    public AbstractService(Client client, PluginSettings pluginSettings) {
        this(client, pluginSettings, null);
    }

    public AbstractService(Client client, PluginSettings pluginSettings, CryptoService cryptoService) {
        this("duniter.gchange", client, pluginSettings, cryptoService);
    }

    public AbstractService(String loggerName, Client client, PluginSettings pluginSettings, CryptoService cryptoService) {
        super(loggerName, client, pluginSettings.getDelegate(), cryptoService);
        this.pluginSettings = pluginSettings;
    }

}

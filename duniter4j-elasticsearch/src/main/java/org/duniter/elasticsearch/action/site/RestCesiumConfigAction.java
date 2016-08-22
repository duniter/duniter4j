package org.duniter.elasticsearch.action.site;

/*
 * #%L
 * duniter4j-elasticsearch-plugin
 * %%
 * Copyright (C) 2014 - 2016 EIS
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

import org.duniter.core.exception.BusinessException;
import org.duniter.elasticsearch.PluginSettings;
import org.duniter.elasticsearch.exception.DuniterElasticsearchException;
import org.duniter.elasticsearch.exception.NodeConfigException;
import org.duniter.elasticsearch.rest.XContentThrowableRestResponse;
import org.duniter.elasticsearch.service.RegistryService;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.network.NetworkAddress;
import org.elasticsearch.common.network.NetworkService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.BoundTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.http.HttpChannel;
import org.elasticsearch.http.HttpServerTransport;
import org.elasticsearch.index.translog.TranslogService;
import org.elasticsearch.node.Node;
import org.elasticsearch.rest.*;
import org.elasticsearch.transport.TransportService;

import java.io.IOException;
import java.net.InetAddress;

import static org.elasticsearch.rest.RestStatus.OK;

public class RestCesiumConfigAction extends RestFilter {

    private static final ESLogger log = ESLoggerFactory.getLogger(RestCesiumConfigAction.class.getName());

    private static final String CONFIG_FILE_PATH = "/_plugin/duniter4j-elasticsearch/config.js";
    private NetworkService networkService;
    private HttpServerTransport transport;
    private PluginSettings pluginSettings;

    private String configJsContent;

    @Inject
    public RestCesiumConfigAction(RestController controller, PluginSettings pluginSettings, NetworkService networkService,
                                  HttpServerTransport transport) {
        controller.registerFilter(this);
        this.networkService = networkService;
        this.pluginSettings = pluginSettings;
        this.transport = transport;
    }

    @Override
    public void process(final RestRequest request, RestChannel restChannel, RestFilterChain restFilterChain) throws Exception {
        // If path = config file: send content
        if (CONFIG_FILE_PATH.equalsIgnoreCase(request.path())) {
            handleRequest(request, restChannel);
        }
        else {
            // Continue
            restFilterChain.continueProcessing(request, restChannel);
        }
    }

    protected void handleRequest(final RestRequest request, RestChannel restChannel) throws Exception {

        try {
            restChannel.sendResponse(new BytesRestResponse(OK, getCesiumConfigJs()));
        }
        catch(DuniterElasticsearchException | BusinessException e) {
            log.error(e.getMessage(), e);
            restChannel.sendResponse(new XContentThrowableRestResponse(request, e));
        }
        catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    protected String getCesiumConfigJs() throws DuniterElasticsearchException {
        if (configJsContent != null) {
            return configJsContent;
        }

        // Get ES node address
        BoundTransportAddress host = transport.boundAddress();
        TransportAddress esNodeAddress = host.publishAddress();

        // Compute the config file content
        configJsContent = String.format("angular.module(\"cesium.config\", [])\n" +
                        ".constant(\"csConfig\", {\n" +
                        "                \"node\": {\n" +
                        "                    \"host\": \"%s\",\n" +
                        "                    \"port\": \"%s\"\n" +
                        "                },\n" +
                        "                \"plugins\": {\n" +
                        "                  \"es\": {\n" +
                        "                    \"enable\": \"true\",\n" +
                        "                    \"host\": \"%s\",\n" +
                        "                    \"port\": \"%s\"\n" +
                        "                  },\n" +
                        "                },\n" +
                        "                \"newIssueUrl\": \"https://github.com/duniter/cesium/issues/new?labels=bug\",\n" +
                        "                \"time\": 4000,\n" +
                        "                \"version\": \"0.2.1\",\n" +
                        "             });",
                pluginSettings.getNodeBmaHost(),
                pluginSettings.getNodeBmaPort(),
                esNodeAddress.getHost(),
                esNodeAddress.getPort()
                );

        return configJsContent;
    }

}
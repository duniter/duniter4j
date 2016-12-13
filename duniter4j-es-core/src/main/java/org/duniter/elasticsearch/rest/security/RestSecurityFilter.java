package org.duniter.elasticsearch.rest.security;

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

import org.duniter.elasticsearch.PluginSettings;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.rest.*;

import static org.elasticsearch.rest.RestStatus.FORBIDDEN;

public class RestSecurityFilter extends RestFilter {

    private static final ESLogger log = ESLoggerFactory.getLogger("duniter.security");

    private RestSecurityController securityController;
    private final boolean debug;

    @Inject
    public RestSecurityFilter(PluginSettings pluginSettings, RestController controller, RestSecurityController securityController) {
        super();
        if (pluginSettings.enableSecurity()) {
            log.info("Enable security on duniter4j index access");
            controller.registerFilter(this);
        }
        this.securityController = securityController;
        this.debug = log.isDebugEnabled();
    }

    @Override
    public void process(RestRequest request, RestChannel channel, RestFilterChain filterChain) throws Exception {

        if (securityController.isAllow(request)) {
            if (debug) {
                log.debug(String.format("Allow %s request [%s]", request.method().name(), request.path()));
            }
            filterChain.continueProcessing(request, channel);
        }

        else {
            log.warn(String.format("Refused %s request to [%s]", request.method().name(), request.path()));
            channel.sendResponse(new BytesRestResponse(FORBIDDEN));
        }
    }

}
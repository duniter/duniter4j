package io.ucoin.ucoinj.elasticsearch.action.market;

/*
 * #%L
 * ucoinj-elasticsearch-plugin
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

import io.ucoin.ucoinj.core.exception.BusinessException;
import io.ucoin.ucoinj.elasticsearch.service.ServiceLocator;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.*;

import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestStatus.BAD_REQUEST;
import static org.elasticsearch.rest.RestStatus.OK;

public class RestMarketOfferIndexAction extends BaseRestHandler {

    private static final ESLogger log = ESLoggerFactory.getLogger(RestMarketOfferIndexAction.class.getName());

    @Inject
    public RestMarketOfferIndexAction(Settings settings, RestController controller, Client client) {
        super(settings, controller, client);
        controller.registerHandler(POST, "/market/offer", this);
    }

    @Override
    protected void handleRequest(final RestRequest request, RestChannel restChannel, Client client) throws Exception {

        try {
            String recordId = ServiceLocator.instance().getMarketRecordIndexerService().indexOfferFromJson(request.content().toUtf8());

            restChannel.sendResponse(new BytesRestResponse(OK, recordId));
        }
        catch(BusinessException e) {
            log.error(e.getMessage(), e);
            restChannel.sendResponse(new BytesRestResponse(BAD_REQUEST, String.format("{error: {ucode: 'XXX', message:'%s'}}", e.getMessage())));
        }
        catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}
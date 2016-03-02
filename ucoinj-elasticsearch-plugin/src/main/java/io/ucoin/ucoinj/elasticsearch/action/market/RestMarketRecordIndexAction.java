package io.ucoin.ucoinj.elasticsearch.action.market;

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

public class RestMarketRecordIndexAction extends BaseRestHandler {

    private static final ESLogger log = ESLoggerFactory.getLogger(RestMarketRecordIndexAction.class.getName());

    @Inject
    public RestMarketRecordIndexAction(Settings settings, RestController controller, Client client) {
        super(settings, controller, client);
        controller.registerHandler(POST, "/market/record", this);
    }

    @Override
    protected void handleRequest(final RestRequest request, RestChannel restChannel, Client client) throws Exception {

        try {
            String recordId = ServiceLocator.instance().getMarketRecordIndexerService().indexRecordFromJson(request.content().toUtf8());

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
package io.ucoin.ucoinj.elasticsearch.action.product;

import io.ucoin.ucoinj.elasticsearch.service.ServiceLocator;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.*;

import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestStatus.OK;

public class RestRecordIndexAction extends BaseRestHandler {

    private static final ESLogger log = ESLoggerFactory.getLogger(RestRecordIndexAction.class.getName());

    @Inject
    public RestRecordIndexAction(Settings settings, RestController controller, Client client) {
        super(settings, controller, client);
        controller.registerHandler(POST, "/store/record", this);
    }

    @Override
    protected void handleRequest(final RestRequest request, RestChannel restChannel, Client client) throws Exception {

        String productId = ServiceLocator.instance().getRecordIndexerService().indexRecordFromJson(request.content().toUtf8());

        restChannel.sendResponse(new BytesRestResponse(OK, productId));
    }

}
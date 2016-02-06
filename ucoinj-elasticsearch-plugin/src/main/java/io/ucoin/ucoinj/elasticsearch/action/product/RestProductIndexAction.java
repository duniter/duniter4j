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

public class RestProductIndexAction extends BaseRestHandler {

    private static final ESLogger log = ESLoggerFactory.getLogger(RestProductIndexAction.class.getName());

    @Inject
    public RestProductIndexAction(Settings settings, RestController controller, Client client) {
        super(settings, controller, client);
        controller.registerHandler(POST, "/product", this);
    }

    @Override
    protected void handleRequest(final RestRequest request, RestChannel restChannel, Client client) throws Exception {

        String productId = ServiceLocator.instance().getProductIndexerService().indexProductFromJson(request.content().toUtf8());

        restChannel.sendResponse(new BytesRestResponse(OK, productId));
    }

}
package io.ucoin.ucoinj.elasticsearch.action.currency;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.*;

import static org.elasticsearch.rest.RestStatus.OK;

public class RestCurrencyIndexAction extends BaseRestHandler {

    @Inject
    public RestCurrencyIndexAction(Settings settings, RestController controller, Client client) {
        super(settings, controller, client);
        controller.registerHandler(RestRequest.Method.POST, "/currency", this);
    }

    @Override
    protected void handleRequest(RestRequest restRequest, RestChannel restChannel, Client client) throws Exception {
        String json = restRequest.content().toUtf8();
        //ServiceLocator.instance().getCurrencyIndexerService().indexCurrency();
        String currencyName = "";
        restChannel.sendResponse(new BytesRestResponse(OK, currencyName));
    }

}
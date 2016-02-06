package io.ucoin.ucoinj.elasticsearch.action.security;

import io.ucoin.ucoinj.core.client.model.bma.gson.GsonUtils;
import io.ucoin.ucoinj.core.util.StringUtils;
import io.ucoin.ucoinj.elasticsearch.security.challenge.ChallengeMessageStore;
import io.ucoin.ucoinj.elasticsearch.security.token.SecurityTokenStore;
import io.ucoin.ucoinj.elasticsearch.service.ServiceLocator;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.*;

import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestStatus.FORBIDDEN;
import static org.elasticsearch.rest.RestStatus.OK;

public class RestSecurityAuthAction extends BaseRestHandler {

    private static final ESLogger log = ESLoggerFactory.getLogger(RestSecurityAuthAction.class.getName());

    private ChallengeMessageStore challengeMessageStore;
    private SecurityTokenStore securityTokenStore;

    @Inject
    public RestSecurityAuthAction(Settings settings, RestController controller, Client client,
                                  ChallengeMessageStore challengeMessageStore,
                                  SecurityTokenStore securityTokenStore) {
        super(settings, controller, client);
        this.challengeMessageStore = challengeMessageStore;
        this.securityTokenStore = securityTokenStore;
        controller.registerHandler(POST, "/auth", this);
    }

    @Override
    protected void handleRequest(final RestRequest request, RestChannel restChannel, Client client) throws Exception {

        AuthData authData = GsonUtils.newBuilder().create().fromJson(request.content().toUtf8(), AuthData.class);

        // TODO Authorization: Basic   instead ?

        if (StringUtils.isNotBlank(authData.pubkey)) {
            if (challengeMessageStore.validateChallenge(authData.challenge)) {
                boolean signatureOK = ServiceLocator.instance().getCryptoService().verify(authData.challenge, authData.signature, authData.pubkey);
                if (signatureOK) {
                    String token = securityTokenStore.createNewToken(authData.challenge, authData.signature, authData.pubkey);
                    restChannel.sendResponse(new BytesRestResponse(OK, token));
                    return;
                }
            }
        }

        restChannel.sendResponse(new BytesRestResponse(FORBIDDEN, Boolean.FALSE.toString()));
    }

    class AuthData {
        public String pubkey;
        public String challenge;
        public String signature;
    }
}
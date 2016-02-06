package io.ucoin.ucoinj.elasticsearch.action.security;

import io.ucoin.ucoinj.elasticsearch.security.challenge.ChallengeMessageStore;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.*;

import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestStatus.OK;

public class RestSecurityGetChallengeAction extends BaseRestHandler {

    private ChallengeMessageStore challengeMessageStore;

    @Inject
    public RestSecurityGetChallengeAction(Settings settings, RestController controller, Client client, ChallengeMessageStore challengeMessageStore) {
        super(settings, controller, client);
        this.challengeMessageStore = challengeMessageStore;
        controller.registerHandler(GET, "/auth", this);
    }

    @Override
    protected void handleRequest(final RestRequest request, RestChannel restChannel, Client client) throws Exception {
        restChannel.sendResponse(new BytesRestResponse(OK, challengeMessageStore.createNewChallenge()));
    }

}
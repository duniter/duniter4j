package org.duniter.elasticsearch.user.rest.user;

import org.duniter.elasticsearch.rest.security.RestSecurityController;
import org.duniter.elasticsearch.user.service.UserEventService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.rest.RestRequest;

/**
 * Created by blavenie on 13/12/16.
 */
public class RestUserEventSearchAction {

    @Inject
    public RestUserEventSearchAction(RestSecurityController securityController) {
        securityController.allow(RestRequest.Method.GET, String.format("/%s/%s/_search", UserEventService.INDEX, UserEventService.EVENT_TYPE));
        securityController.allow(RestRequest.Method.POST, String.format("/%s/%s/_search", UserEventService.INDEX, UserEventService.EVENT_TYPE));
        securityController.allow(RestRequest.Method.GET, String.format("/%s/%s/_count", UserEventService.INDEX, UserEventService.EVENT_TYPE));
        securityController.allow(RestRequest.Method.POST, String.format("/%s/%s/_count", UserEventService.INDEX, UserEventService.EVENT_TYPE));
    }
}

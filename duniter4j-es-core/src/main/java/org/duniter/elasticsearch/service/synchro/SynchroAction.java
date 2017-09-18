package org.duniter.elasticsearch.service.synchro;

import org.duniter.core.client.model.bma.EndpointApi;
import org.duniter.core.client.model.local.Peer;
import org.duniter.elasticsearch.model.SynchroResult;

public interface SynchroAction {

    EndpointApi getEndPointApi();

    void handleSynchronize(Peer peer,
                      long fromTime,
                      SynchroResult result);
}

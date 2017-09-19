package org.duniter.elasticsearch.synchro;

import org.duniter.core.client.model.bma.EndpointApi;
import org.duniter.core.client.model.local.Peer;
import org.duniter.elasticsearch.model.SynchroResult;
import org.duniter.elasticsearch.service.changes.ChangeEvent;
import org.duniter.elasticsearch.service.changes.ChangeSource;

public interface SynchroAction {

    EndpointApi getEndPointApi();

    ChangeSource getChangeSource();

    void handleSynchronize(Peer peer,
                      long fromTime,
                      SynchroResult result);

    void handleChange(Peer peer, ChangeEvent changeEvent);
}

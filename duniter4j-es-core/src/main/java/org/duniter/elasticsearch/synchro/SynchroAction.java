package org.duniter.elasticsearch.synchro;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import org.duniter.core.client.model.bma.EndpointApi;
import org.duniter.core.client.model.local.Peer;
import org.duniter.elasticsearch.model.SynchroResult;
import org.duniter.elasticsearch.service.changes.ChangeEvent;
import org.duniter.elasticsearch.service.changes.ChangeSource;

public interface SynchroAction {

    interface SourceConsumer {
        void accept(String id, JsonNode source, SynchroActionResult result) throws Exception;
    }

    EndpointApi getEndPointApi();

    ChangeSource getChangeSource();

    void handleSynchronize(Peer peer,
                      long fromTime,
                      SynchroResult result);

    void handleChange(Peer peer, ChangeEvent changeEvent);

    void addInsertionListener(SourceConsumer listener);

    void addUpdateListener(SourceConsumer listener);

    void addValidationListener(SourceConsumer listener);
}

package org.duniter.elasticsearch.service.synchro;

/*
 * #%L
 * Duniter4j :: ElasticSearch Plugin
 * %%
 * Copyright (C) 2014 - 2016 EIS
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.io.IOUtils;
import org.duniter.core.client.dao.CurrencyDao;
import org.duniter.core.client.dao.PeerDao;
import org.duniter.core.client.model.bma.EndpointApi;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.client.service.HttpService;
import org.duniter.core.service.CryptoService;
import org.duniter.core.util.CollectionUtils;
import org.duniter.core.util.DateUtils;
import org.duniter.core.util.Preconditions;
import org.duniter.core.util.websocket.WebsocketClientEndpoint;
import org.duniter.elasticsearch.PluginSettings;
import org.duniter.elasticsearch.client.Duniter4jClient;
import org.duniter.elasticsearch.dao.SynchroExecutionDao;
import org.duniter.elasticsearch.model.SynchroExecution;
import org.duniter.elasticsearch.model.SynchroResult;
import org.duniter.elasticsearch.service.AbstractService;
import org.duniter.elasticsearch.service.ServiceLocator;
import org.duniter.elasticsearch.service.changes.ChangeEvent;
import org.duniter.elasticsearch.service.changes.ChangeSource;
import org.duniter.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.common.inject.Inject;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by blavenie on 27/10/16.
 */
public class SynchroService extends AbstractService {

    private static final String WS_CHANGES_URL = "/ws/_changes";

    protected HttpService httpService;
    protected final Set<EndpointApi> peerApiFilters = Sets.newHashSet();
    protected final ThreadPool threadPool;
    protected final PeerDao peerDao;
    protected final CurrencyDao currencyDao;
    protected final SynchroExecutionDao synchroExecutionDao;
    private List<WebsocketClientEndpoint> wsClientEndpoints = Lists.newArrayList();
    private List<SynchroAction> actions = Lists.newArrayList();

    @Inject
    public SynchroService(Duniter4jClient client,
                          PluginSettings settings,
                          CryptoService cryptoService,
                          ThreadPool threadPool,
                          CurrencyDao currencyDao,
                          PeerDao peerDao,
                          SynchroExecutionDao synchroExecutionDao,
                          final ServiceLocator serviceLocator) {
        super("duniter.synchro", client, settings, cryptoService);
        this.threadPool = threadPool;
        this.currencyDao = currencyDao;
        this.peerDao = peerDao;
        this.synchroExecutionDao = synchroExecutionDao;
        threadPool.scheduleOnStarted(() -> {
            httpService = serviceLocator.getHttpService();
            setIsReady(true);
        });
    }

    public void register(SynchroAction action) {
        Preconditions.checkNotNull(action);
        Preconditions.checkNotNull(action.getEndPointApi());

        if (!peerApiFilters.contains(action.getEndPointApi())) {
            peerApiFilters.add(action.getEndPointApi());
        }
        actions.add(action);
    }

    /**
     * Start scheduling doc stats update
     * @return
     */
    public SynchroService startScheduling() {
        long delayBeforeNextHour = DateUtils.delayBeforeNextHour();

        // Five minute before the hour (to make sure to be ready when computing doc stat - see DocStatService)
        delayBeforeNextHour -= 5 * 60 * 1000;

        // If not already scheduling to early (in the next 5 min) then launch it
        if (delayBeforeNextHour > 5 * 60 * 1000) {

            // Launch with a delay of 10 sec
            threadPool.schedule(this::synchronize, 10 * 1000, TimeUnit.MILLISECONDS);
        }

        // Schedule every hour
        threadPool.scheduleAtFixedRate(
                this::synchronize,
                delayBeforeNextHour,
                60 * 60 * 1000 /* every hour */,
                TimeUnit.MILLISECONDS);

        return this;
    }

    /* -- protected methods -- */

    protected void synchronize() {
        logger.info("Starting synchronization...");

        // Closing all opened WS
        closeWsClientEndpoints();

        if (CollectionUtils.isNotEmpty(peerApiFilters)) {

            peerApiFilters.forEach(peerApiFilter -> {

                // Get peers
                List<Peer> peers = getPeersFromApi(peerApiFilter);
                if (CollectionUtils.isNotEmpty(peers)) {
                    peers.forEach(this::synchronize);
                    logger.info("Synchronization [OK]");
                } else {
                    logger.info(String.format("Synchronization [OK] - no endpoint found for API [%s]", peerApiFilter.name()));
                }
            });
        }
    }

    protected List<Peer> getPeersFromApi(final EndpointApi api) {
        Preconditions.checkNotNull(api);

        try {
            List<String> currencyIds = currencyDao.getCurrencyIds();
            if (CollectionUtils.isEmpty(currencyIds)) return null;

            return currencyIds.stream()
                    .map(currencyId -> peerDao.getPeersByCurrencyIdAndApi(currencyId, api.name()))
                    .filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        }
        catch (Exception e) {
            logger.error(String.format("Could not get peers for Api [%s]", api.name()), e);
            return null;
        }
    }

    protected void synchronize(final Peer peer) {
        long now = System.currentTimeMillis();
        SynchroResult result = new SynchroResult();

        long fromTime = getLastExecutionTime(peer);

        // If not the first synchro, add a delay to last execution time
        // to avoid missing data because incorrect clock configuration
        if (fromTime > 0) {
            fromTime -= Math.abs(pluginSettings.getSynchroTimeOffset());
        }

        ChangeSource changeSourceToListen = new ChangeSource();

        // insert
        for (SynchroAction action: actions) {

            action.handleSynchronize(peer, fromTime, result);
        }
        //synchronize(peer, fromTime, result, changeSourceToListen);

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("[%s] [%s] User data imported in %s ms: %s", peer.getCurrency(), peer, System.currentTimeMillis() - now, result.toString()));
        }

        saveExecution(peer, result);

        // Listens changes on this peer
        //startListenChanges(peer);

    }

    protected long getLastExecutionTime(Peer peer) {
        Preconditions.checkNotNull(peer);
        Preconditions.checkNotNull(peer.getId());

        try {
            SynchroExecution execution = synchroExecutionDao.getLastExecution(peer);
            return execution != null ? execution.getTime() : 0;
        }
        catch (Exception e) {
            logger.error(String.format("Error while saving last synchro execution time, for peer [%s]. Will resync all.", peer), e);
            return 0;
        }
    }

    protected void saveExecution(Peer peer, SynchroResult result) {
        Preconditions.checkNotNull(peer);
        Preconditions.checkNotNull(peer.getId());
        Preconditions.checkNotNull(result);

        try {
            SynchroExecution execution = new SynchroExecution();
            execution.setCurrency(peer.getCurrency());
            execution.setPeer(peer.getId());
            execution.setResult(result);

            // Last execution time (in seconds)
            long executionTime = System.currentTimeMillis()/1000;
            execution.setTime(executionTime);

            synchroExecutionDao.save(execution);
        }
        catch (Exception e) {
            logger.error(String.format("Error while saving synchro execution on peer [%s]", peer), e);
        }
    }

    protected void closeWsClientEndpoints() {
        // Closing all opened WS
        wsClientEndpoints.forEach(IOUtils::closeQuietly);
        wsClientEndpoints.clear();
    }

    protected void listenChanges(final Peer peer, ChangeSource changeSource) {
        // Listens changes on this peer
        Preconditions.checkNotNull(peer);
        Preconditions.checkNotNull(changeSource);

        // Get (or create) the websocket endpoint
        WebsocketClientEndpoint wsClientEndPoint = httpService.getWebsocketClientEndpoint(peer, WS_CHANGES_URL, false);

        // filter on selected sources
        wsClientEndPoint.sendMessage(changeSource.toString());

        // add listener
        wsClientEndPoint.registerListener( message -> {
            try {
                ChangeEvent changeEvent = getObjectMapper().readValue(message, ChangeEvent.class);
                importChangeEvent(peer, changeEvent);
            } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.warn(String.format("[%s] Unable to process changes received by [/ws/_changes]: %s", peer, e.getMessage()), e);
                }
                else {
                    logger.warn(String.format("[%s] Unable to process changes received by [/ws/_changes]: %s", peer, e.getMessage()));
                }
            }
        });

        // Add to list
        wsClientEndpoints.add(wsClientEndPoint);
    }

    protected void importChangeEvent(final Peer peer, ChangeEvent changeEvent) {
        Preconditions.checkNotNull(changeEvent);
        Preconditions.checkNotNull(changeEvent.getOperation());

        // Skip delete operation (imported by history/delete)
        if (changeEvent.getOperation() == ChangeEvent.Operation.DELETE) return;

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("[%s] [%s/%s] Received change event", peer, changeEvent.getIndex(), changeEvent.getType()));
        }
        changeEvent.getSource();
    }
}

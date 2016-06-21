package org.duniter.elasticsearch.threadpool;

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
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.component.Lifecycle;
import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.concurrent.EsAbortPolicy;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.transport.TransportService;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Manage thread pool, to execute tasks asynchronously.
 * Created by eis on 17/06/16.
 */
public class ThreadPool extends AbstractLifecycleComponent<ThreadPool> {

    private ScheduledThreadPoolExecutor scheduler = null;
    private Injector injector;

    private final List<Runnable> afterStartedCommands;

    @Inject
    public ThreadPool(Settings settings,
                      Injector injector
                        ) {
        super(settings);
        this.injector = injector;
        this.afterStartedCommands = Lists.newArrayList();

        this.scheduler = new ScheduledThreadPoolExecutor(1, EsExecutors.daemonThreadFactory(settings, "duniter4j-scheduler"), new EsAbortPolicy());
        this.scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        this.scheduler.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        this.scheduler.setRemoveOnCancelPolicy(true);
    }

    public void doStart(){
        if (logger.isDebugEnabled()) {
            logger.debug("Starting Duniter4j ThreadPool...");
        }

        if (!afterStartedCommands.isEmpty()) {
            scheduleOnStarted(() -> {
                for (Runnable command: afterStartedCommands) {
                    command.run();
                }
                this.afterStartedCommands.clear();
            });
        }
    }

    public void doStop(){
        scheduler.shutdown();
        // TODO : cancel all aiting jobs
    }

    public void doClose() {}

    public ScheduledExecutorService scheduler() {
        return this.scheduler;
    }

    /**
     * Schedules an action when node is started (all services and modules ready)
     *
     * @param command the action to take
     * @return a ScheduledFuture who's get will return when the task is complete and throw an exception if it is canceled
     */
    public void scheduleOnStarted(Runnable command) {
        /*if (lifecycle.state() == Lifecycle.State.INITIALIZED ) {
            afterStartedCommands.add(command);
        }
        else {*/
            scheduleAfterServiceState(TransportService.class, Lifecycle.State.STARTED, command);
       // }
    }

    /**
     * Schedules an action that runs on the scheduler thread, after a delay.
     *
     * @param command the action to take
     * @param interval the delay interval
     * @return a ScheduledFuture who's get will return when the task is complete and throw an exception if it is canceled
     */
    public ScheduledFuture<?> schedule(Runnable command, TimeValue interval) {
        return scheduler.schedule(new LoggingRunnable(command), interval.millis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Schedules a periodic action that always runs on the scheduler thread.
     *
     * @param command the action to take
     * @param interval the delay interval
     * @return a ScheduledFuture who's get will return when the task is complete and throw an exception if it is canceled
     */
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, TimeValue interval) {
        return scheduler.scheduleWithFixedDelay(new LoggingRunnable(command), interval.millis(), interval.millis(), TimeUnit.MILLISECONDS);
    }


    /* -- protected methods  -- */

    protected <T extends LifecycleComponent<T>> ScheduledFuture<?> scheduleAfterServiceState(Class<T> waitingServiceClass, final Lifecycle.State waitingState, final Runnable job) {
        final T service = injector.getInstance(waitingServiceClass);
        return schedule(() -> {
            while(service.lifecycleState() != waitingState) {
                try {
                    Thread.sleep(100); // wait 100 ms
                }
                catch(InterruptedException e) {
                }
            }

            // continue
            job.run();
        }, TimeValue.timeValueSeconds(10));
    }


    /*public void resetAllData() {
        //resetAllCurrencies();
        //resetDataBlocks();
        //resetMarketRecords();
        //resetRegistry();
    }

    public void resetAllCurrencies() {
        currencyRegistryService.deleteAllCurrencies();
    }

    public void resetDataBlocks() {
        BlockchainRemoteService blockchainService = serviceLocator.getBlockchainRemoteService();
        Peer peer = checkConfigAndGetPeer(pluginSettings);

        try {
            // Get the blockchain name from node
            BlockchainParameters parameter = blockchainService.getParameters(peer);
            if (parameter == null) {
                logger.error(String.format("Could not connect to node [%s:%s]",
                        pluginSettings.getNodeBmaHost(), pluginSettings.getNodeBmaPort()));
                return;
            }
            String currencyName = parameter.getCurrency();

            logger.info(String.format("Reset data for index [%s]", currencyName));

            // Delete then create index on blockchain
            boolean indexExists = blockBlockchainService.existsIndex(currencyName);
            if (indexExists) {
                blockBlockchainService.deleteIndex(currencyName);
                blockBlockchainService.createIndex(currencyName);
            }


            logger.info(String.format("Successfully reset data for index [%s]", currencyName));
        } catch(Exception e) {
            logger.error("Error during reset data: " + e.getMessage(), e);
        }
    }

    public void resetMarketRecords() {
        try {
            // Delete then create index on records
            boolean indexExists = recordMarketService.existsIndex();
            if (indexExists) {
                recordMarketService.deleteIndex();
            }
            logger.info(String.format("Successfully reset market records"));

            categoryMarketService.createIndex();
            categoryMarketService.initCategories();
            logger.info(String.format("Successfully re-initialized market categories data"));

        } catch(Exception e) {
            logger.error("Error during reset market records: " + e.getMessage(), e);
        }
    }

    public void resetRegistry() {
        try {
            // Delete then create index on records
            if (recordRegistryService.existsIndex()) {
                recordRegistryService.deleteIndex();
            }
            recordRegistryService.createIndex();
            logger.info(String.format("Successfully reset registry records"));


            if (categoryRegistryService.existsIndex()) {
                categoryRegistryService.deleteIndex();
            }
            categoryRegistryService.createIndex();
            categoryRegistryService.initCategories();
            logger.info(String.format("Successfully re-initialized registry categories"));

            if (citiesRegistryService.existsIndex()) {
                citiesRegistryService.deleteIndex();
            }
            citiesRegistryService.initCities();
            logger.info(String.format("Successfully re-initialized registry cities"));

        } catch(Exception e) {
            logger.error("Error during reset registry records: " + e.getMessage(), e);
        }
    }*/

    /* -- internal methods -- */

    class LoggingRunnable implements Runnable {

        private final Runnable runnable;

        LoggingRunnable(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public void run() {
            try {
                runnable.run();
            } catch (Throwable t) {
                logger.warn("failed to run {}", t, runnable.toString());
                throw t;
            }
        }

        @Override
        public int hashCode() {
            return runnable.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return runnable.equals(obj);
        }

        @Override
        public String toString() {
            return "[threaded] " + runnable.toString();
        }
    }
}

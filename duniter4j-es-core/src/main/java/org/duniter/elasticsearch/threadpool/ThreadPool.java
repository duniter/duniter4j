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

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.elasticsearch.action.admin.cluster.stats.ClusterStatsRequestBuilder;
import org.elasticsearch.action.admin.cluster.stats.ClusterStatsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.component.Lifecycle;
import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.concurrent.EsAbortPolicy;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.transport.TransportService;
import org.nuiton.i18n.I18n;

import java.util.List;
import java.util.concurrent.*;

/**
 * Manage thread pool, to execute tasks asynchronously.
 * Created by eis on 17/06/16.
 */
public class ThreadPool extends AbstractLifecycleComponent<ThreadPool> {

    private ScheduledThreadPoolExecutor scheduler = null;
    private Injector injector;
    private ESLogger logger = Loggers.getLogger("threadpool");

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
     * Schedules an rest when node is started (all services and modules ready)
     *
     * @param job the rest to execute when node started
     * @return a ScheduledFuture who's get will return when the task is complete and throw an exception if it is canceled
     */
    public void scheduleOnStarted(Runnable job) {
        Preconditions.checkNotNull(job);
        scheduleAfterServiceState(TransportService.class, Lifecycle.State.STARTED, job);
    }

    /**
     * Schedules an rest when cluster is ready
     *
     * @param job the rest to execute
     * @param expectedStatus expected health status, to run the job
     * @return a ScheduledFuture who's get will return when the task is complete and throw an exception if it is canceled
     */
    public void scheduleOnClusterHealthStatus(Runnable job, ClusterHealthStatus... expectedStatus) {
        Preconditions.checkNotNull(job);

        scheduleOnStarted(() -> {
            if (waitClusterHealthStatus(expectedStatus)) {
                // continue
                job.run();
            }
        });
    }

    /**
     * Schedules an rest that runs on the scheduler thread, after a delay.
     *
     * @param command the rest to take
     * @param interval the delay interval
     * @return a ScheduledFuture who's get will return when the task is complete and throw an exception if it is canceled
     */
    public ScheduledFuture<?> schedule(Runnable command, TimeValue interval) {
        return scheduler.schedule(new LoggingRunnable(command), interval.millis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Schedules a periodic rest that always runs on the scheduler thread.
     *
     * @param command the rest to take
     * @param interval the delay interval
     * @return a ScheduledFuture who's get will return when the task is complete and throw an exception if it is canceled
     */
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, TimeValue interval) {
        return scheduler.scheduleWithFixedDelay(new LoggingRunnable(command), interval.millis(), interval.millis(), TimeUnit.MILLISECONDS);
    }


    /* -- protected methods  -- */

    protected <T extends LifecycleComponent<T>> ScheduledFuture<?> scheduleAfterServiceState(Class<T> waitingServiceClass,
                                                                                             final Lifecycle.State waitingState,
                                                                                             final Runnable job) {
        Preconditions.checkNotNull(waitingServiceClass);
        Preconditions.checkNotNull(waitingState);
        Preconditions.checkNotNull(job);

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

    public boolean waitClusterHealthStatus(ClusterHealthStatus... expectedStatus) {
        Preconditions.checkNotNull(expectedStatus);
        Preconditions.checkArgument(expectedStatus.length > 0);

        Client client = injector.getInstance(Client.class);
        ClusterStatsRequestBuilder statsRequest = client.admin().cluster().prepareClusterStats();
        ClusterStatsResponse stats = null;
        boolean canContinue = false;
        boolean firstTry = true;
        while (!canContinue) {
            try {
                if (stats != null) Thread.sleep(100); // wait 100 ms
                stats = statsRequest.execute().get();
                for (ClusterHealthStatus status: expectedStatus) {
                    if (stats.getStatus() == status) {
                        if (!firstTry && logger.isDebugEnabled()) {
                            logger.debug(I18n.t("duniter4j.threadPool.clusterHealthStatus.changed", status.name()));
                        }
                        canContinue = true;
                        break;
                    }
                }
                firstTry = false;
            } catch (ExecutionException e) {
                // Continue
            } catch (InterruptedException e) {
                return false; // stop
            }
        }

        return canContinue;
    }

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

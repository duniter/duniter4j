package org.duniter.elasticsearch.threadpool;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.util.concurrent.EsAbortPolicy;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RetryPolicy extends EsAbortPolicy {

    private final ESLogger logger;
    private final long retryDelay;
    private final TimeUnit timeUnit;

    public RetryPolicy(long retryDelay, TimeUnit timeUnit) {
        this(Loggers.getLogger("duniter.threadpool.policy"), retryDelay, timeUnit);
    }

    public RetryPolicy(ESLogger logger, long retryDelay, TimeUnit timeUnit) {
        super();
        this.logger = logger;
        this.retryDelay = retryDelay;
        this.timeUnit = timeUnit;
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {

        if (executor instanceof ScheduledThreadPoolExecutor) {
            ScheduledThreadPoolExecutor scheduledExecutorService = (ScheduledThreadPoolExecutor)executor;
            scheduledExecutorService.schedule(r, retryDelay, timeUnit);
            logger.warn(String.format("Scheduler queue is full (max pool size = %s). Will retry later...",
                    executor.getMaximumPoolSize()));
        }
        else {
            logger.error("Scheduler queue is full (max pool size = %s). Operation is rejected !");
            super.rejectedExecution(r, executor);
        }
    }

}
package org.duniter.elasticsearch.threadpool;

import org.elasticsearch.common.logging.ESLogger;

import java.util.concurrent.*;

public class LoggingScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor {

    private final ESLogger logger;

    public LoggingScheduledThreadPoolExecutor(ESLogger logger,
                                              int corePoolSize,
                                              ThreadFactory threadFactory,
                                              RejectedExecutionHandler handler) {
        super(corePoolSize, threadFactory, handler);
        this.logger =logger;
    }

   protected <V> RunnableScheduledFuture<V> decorateTask(
                Runnable r, RunnableScheduledFuture<V> task) {
       return new LoggingTask<V>(logger, task);
   }

   protected <V> RunnableScheduledFuture<V> decorateTask(
           Callable<V> c, RunnableScheduledFuture<V> task) {
       return new LoggingTask<V>(logger, task);
   }


    static class LoggingTask<V> implements RunnableScheduledFuture<V> {
        private final RunnableScheduledFuture<V> task;
        private final ESLogger logger;

        public LoggingTask(ESLogger logger, RunnableScheduledFuture<V> task) {
            this.task = task;
            this.logger = logger;
        }

        @Override
        public void run() {
            try {
                task.run();
            } catch (Throwable e) {
                logger.warn(String.format("Failed to execute a task: %s", e.getMessage()), e);
            }
        }

        @Override
        public boolean isPeriodic() {
            return task.isPeriodic();
        }

        @Override
        public int compareTo(Delayed o) {
            return task.compareTo(o);
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return task.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return task.isCancelled();
        }

        @Override
        public boolean isDone() {
            return task.isDone();
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {
           return task.get();
        }

        @Override
        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return task.get(timeout, unit);
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return task.getDelay(unit);
        }
    }

}
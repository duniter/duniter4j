package org.duniter.core.util;

import com.google.common.collect.MapMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A concurrent lock manager
 */
public class LockManager {

    private static final Logger log = LoggerFactory.getLogger(LockManager.class);
    private static int DEFAULT_UNSUCCESSFUL_TRY_LOCK_WARN = 4;
    private static int DEFAULT_CONCURRENCY_LEVEL = 4;

    private final int unsuccessfulLockCountForWarn;
    private final Map<String, Lock> lockMap;
    private final Map<String, Integer> tryLockCounter;

    public LockManager(int concurrencyLevel, int maxLockCounterForLog) {
        this.unsuccessfulLockCountForWarn = maxLockCounterForLog;
        this.lockMap = new MapMaker().concurrencyLevel(concurrencyLevel).makeMap();
        this.tryLockCounter = new MapMaker().concurrencyLevel(concurrencyLevel).makeMap();
    }

    public LockManager(int concurrencyLevel) {
        this(concurrencyLevel, concurrencyLevel);
    }

    public LockManager() {
        this(DEFAULT_CONCURRENCY_LEVEL, DEFAULT_UNSUCCESSFUL_TRY_LOCK_WARN);
    }

   /**
    * Acquires the lock.
    *
    */
    public void lock(String name) {
        Lock lock = computeIfAbsent(name);
        lock.lock();
    }

    public boolean tryLock(final String name, long time, TimeUnit unit) throws InterruptedException {
        Lock lock = computeIfAbsent(name);
        boolean locked = lock.tryLock(time, unit);
        logTryLock(name, locked);
        return locked;
    }

    public boolean tryLock(final String name) {
        Lock lock = computeIfAbsent(name);
        boolean locked = lock.tryLock();
        logTryLock(name, locked);
        return locked;
    }

    public void unlock(final String name) {
        Lock lock = lockMap.get(name);
        if (lock != null) {
            lock.unlock();
            // Reset counter
            tryLockCounter.computeIfPresent(name,  (input, counter) -> 0);
        }
    }

    public boolean isLocked(String name) {
        Integer tryLockCount = tryLockCounter.get(name);
        return tryLockCount != null && tryLockCount.intValue() > 0;
    }

    /* -- protected method -- */

    protected Lock computeIfAbsent(final String name) {
        return  lockMap.computeIfAbsent(name, input -> new ReentrantLock());
    }

    protected void logTryLock(final String name, final boolean locked) {
        // Counter unsuccessful lock
        if (locked) {
            // Reset counter
            tryLockCounter.computeIfPresent(name, (input, counter) -> 1);
        }
        else {
            if (!tryLockCounter.containsKey(name)) {
                tryLockCounter.computeIfAbsent(name, input -> 2);
            }
            else {
                tryLockCounter.computeIfPresent(name,  (input, counter) -> {
                    if (counter < unsuccessfulLockCountForWarn) return counter + 1;
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Unable to acquire lock [%s] - after %s unsuccessful attempts", name, counter + 1));
                    }
                    return 1; // reset log counter
                });

            }
        }
    }
}
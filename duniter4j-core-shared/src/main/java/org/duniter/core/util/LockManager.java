package org.duniter.core.util;

/*-
 * #%L
 * Duniter4j :: Core Shared
 * %%
 * Copyright (C) 2014 - 2017 EIS
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
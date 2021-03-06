/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.time;

import com.google.common.annotations.VisibleForTesting;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A clock that is guaranteed to not go backwards.
 * <p>
 * It aims to strike a balance between never going backwards (allowing timestamps to represent causality)
 * and keeping in sync with the system wall clock so that time values make sense in comparison with the system wall clock,
 * including timestamps generated from other processes.
 * <p>
 * This clock effectively measures time by duration (according to System.nanoTime()),
 * in between syncs with the system wall clock.
 * When issuing the first timestamp after the sync interval has expired,
 * The system wall clock will be read, and the current time set to the max of wall clock time or the most recently issued timestamp.
 * All other timestamps are calculated as the wall clock time at last sync + elapsed time since.
 * <p>
 * This clock deals relatively well when the system wall clock shift is adjusted by small amounts.
 * It also deals relatively well when the system wall clock jumps forward by large amounts (this clock will jump with it).
 * It does not deal as well with large jumps back in time.
 * <p>
 * When the system wall clock jumps back in time, this clock will effectively slow down until it is back in sync.
 * All syncing timestamps will be the same as the previously issued timestamp.
 * The rate by which this clock slows, and therefore the time it takes to resync,
 * is determined by how frequently the clock is read.
 * If timestamps are only requested at a rate greater than the sync interval,
 * all timestamps will have the same value until the clocks synchronize (i.e. this clock will pause).
 * If timestamps are requested more frequently than the sync interval,
 * timestamps before and after the sync point will under represent the actual elapsed time,
 * gradually bringing the clocks back into sync.
 */
class MonotonicClock implements Clock {

    private static final long SYNC_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(3);

    /**
     * This determines the minimum elapsed time between syncs.
     * The coordination strategy between different book keeping variables
     * relies on this being greater than
     */
    private final long syncIntervalMillis;
    private final TimeSource timeSource;

    private final AtomicLong syncMillisRef;
    private final AtomicLong syncNanosRef;
    private final AtomicLong max = new AtomicLong(0);

    MonotonicClock() {
        this(TimeSource.SYSTEM, SYNC_INTERVAL_MILLIS);
    }

    @VisibleForTesting
    MonotonicClock(TimeSource timeSource, long syncIntervalMillis) {
        long nanoTime = timeSource.nanoTime();
        long currentTimeMillis = timeSource.currentTimeMillis();

        this.timeSource = timeSource;
        this.syncIntervalMillis = syncIntervalMillis;
        this.syncNanosRef = new AtomicLong(nanoTime);
        this.syncMillisRef = new AtomicLong(currentTimeMillis);
        this.max.set(currentTimeMillis);
    }

    public long getCurrentTime() {
        long nowNanos = timeSource.nanoTime();
        long syncNanos = syncNanosRef.get();
        long syncMillis = syncMillisRef.get();
        long sinceSyncNanos = nowNanos - syncNanos;
        long sinceSyncMillis = TimeUnit.NANOSECONDS.toMillis(sinceSyncNanos);

        if (syncIsDue(nowNanos, syncNanos, sinceSyncMillis)) {
            return sync(syncMillis);
        } else {
            return advance(syncMillis + sinceSyncMillis);
        }
    }

    private boolean syncIsDue(long nowNanos, long syncNanos, long sinceSyncMillis) {
        return sinceSyncMillis >= syncIntervalMillis && syncNanosRef.compareAndSet(syncNanos, nowNanos);
    }

    private long sync(long syncMillis) {
        long newSyncMillis = advance(timeSource.currentTimeMillis());
        // CAS due to potentially a later, but overlapping, sync having already completed
        syncMillisRef.compareAndSet(syncMillis, newSyncMillis);
        return newSyncMillis;
    }

    private long advance(long timestamp) {
        long prev;
        long next;
        do {
            prev = max.get();
            next = Math.max(prev, timestamp);
        } while (!max.compareAndSet(prev, next));

        return next;
    }

}

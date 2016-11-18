/*
 * Copyright 2016 Centro, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package net.centro.rtb.monitoringcenter.metrics.system.jvm;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Timer;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.List;
import java.util.Map;

/**
 * This interface encompasses the current readings for various JVM data points, involving such realms as threads,
 * classes, buffer pools, memory utilization, and garbage collection. All metrics exposed by this class are immutable
 * by the client.
 */
public interface JvmStatus {
    /**
     * Retrieves the total number of classes loaded since the JVM started execution.
     *
     * @see ClassLoadingMXBean#getTotalLoadedClassCount()
     * @return a gauge holding the total number of classes loaded in the lifetime of this JVM.
     */
    Gauge<Long> getTotalLoadedClassesGauge();

    /**
     * Retrieves the total number of classes unloaded since the JVM started execution.
     *
     * @see ClassLoadingMXBean#getUnloadedClassCount()
     * @return a gauge holding the total number of classes unloaded since the JVM started execution.
     */
    Gauge<Long> getUnloadedClassesGauge();

    /**
     * Retrieves the number of classes that are currently loaded in the JVM.
     *
     * @see ClassLoadingMXBean#getLoadedClassCount()
     * @return a gauge holding the number of classes that are currently loaded in the JVM.
     */
    Gauge<Integer> getCurrentlyLoadedClassesGauge();

    /**
     * Retrieves the current number of live threads including both daemon and non-daemon threads.
     *
     * @see ThreadMXBean#getThreadCount()
     * @return a gauge holding the current number of live threads.
     */
    Gauge<Integer> getCurrentThreadsGauge();

    /**
     * Retrieves the peak live thread count since the JVM started or peak was reset.
     *
     * @see ThreadMXBean#getPeakThreadCount()
     * @return a gauge holding the peak live thread count.
     */
    Gauge<Integer> getPeakThreadsGauge();

    /**
     * Retrieves the current number of live daemon threads.
     *
     * @see ThreadMXBean#getDaemonThreadCount()
     * @return a gauge holding the current number of live daemon threads.
     */
    Gauge<Integer> getDaemonThreadsGauge();

    /**
     * Retrieves the number of threads that are currently deadlocked.
     *
     * @see ThreadMXBean#findDeadlockedThreads()
     * @return a gauge holding the number of threads that are currently deadlocked.
     */
    Gauge<Integer> getDeadlockedThreadsGauge();

    /**
     * Retrieves current thread counts by thread states.
     *
     * @return a map of gauges holding current thread counts by thread states.
     */
    Map<Thread.State, Gauge<Integer>> getThreadsGaugesByThreadStates();

    /**
     * Retrieves a list of buffer pool statuses. An empty list is returned if no buffer pool data is available.
     *
     * @return a list of buffer pool statuses.
     */
    List<BufferPoolStatus> getBufferPoolStatuses();

    /**
     * Retrieves the number of full garbage collections since the JVM started execution.
     *
     * @return a gauge holding the number of full garbage collections; <tt>null</tt> if not available.
     */
    Gauge<Long> getFullGcCollectionsGauge();

    /**
     * Retrieves the timer for major garbage collections.
     *
     * @return the timer for major garbage collections.
     */
    Timer getMajorGcTimer();

    /**
     * Retrieves the timer for minor garbage collections.
     *
     * @return the timer for minor garbage collections.
     */
    Timer getMinorGcTimer();

    /**
     * Retrieves a list of garbage collector statuses. An empty list is returned if no garbage collector data is
     * available.
     *
     * @return a list of garbage collector statuses.
     */
    List<GarbageCollectorStatus> getGarbageCollectorStatuses();

    /**
     * Retrieves the total memory usage status, including both heap and non-heap spaces.
     *
     * @return the total memory usage status.
     */
    MemoryUsageStatus getTotalMemoryUsageStatus();

    /**
     * Retrieves the memory usage status of the heap space.
     *
     * @see MemoryMXBean#getHeapMemoryUsage()
     * @return the memory usage status of the heap space.
     */
    MemoryUsageStatus getHeapMemoryUsageStatus();

    /**
     * Retrieves the memory usage status of the non-heap space.
     *
     * @see MemoryMXBean#getNonHeapMemoryUsage()
     * @return the memory usage status of the non-heap space.
     */
    MemoryUsageStatus getNonHeapMemoryUsageStatus();

    /**
     * Retrieves a list of memory pool statuses. An empty list is returned if no memory pool data is available.
     *
     * @return a list of memory pool statuses.
     */
    List<MemoryPoolStatus> getMemoryPoolStatuses();

    /**
     * Retrieves the uptime of the JVM in milliseconds.
     *
     * @see RuntimeMXBean#getUptime()
     * @return a gauge holding the uptime of the Java virtual machine in milliseconds.
     */
    Gauge<Long> getUptimeInMillisGauge();
}

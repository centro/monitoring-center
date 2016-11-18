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

import java.lang.management.MemoryUsage;

/**
 * This interface encompasses the current readings for JVM memory usage for a particular memory pool or for the entire
 * heap or non-heap space. All metrics exposed by this class are immutable by the client.
 *
 * @see java.lang.management.MemoryUsage
 */
public interface MemoryUsageStatus {
    /**
     * Retrieves the amount of memory in bytes that the JVM initially requests from the operating system for memory
     * management.
     *
     * @see MemoryUsage#getInit()
     * @return a gauge holding the initial size of memory in bytes; <tt>null</tt> if not available.
     */
    Gauge<Long> getInitialSizeInBytesGauge();

    /**
     * Retrieves the amount of used memory in bytes.
     *
     * @see MemoryUsage#getUsed()
     * @return a gauge holding the amount of used memory in bytes.
     */
    Gauge<Long> getUsedMemoryInBytesGauge();

    /**
     * Retrieves the maximum amount of memory in bytes that can be used for memory management.
     *
     * @see MemoryUsage#getMax()
     * @return a gauge holding the maximum amount of memory in bytes; <tt>null</tt> if not available.
     */
    Gauge<Long> getMaxAvailableMemoryInBytesGauge();

    /**
     * Retrieves the amount of memory in bytes that is committed (i.e., guaranteed) for the JVM to use.
     *
     * @see MemoryUsage#getCommitted()
     * @return a gauge holding the amount of memory in bytes that is committed (i.e., guaranteed) for the JVM to use.
     */
    Gauge<Long> getCommittedMemoryInBytesGauge();

    /**
     * Retrieves the percentage of used memory. The divisor is either the maximum available amount of memory or the
     * committed amount of memory, depending on the availability of the former. The returned value is in the range
     * from 0 to 100.
     *
     * @return a gauge holding the percentage of used memory; <tt>null</tt> if not applicable.
     */
    Gauge<Double> getUsedMemoryPercentageGauge();
}

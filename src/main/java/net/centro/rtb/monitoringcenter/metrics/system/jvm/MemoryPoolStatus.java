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

import java.lang.management.MemoryPoolMXBean;

/**
 * This interface encompasses the current readings for a JVM memory pool. All metrics exposed by this class are
 * immutable by the client.
 *
 * @see java.lang.management.MemoryPoolMXBean
 */
public interface MemoryPoolStatus {
    /**
     * Retrieves the name of this memory pool.
     *
     * @return the name of this memory pool.
     */
    String getName();

    /**
     * Retrieves the memory usage status.
     *
     * @return the memory usage status.
     */
    MemoryUsageStatus getMemoryUsageStatus();

    /**
     * Retrieves the amount of used memory in bytes after the most recent garbage collection.
     *
     * @see MemoryPoolMXBean#getCollectionUsage()
     * @return a gauge holding the amount of used memory in bytes after the most recent garbage collection;
     * <tt>null</tt> if not available.
     */
    Gauge<Long> getUsedAfterGcInBytesGauge();
}
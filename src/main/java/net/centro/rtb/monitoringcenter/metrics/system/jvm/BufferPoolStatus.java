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

import java.lang.management.BufferPoolMXBean;

/**
 * This interface represents the current readings for a JVM buffer pool. All metrics exposed by this class are immutable
 * by the client.
 *
 * @see java.lang.management.BufferPoolMXBean
 */
public interface BufferPoolStatus {
    /**
     * Retrieves the name of the buffer pool.
     *
     * @return the name of the buffer pool.
     */
    String getName();

    /**
     * Retrieves an estimate of the number of buffers in the pool.
     *
     * @see BufferPoolMXBean#getCount()
     * @return a gauge holding an estimate of the number of buffers in the pool.
     */
    Gauge<Long> getSizeGauge();

    /**
     * Retrieves an estimate of the total capacity of buffers in the pool in bytes.
     *
     * @see BufferPoolMXBean#getTotalCapacity()
     * @return a gauge holding an estimate of the total capacity of buffers in the pool in bytes.
     */
    Gauge<Long> getTotalCapacityInBytesGauge();

    /**
     * Retrieves an estimate of the memory that the JVM is using for this buffer pool in bytes.
     *
     * @see BufferPoolMXBean#getMemoryUsed()
     * @return a gauge holding an estimate of the memory that the JVM is using for this buffer pool in bytes;
     * <tt>null</tt> if not available.
     */
    Gauge<Long> getUsedMemoryInBytesGauge();
}

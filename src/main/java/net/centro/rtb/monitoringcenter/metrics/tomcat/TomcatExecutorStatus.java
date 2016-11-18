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

package net.centro.rtb.monitoringcenter.metrics.tomcat;

import com.codahale.metrics.Gauge;

/**
 * This interface reflects the current readings for a Tomcat's custom-defined executor. The values are collected from
 * a Tomcat's Executor JMX bean. All metrics exposed by this class are immutable by the client.
 */
public interface TomcatExecutorStatus {
    /**
     * Retrieves the name of the executor.
     *
     * @return the name of the executor.
     */
    String getName();

    /**
     * Retrieves the current number of threads in the thread pool utilized by the executor.
     *
     * @return a gauge holding the current number of threads in the thread pool utilized by the executor; <tt>null</tt>
     * if not available.
     */
    Gauge<Integer> getCurrentPoolSizeGauge();

    /**
     * Retrieves the maximum size of the thread pool utilized by the executor.
     *
     * @return a gauge holding the maximum size of the thread pool utilized by the executor; <tt>null</tt> if
     * not available.
     */
    Gauge<Integer> getMaxPoolSizeGauge();

    /**
     * Retrieves the number of busy threads in the thread pool utilized by the executor.
     *
     * @return a gauge holding the number of busy threads in the thread pool utilized by the executor; <tt>null</tt> if
     * not available.
     */
    Gauge<Integer> getBusyThreadsGauge();

    /**
     * Retrieves the current size of the pending tasks queue.
     *
     * @return a gauge holding the current size of the pending tasks queue; <tt>null</tt> if not available.
     */
    Gauge<Integer> getQueueSizeGauge();

    /**
     * Retrieves the capacity of the pending tasks queue.
     *
     * @return a gauge holding the capacity of the pending tasks queue; <tt>null</tt> if not available.
     */
    Gauge<Integer> getQueueCapacityGauge();
}

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
 * This interface reflects the current readings for a Tomcat's connector. The values are collected from Tomcat's
 * ThreadPool and GlobalRequestProcessor JMX beans. All metrics exposed by this class are immutable by the client.
 */
public interface TomcatConnectorStatus {
    /**
     * Retrieves the name of the connector. For instance, "http-apr-80".
     *
     * @return the name of the connector.
     */
    String getName();

    /**
     * Retrieves the port associated with the connector.
     *
     * @return the port associated with the connector; <tt>null</tt> if there was a problem retrieving the value via
     * JMX.
     */
    Integer getPort();

    /**
     * Indicates whether this connector is deployed under internal port (a private port) or not.
     *
     * @return <tt>true</tt> if this connector is deployed under internal port; <tt>false</tt> otherwise or if unknown.
     */
    boolean isInternalPort();

    /**
     * Indicates whether this connector is secure (SSL/TLS) or not.
     *
     * @return <tt>true</tt> if this connector is secure (SSL/TLS); <tt>false</tt> otherwise or if unknown.
     */
    boolean isSecure();

    /**
     * Indicates whether this connector is an AJP connector or not.
     *
     * @return <tt>true</tt> if this connector is an AJP connector; <tt>false</tt> otherwise or if unknown.
     */
    boolean isAjp();

    /**
     * Retrieves the current number of threads in the thread pool utilized by the connector.
     *
     * @return a gauge holding the current number of threads in the thread pool utilized by the connector; <tt>null</tt>
     * if not available.
     */
    Gauge<Integer> getCurrentPoolSizeGauge();

    /**
     * Retrieves the maximum size of the thread pool utilized by the connector.
     *
     * @return a gauge holding the maximum size of the thread pool utilized by the connector; <tt>null</tt> if
     * not available.
     */
    Gauge<Integer> getMaxPoolSizeGauge();

    /**
     * Retrieves the number of busy threads in the thread pool utilized by the connector.
     *
     * @return a gauge holding the number of busy threads in the thread pool utilized by the connector; <tt>null</tt> if
     * not available.
     */
    Gauge<Integer> getBusyThreadsGauge();

    /**
     * Retrieves the number of active connections.
     *
     * @return a gauge holding the number of active connections; <tt>null</tt> if not available.
     */
    Gauge<Long> getActiveConnectionsGauge();

    /**
     * Retrieves the maximum number of connections.
     *
     * @return a gauge holding the maximum number of connections; <tt>null</tt> if not available.
     */
    Gauge<Integer> getMaxConnectionsGauge();

    /**
     * Retrieves the total number of requests processed by this connector.
     *
     * @return a gauge holding the total number of requests processed by this connector; <tt>null</tt> if not available.
     */
    Gauge<Integer> getTotalRequestsGauge();

    /**
     * Retrieves the number of error responses returned by this connector. An error response is defined as a response
     * with a [400-599] HTTP status code.
     *
     * @return a gauge holding the number of error responses returned by this connector; <tt>null</tt> if not available.
     */
    Gauge<Integer> getErrorsGauge();

    /**
     * Retrieves the current request rate per second (QPS).
     *
     * @return a gauge holding the current request rate per second (QPS); <tt>null</tt> if not available.
     */
    Gauge<Integer> getQpsGauge();

    /**
     * Retrieves the number of bytes received by this connector.
     *
     * @return a gauge holding the number of bytes received by this connector; <tt>null</tt> if not available.
     */
    Gauge<Long> getReceivedBytesGauge();

    /**
     * Retrieves the number of bytes sent by this connector.
     *
     * @return a gauge holding the number of bytes sent by this connector; <tt>null</tt> if not available.
     */
    Gauge<Long> getSentBytesGauge();
}

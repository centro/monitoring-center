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

package net.centro.rtb.monitoringcenter.metrics.system.os;

import com.codahale.metrics.Gauge;

/**
 * This interface holds the current readings for network interface usage. All metrics exposed by this class are
 * immutable by the client.
 */
public interface NetworkInterfaceStatus {
    /**
     * Retrieves the name of this network interface.
     *
     * @return the name of this network interface.
     */
    String getName();

    /**
     * Retrieves the current number of received bytes per second.
     *
     * @return a gauge holding the current number of received bytes per second; <tt>null</tt> if not available.
     */
    Gauge<Long> getReceivedBytesPerSecondGauge();

    /**
     * Retrieves the current number of transmitted bytes per second.
     *
     * @return a gauge holding the current number of transmitted bytes per second; <tt>null</tt> if not available.
     */
    Gauge<Long> getTransmittedBytesPerSecondGauge();
}

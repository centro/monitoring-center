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

package net.centro.rtb.monitoringcenter.metrics.system;

import net.centro.rtb.monitoringcenter.metrics.system.jvm.JvmStatus;
import net.centro.rtb.monitoringcenter.metrics.system.os.OperatingSystemStatus;

/**
 * This interface encompasses the current readings for system-level metrics; it is comprised of OS and JVM statuses.
 */
public interface SystemStatus {
    /**
     * Retrieves the status of the operating system.
     *
     * @return the status of the operating system.
     */
    OperatingSystemStatus getOperatingSystemStatus();

    /**
     * Retrieves the status of the Java Virtual Machine (JVM).
     *
     * @return the status of the Java Virtual Machine (JVM).
     */
    JvmStatus getJvmStatus();
}

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

package net.centro.rtb.monitoringcenter.infos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * This class encapsulates information about the operating system and the JVM.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SystemInfo {
    private OperatingSystemInfo operatingSystemInfo;
    private JvmInfo jvmInfo;

    private SystemInfo() {
    }

    public static SystemInfo create() {
        SystemInfo systemInfo = new SystemInfo();

        systemInfo.operatingSystemInfo = OperatingSystemInfo.create();
        systemInfo.jvmInfo = JvmInfo.create();

        return systemInfo;
    }

    /**
     * Retrieves the information about the operating system.
     *
     * @return the information about the operating system.
     */
    public OperatingSystemInfo getOperatingSystemInfo() {
        return operatingSystemInfo;
    }

    /**
     * Retrieves the information about the Java Virtual Machine (JVM).
     *
     * @return the information about the Java Virtual Machine (JVM).
     */
    public JvmInfo getJvmInfo() {
        return jvmInfo;
    }
}

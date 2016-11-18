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

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.centro.rtb.monitoringcenter.metrics.system.jvm.JvmMetricSet;
import net.centro.rtb.monitoringcenter.metrics.system.jvm.JvmStatus;
import net.centro.rtb.monitoringcenter.metrics.system.os.OperatingSystemMetricSet;
import net.centro.rtb.monitoringcenter.metrics.system.os.OperatingSystemStatus;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class SystemMetricSet implements MetricSet, SystemStatus {
    private OperatingSystemMetricSet operatingSystemMetricSet;
    private JvmMetricSet jvmMetricSet;

    private Map<String, Metric> metricsByNames;

    private AtomicBoolean shutdown;

    public SystemMetricSet() {
        Map<String, Metric> metricsByNames = new HashMap<>();

        this.operatingSystemMetricSet = new OperatingSystemMetricSet();
        metricsByNames.put("os", operatingSystemMetricSet);

        this.jvmMetricSet = new JvmMetricSet();
        metricsByNames.put("jvm", jvmMetricSet);

        this.metricsByNames = metricsByNames;

        this.shutdown = new AtomicBoolean(false);
    }

    @Override
    public Map<String, Metric> getMetrics() {
        return Collections.unmodifiableMap(metricsByNames);
    }

    @JsonProperty
    @Override
    public OperatingSystemStatus getOperatingSystemStatus() {
        return operatingSystemMetricSet;
    }

    @JsonProperty
    @Override
    public JvmStatus getJvmStatus() {
        return jvmMetricSet;
    }

    public void shutdown() {
        if (shutdown.getAndSet(true)) {
            return;
        }

        operatingSystemMetricSet.shutdown();
        jvmMetricSet.shutdown();
    }
}

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
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import net.centro.rtb.monitoringcenter.util.JmxUtil;

import javax.management.ObjectName;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE)
class TomcatExecutorMetricSet implements MetricSet, TomcatExecutorStatus {
    private String name;

    private Gauge<Integer> currentPoolSizeGauge;
    private Gauge<Integer> maxPoolSizeGauge;
    private Gauge<Integer> busyThreadsGauge;
    private Gauge<Integer> queueSizeGauge;
    private Gauge<Integer> queueCapacityGauge;

    private Map<String, Metric> metricsByNames;

    TomcatExecutorMetricSet(ObjectName executorObjectName) {
        Preconditions.checkNotNull(executorObjectName);

        this.name = executorObjectName.getKeyProperty("name");

        Map<String, Metric> metricsByNames = new HashMap<>();

        this.currentPoolSizeGauge = JmxUtil.getJmxAttributeAsGauge(executorObjectName, "poolSize", Integer.class, 0);
        if (currentPoolSizeGauge != null) {
            metricsByNames.put("currentPoolSize", currentPoolSizeGauge);
        }

        this.maxPoolSizeGauge = JmxUtil.getJmxAttributeAsGauge(executorObjectName, "maxThreads", Integer.class, 0);
        if (maxPoolSizeGauge != null) {
            metricsByNames.put("maxPoolSize", maxPoolSizeGauge);
        }

        this.busyThreadsGauge = JmxUtil.getJmxAttributeAsGauge(executorObjectName, "activeCount", Integer.class, 0);
        if (busyThreadsGauge != null) {
            metricsByNames.put("busyThreads", busyThreadsGauge);
        }

        this.queueSizeGauge = JmxUtil.getJmxAttributeAsGauge(executorObjectName, "queueSize", Integer.class, 0);
        if (queueSizeGauge != null) {
            metricsByNames.put("queueSize", queueSizeGauge);
        }

        this.queueCapacityGauge = JmxUtil.getJmxAttributeAsGauge(executorObjectName, "maxQueueSize", Integer.class, 0);
        if (queueCapacityGauge != null) {
            metricsByNames.put("queueCapacity", queueCapacityGauge);
        }

        this.metricsByNames = metricsByNames;
    }

    @Override
    public Map<String, Metric> getMetrics() {
        return Collections.unmodifiableMap(metricsByNames);
    }

    @JsonProperty
    @Override
    public String getName() {
        return name;
    }

    @JsonProperty
    @Override
    public Gauge<Integer> getCurrentPoolSizeGauge() {
        return currentPoolSizeGauge;
    }

    @JsonProperty
    @Override
    public Gauge<Integer> getMaxPoolSizeGauge() {
        return maxPoolSizeGauge;
    }

    @JsonProperty
    @Override
    public Gauge<Integer> getBusyThreadsGauge() {
        return busyThreadsGauge;
    }

    @JsonProperty
    @Override
    public Gauge<Integer> getQueueSizeGauge() {
        return queueSizeGauge;
    }

    @JsonProperty
    @Override
    public Gauge<Integer> getQueueCapacityGauge() {
        return queueCapacityGauge;
    }
}

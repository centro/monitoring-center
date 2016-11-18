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
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class JvmMetricSet implements MetricSet, JvmStatus {
    private RuntimeMXBean runtimeMXBean;

    private BufferPoolMetricSet bufferPoolMetricSet;
    private ClassLoadingMetricSet classLoadingMetricSet;
    private ThreadMetricSet threadMetricSet;
    private JvmMemoryMetricSet memoryMetricSet;
    private GarbageCollectorMetricSet gcMetricSet;

    private Gauge<Long> uptimeInMillisGauge;

    private Map<String, Metric> metricsByNames;

    private AtomicBoolean shutdown;

    public JvmMetricSet() {
        this.runtimeMXBean = ManagementFactory.getRuntimeMXBean();

        Map<String, Metric> metricsByNames = new HashMap<>();

        this.bufferPoolMetricSet = new BufferPoolMetricSet();
        metricsByNames.put("bufferPools", bufferPoolMetricSet);

        this.classLoadingMetricSet = new ClassLoadingMetricSet();
        metricsByNames.put("classes", classLoadingMetricSet);

        this.threadMetricSet = new ThreadMetricSet();
        metricsByNames.put("threads", threadMetricSet);

        this.memoryMetricSet = new JvmMemoryMetricSet();
        metricsByNames.put("memory", memoryMetricSet);

        this.gcMetricSet = new GarbageCollectorMetricSet();
        metricsByNames.put("gc", gcMetricSet);

        this.uptimeInMillisGauge = new Gauge<Long>() {
            @Override
            public Long getValue() {
                return runtimeMXBean.getUptime();
            }
        };
        metricsByNames.put("uptimeInMillis", uptimeInMillisGauge);

        this.metricsByNames = metricsByNames;

        this.shutdown = new AtomicBoolean(false);
    }

    @Override
    public Map<String, Metric> getMetrics() {
        return Collections.unmodifiableMap(metricsByNames);
    }

    @JsonProperty
    @Override
    public List<BufferPoolStatus> getBufferPoolStatuses() {
        return bufferPoolMetricSet.getBufferPoolStatuses();
    }

    @JsonProperty
    @Override
    public Gauge<Long> getTotalLoadedClassesGauge() {
        return classLoadingMetricSet.getTotalLoadedClassesGauge();
    }

    @JsonProperty
    @Override
    public Gauge<Long> getUnloadedClassesGauge() {
        return classLoadingMetricSet.getUnloadedClassesGauge();
    }

    @JsonProperty
    @Override
    public Gauge<Integer> getCurrentlyLoadedClassesGauge() {
        return classLoadingMetricSet.getCurrentlyLoadedClassesGauge();
    }

    @JsonProperty
    @Override
    public Gauge<Integer> getCurrentThreadsGauge() {
        return threadMetricSet.getCurrentThreadsGauge();
    }

    @JsonProperty
    @Override
    public Gauge<Integer> getPeakThreadsGauge() {
        return threadMetricSet.getPeakThreadsGauge();
    }

    @JsonProperty
    @Override
    public Gauge<Integer> getDaemonThreadsGauge() {
        return threadMetricSet.getDaemonThreadsGauge();
    }

    @JsonProperty
    @Override
    public Gauge<Integer> getDeadlockedThreadsGauge() {
        return threadMetricSet.getDeadlockedThreadsGauge();
    }

    @JsonProperty
    @Override
    public Map<Thread.State, Gauge<Integer>> getThreadsGaugesByThreadStates() {
        return threadMetricSet.getThreadsGaugesByThreadStates();
    }

    @JsonProperty
    @Override
    public MemoryUsageStatus getTotalMemoryUsageStatus() {
        return memoryMetricSet.getTotalMemoryUsageStatus();
    }

    @JsonProperty
    @Override
    public MemoryUsageStatus getHeapMemoryUsageStatus() {
        return memoryMetricSet.getHeapMemoryUsageStatus();
    }

    @JsonProperty
    @Override
    public MemoryUsageStatus getNonHeapMemoryUsageStatus() {
        return memoryMetricSet.getNonHeapMemoryUsageStatus();
    }

    @JsonProperty
    @Override
    public List<MemoryPoolStatus> getMemoryPoolStatuses() {
        return memoryMetricSet.getMemoryPoolStatuses();
    }

    @JsonProperty
    @Override
    public List<GarbageCollectorStatus> getGarbageCollectorStatuses() {
        return gcMetricSet.getGarbageCollectorStatuses();
    }

    @JsonProperty
    @Override
    public Timer getMinorGcTimer() {
        return gcMetricSet.getMinorGcTimer();
    }

    @JsonProperty
    @Override
    public Timer getMajorGcTimer() {
        return gcMetricSet.getMajorGcTimer();
    }

    @JsonProperty
    @Override
    public Gauge<Long> getFullGcCollectionsGauge() {
        return gcMetricSet.getFullCollectionsGauge();
    }

    @JsonProperty
    @Override
    public Gauge<Long> getUptimeInMillisGauge() {
        return uptimeInMillisGauge;
    }

    public void shutdown() {
        if (shutdown.getAndSet(true)) {
            return;
        }

        gcMetricSet.shutdown();
    }
}

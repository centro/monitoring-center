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
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;

import java.lang.management.MemoryUsage;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE)
class JvmMemoryUsageMetricSet implements MetricSet, MemoryUsageStatus {
    private Gauge<Long> initialSizeInBytesGauge;
    private Gauge<Long> usedMemoryInBytesGauge;
    private Gauge<Long> maxAvailableMemoryInBytesGauge;
    private Gauge<Long> committedMemoryInBytesGauge;
    private Gauge<Double> usedMemoryPercentageGauge;

    private Map<String, Metric> metricsByNames;

    JvmMemoryUsageMetricSet(final MemoryUsageProvider memoryUsageProvider) {
        Preconditions.checkNotNull(memoryUsageProvider);

        final MemoryUsage memoryUsage = memoryUsageProvider.get();
        Preconditions.checkNotNull(memoryUsage);

        Map<String, Metric> metricsByNames = new HashMap<>();

        if (memoryUsage.getInit() >= 0) {
            this.initialSizeInBytesGauge = new Gauge<Long>() {
                @Override
                public Long getValue() {
                    return memoryUsageProvider.get().getInit();
                }
            };
            metricsByNames.put("initialInBytes", initialSizeInBytesGauge);
        }

        this.usedMemoryInBytesGauge = new Gauge<Long>() {
            @Override
            public Long getValue() {
                return memoryUsageProvider.get().getUsed();
            }
        };
        metricsByNames.put("usedInBytes", usedMemoryInBytesGauge);

        if (memoryUsage.getMax() >= 0) {
            this.maxAvailableMemoryInBytesGauge = new Gauge<Long>() {
                @Override
                public Long getValue() {
                    return memoryUsageProvider.get().getMax();
                }
            };
            metricsByNames.put("maxAvailableInBytes", maxAvailableMemoryInBytesGauge);
        }

        this.committedMemoryInBytesGauge = new Gauge<Long>() {
            @Override
            public Long getValue() {
                return memoryUsageProvider.get().getCommitted();
            }
        };
        metricsByNames.put("committedInBytes", committedMemoryInBytesGauge);

        this.usedMemoryPercentageGauge = new Gauge<Double>() {
            @Override
            public Double getValue() {
                MemoryUsage memoryUsage = memoryUsageProvider.get();
                long max = memoryUsage.getMax() > 0 ? memoryUsage.getMax() : memoryUsage.getCommitted();
                return Double.valueOf(memoryUsage.getUsed()) / max * 100;
            }
        };
        metricsByNames.put("usedPercentage", usedMemoryPercentageGauge);

        this.metricsByNames = metricsByNames;
    }

    @Override
    public Map<String, Metric> getMetrics() {
        return Collections.unmodifiableMap(metricsByNames);
    }

    @JsonProperty
    @Override
    public Gauge<Long> getInitialSizeInBytesGauge() {
        return initialSizeInBytesGauge;
    }

    @JsonProperty
    @Override
    public Gauge<Long> getUsedMemoryInBytesGauge() {
        return usedMemoryInBytesGauge;
    }

    @JsonProperty
    @Override
    public Gauge<Long> getMaxAvailableMemoryInBytesGauge() {
        return maxAvailableMemoryInBytesGauge;
    }

    @JsonProperty
    @Override
    public Gauge<Long> getCommittedMemoryInBytesGauge() {
        return committedMemoryInBytesGauge;
    }

    @JsonProperty
    @Override
    public Gauge<Double> getUsedMemoryPercentageGauge() {
        return usedMemoryPercentageGauge;
    }
}

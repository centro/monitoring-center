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
import net.centro.rtb.monitoringcenter.util.MetricNamingUtil;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE)
class BufferPoolMetricSet implements MetricSet {
    private List<BufferPoolMXBean> bufferPoolMXBeans;

    private List<BufferPoolStatus> bufferPoolStatuses;

    private Map<String, Metric> metricsByNames;

    BufferPoolMetricSet() {
        this.bufferPoolMXBeans = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class);

        List<BufferPoolStatus> bufferPoolStatuses = new ArrayList<>();
        Map<String, Metric> metricsByNames = new HashMap<>();

        for (final BufferPoolMXBean bufferPoolMXBean : bufferPoolMXBeans) {
            final String bufferPoolName = bufferPoolMXBean.getName();

            final Gauge<Long> sizeGauge = new Gauge<Long>() {
                @Override
                public Long getValue() {
                    return bufferPoolMXBean.getCount();
                }
            };
            metricsByNames.put(MetricNamingUtil.join(bufferPoolName, "size"), sizeGauge);

            final Gauge<Long> totalCapacityInBytesGauge = new Gauge<Long>() {
                @Override
                public Long getValue() {
                    return bufferPoolMXBean.getTotalCapacity();
                }
            };
            metricsByNames.put(MetricNamingUtil.join(bufferPoolName, "totalCapacityInBytes"), totalCapacityInBytesGauge);

            final Gauge<Long> usedMemoryInBytesGauge;
            if (bufferPoolMXBean.getMemoryUsed() >= 0) {
                usedMemoryInBytesGauge = new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return bufferPoolMXBean.getMemoryUsed();
                    }
                };
                metricsByNames.put(MetricNamingUtil.join(bufferPoolName, "usedMemoryInBytes"), usedMemoryInBytesGauge);
            } else {
                usedMemoryInBytesGauge = null;
            }

            bufferPoolStatuses.add(new BufferPoolStatus() {
                @Override
                public String getName() {
                    return bufferPoolName;
                }

                @Override
                public Gauge<Long> getSizeGauge() {
                    return sizeGauge;
                }

                @Override
                public Gauge<Long> getTotalCapacityInBytesGauge() {
                    return totalCapacityInBytesGauge;
                }

                @Override
                public Gauge<Long> getUsedMemoryInBytesGauge() {
                    return usedMemoryInBytesGauge;
                }
            });
        }

        this.bufferPoolStatuses = bufferPoolStatuses;
        this.metricsByNames = metricsByNames;
    }

    @Override
    public Map<String, Metric> getMetrics() {
        return Collections.unmodifiableMap(metricsByNames);
    }

    @JsonProperty
    List<BufferPoolStatus> getBufferPoolStatuses() {
        return Collections.unmodifiableList(bufferPoolStatuses);
    }
}

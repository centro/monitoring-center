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
import net.centro.rtb.monitoringcenter.util.MetricNamingUtil;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class JvmMemoryMetricSet implements MetricSet {
    private final MemoryMXBean memoryMXBean;
    private final List<MemoryPoolMXBean> memoryPoolMXBeans;

    private MemoryUsageStatus totalMemoryUsageStatus;
    private MemoryUsageStatus heapMemoryUsageStatus;
    private MemoryUsageStatus nonHeapMemoryUsageStatus;

    private List<MemoryPoolStatus> memoryPoolStatuses;

    private Map<String, Metric> metricsByNames;

    JvmMemoryMetricSet() {
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
        this.memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();

        Map<String, Metric> metricsByNames = new HashMap<>();

        // Total
        String totalNamespace = "total";

        final Gauge<Long> totalUsedMemoryInBytesGauge = new Gauge<Long>() {
            @Override
            public Long getValue() {
                return memoryMXBean.getHeapMemoryUsage().getUsed() + memoryMXBean.getNonHeapMemoryUsage().getUsed();
            }
        };
        metricsByNames.put(MetricNamingUtil.join(totalNamespace, "usedInBytes"), totalUsedMemoryInBytesGauge);

        final Gauge<Long> totalCommittedMemoryInBytesGauge = new Gauge<Long>() {
            @Override
            public Long getValue() {
                return memoryMXBean.getHeapMemoryUsage().getCommitted() + memoryMXBean.getNonHeapMemoryUsage().getCommitted();
            }
        };
        metricsByNames.put(MetricNamingUtil.join(totalNamespace, "committedInBytes"), totalCommittedMemoryInBytesGauge);

        this.totalMemoryUsageStatus = new MemoryUsageStatus() {
            @Override
            public Gauge<Long> getInitialSizeInBytesGauge() {
                return null;
            }

            @Override
            public Gauge<Long> getUsedMemoryInBytesGauge() {
                return totalUsedMemoryInBytesGauge;
            }

            @Override
            public Gauge<Long> getMaxAvailableMemoryInBytesGauge() {
                return null;
            }

            @Override
            public Gauge<Long> getCommittedMemoryInBytesGauge() {
                return totalCommittedMemoryInBytesGauge;
            }

            @Override
            public Gauge<Double> getUsedMemoryPercentageGauge() {
                return null;
            }
        };

        // Heap
        JvmMemoryUsageMetricSet heapMemoryUsageMetricSet = new JvmMemoryUsageMetricSet(new MemoryUsageProvider() {
            @Override
            public MemoryUsage get() {
                return memoryMXBean.getHeapMemoryUsage();
            }
        });
        metricsByNames.put("heap", heapMemoryUsageMetricSet);
        this.heapMemoryUsageStatus = heapMemoryUsageMetricSet;

        // Non-heap
        JvmMemoryUsageMetricSet nonHeapMemoryUsageMetricSet = new JvmMemoryUsageMetricSet(new MemoryUsageProvider() {
            @Override
            public MemoryUsage get() {
                return memoryMXBean.getNonHeapMemoryUsage();
            }
        });
        metricsByNames.put("nonHeap", nonHeapMemoryUsageMetricSet);
        this.nonHeapMemoryUsageStatus = nonHeapMemoryUsageMetricSet;

        // Memory pools
        List<MemoryPoolStatus> memoryPoolStatuses = new ArrayList<>();
        for (final MemoryPoolMXBean pool : memoryPoolMXBeans) {
            final String memoryPoolName = pool.getName();
            final String poolNamespace = MetricNamingUtil.join("pools", MetricNamingUtil.sanitize(memoryPoolName));

            final JvmMemoryUsageMetricSet memoryUsageMetricSet = new JvmMemoryUsageMetricSet(new MemoryUsageProvider() {
                @Override
                public MemoryUsage get() {
                    return pool.getUsage();
                }
            });
            metricsByNames.put(poolNamespace, memoryUsageMetricSet);

            final Gauge<Long> usedAfterGcInBytesGauge;
            if (pool.getCollectionUsage() != null) {
                usedAfterGcInBytesGauge = new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return pool.getCollectionUsage().getUsed();
                    }
                };
                metricsByNames.put(MetricNamingUtil.join(poolNamespace, "usedAfterGcInBytes"), usedAfterGcInBytesGauge);
            } else {
                usedAfterGcInBytesGauge = null;
            }

            memoryPoolStatuses.add(new MemoryPoolStatus() {
                @Override
                public String getName() {
                    return memoryPoolName;
                }

                @Override
                public MemoryUsageStatus getMemoryUsageStatus() {
                    return memoryUsageMetricSet;
                }

                @Override
                public Gauge<Long> getUsedAfterGcInBytesGauge() {
                    return usedAfterGcInBytesGauge;
                }
            });
        }
        this.memoryPoolStatuses = memoryPoolStatuses;

        this.metricsByNames = metricsByNames;
    }

    @Override
    public Map<String, Metric> getMetrics() {
        return Collections.unmodifiableMap(metricsByNames);
    }

    public MemoryUsageStatus getTotalMemoryUsageStatus() {
        return totalMemoryUsageStatus;
    }

    public MemoryUsageStatus getHeapMemoryUsageStatus() {
        return heapMemoryUsageStatus;
    }

    public MemoryUsageStatus getNonHeapMemoryUsageStatus() {
        return nonHeapMemoryUsageStatus;
    }

    public List<MemoryPoolStatus> getMemoryPoolStatuses() {
        return Collections.unmodifiableList(memoryPoolStatuses);
    }
}

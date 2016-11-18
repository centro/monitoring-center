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
import com.codahale.metrics.jvm.ThreadDeadlockDetector;
import com.google.common.base.CaseFormat;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class ThreadMetricSet implements MetricSet {
    private final static int STACK_TRACE_DEPTH = 0;

    private ThreadMXBean threadMXBean;
    private ThreadDeadlockDetector deadlockDetector;

    private Gauge<Integer> currentThreadsGauge;
    private Gauge<Integer> peakThreadsGauge;
    private Gauge<Integer> daemonThreadsGauge;
    private Gauge<Integer> deadlockedThreadsGauge;
    private Map<Thread.State, Gauge<Integer>> threadsGaugesByThreadStates;

    private Map<String, Metric> metricsByNames;

    ThreadMetricSet() {
        this.threadMXBean = ManagementFactory.getThreadMXBean();
        this.deadlockDetector = new ThreadDeadlockDetector();

        Map<String, Metric> metricsByNames = new HashMap<>();

        this.currentThreadsGauge = new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return threadMXBean.getThreadCount();
            }
        };
        metricsByNames.put("currentCount", currentThreadsGauge);

        this.peakThreadsGauge = new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return threadMXBean.getPeakThreadCount();
            }
        };
        metricsByNames.put("peakCount", peakThreadsGauge);

        this.daemonThreadsGauge = new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return threadMXBean.getPeakThreadCount();
            }
        };
        metricsByNames.put("daemons", daemonThreadsGauge);

        this.deadlockedThreadsGauge = new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return deadlockDetector.getDeadlockedThreads().size();
            }
        };
        metricsByNames.put("deadlocked", deadlockedThreadsGauge);

        Map<Thread.State, Gauge<Integer>> threadsGaugesByThreadStates = new HashMap<>();
        for (final Thread.State state : Thread.State.values()) {
            String metricName = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, state.toString());

            Gauge<Integer> threadsGauge = new Gauge<Integer>() {
                @Override
                public Integer getValue() {
                    return getThreadCount(state);
                }
            };
            threadsGaugesByThreadStates.put(state, threadsGauge);
            metricsByNames.put(metricName, threadsGauge);
        }
        this.threadsGaugesByThreadStates = threadsGaugesByThreadStates;

        this.metricsByNames = metricsByNames;
    }

    @Override
    public Map<String, Metric> getMetrics() {
        return Collections.unmodifiableMap(metricsByNames);
    }

    Gauge<Integer> getDeadlockedThreadsGauge() {
        return deadlockedThreadsGauge;
    }

    Gauge<Integer> getDaemonThreadsGauge() {
        return daemonThreadsGauge;
    }

    Gauge<Integer> getPeakThreadsGauge() {
        return peakThreadsGauge;
    }

    Gauge<Integer> getCurrentThreadsGauge() {
        return currentThreadsGauge;
    }

    Map<Thread.State, Gauge<Integer>> getThreadsGaugesByThreadStates() {
        return Collections.unmodifiableMap(threadsGaugesByThreadStates);
    }

    private int getThreadCount(Thread.State state) {
        final ThreadInfo[] allThreads = threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds(), STACK_TRACE_DEPTH);
        int count = 0;
        for (ThreadInfo info : allThreads) {
            if (info != null && info.getThreadState() == state) {
                count++;
            }
        }
        return count;
    }
}

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

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class ClassLoadingMetricSet implements MetricSet {
    private ClassLoadingMXBean classLoadingMXBean;

    private Gauge<Long> totalLoadedClassesGauge;
    private Gauge<Long> unloadedClassesGauge;
    private Gauge<Integer> currentlyLoadedClassesGauge;

    private Map<String, Metric> metricsByNames;

    ClassLoadingMetricSet() {
        this.classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();

        Map<String, Metric> metricsByNames = new HashMap<>();

        this.totalLoadedClassesGauge = new Gauge<Long>() {
            @Override
            public Long getValue() {
                return classLoadingMXBean.getTotalLoadedClassCount();
            }
        };
        metricsByNames.put("totalLoaded", totalLoadedClassesGauge);

        this.unloadedClassesGauge = new Gauge<Long>() {
            @Override
            public Long getValue() {
                return classLoadingMXBean.getUnloadedClassCount();
            }
        };
        metricsByNames.put("unloaded", unloadedClassesGauge);

        this.currentlyLoadedClassesGauge = new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return classLoadingMXBean.getLoadedClassCount();
            }
        };
        metricsByNames.put("currentlyLoaded", currentlyLoadedClassesGauge);

        this.metricsByNames = metricsByNames;
    }

    @Override
    public Map<String, Metric> getMetrics() {
        return Collections.unmodifiableMap(metricsByNames);
    }

    Gauge<Long> getTotalLoadedClassesGauge() {
        return totalLoadedClassesGauge;
    }

    Gauge<Long> getUnloadedClassesGauge() {
        return unloadedClassesGauge;
    }

    Gauge<Integer> getCurrentlyLoadedClassesGauge() {
        return currentlyLoadedClassesGauge;
    }
}

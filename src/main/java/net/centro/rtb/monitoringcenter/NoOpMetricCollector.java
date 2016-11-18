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

package net.centro.rtb.monitoringcenter;

import com.codahale.metrics.Counter;
import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.Timer;
import com.google.common.cache.Cache;
import com.mchange.v2.c3p0.PooledDataSource;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * A MetricCollector returned when the MonitoringCenter has not been configured.
 */
class NoOpMetricCollector implements MetricCollector {
    @Override
    public Counter getCounter(String topLevelName, String... additionalNames) {
        return new Counter();
    }

    @Override
    public Timer getTimer(String topLevelName, String... additionalNames) {
        return new Timer();
    }

    @Override
    public Meter getMeter(String topLevelName, String... additionalNames) {
        return new Meter();
    }

    @Override
    public Histogram getHistogram(String topLevelName, String... additionalNames) {
        return new Histogram(new ExponentiallyDecayingReservoir());
    }

    @Override
    public <T> void registerGauge(Gauge<T> gauge, String topLevelName, String... additionalNames) {
    }

    @Override
    public void registerMetric(Metric metric, String topLevelName, String... additionalNames) {
    }

    @Override
    public void registerMetricSet(MetricSet metricSet, String... names) {
    }

    @Override
    public void removeAll() {
    }

    @Override
    public void removeMetric(Metric metric, String topLevelName, String... additionalNames) {
    }

    @Override
    public void removeMetricSet(MetricSet metricSet, String... names) {
    }

    @Override
    public void replaceMetric(Metric metric, String topLevelName, String... additionalNames) {
    }

    @Override
    public void registerCollection(Collection<?> collection, String topLevelName, String... additionalNames) {
    }

    @Override
    public void registerMap(Map<?, ?> map, String topLevelName, String... additionalNames) {
    }

    @Override
    public void registerGuavaCache(Cache<?, ?> cache, String topLevelName, String... additionalNames) {
    }

    @Override
    public void registerC3P0DataSource(PooledDataSource pooledDataSource, String topLevelName, String... additionalNames) {
    }

    @Override
    public <T> BlockingQueue<T> instrumentBlockingQueue(BlockingQueue<T> blockingQueue, String topLevelName, String... additionalNames) {
        return blockingQueue;
    }

    @Override
    public ExecutorService instrumentExecutorService(ExecutorService executorService, String topLevelName, String... additionalNames) {
        return executorService;
    }

    @Override
    public ScheduledExecutorService instrumentScheduledExecutorService(ScheduledExecutorService scheduledExecutorService, String topLevelName, String... additionalNames) {
        return scheduledExecutorService;
    }

    @Override
    public DataSource instrumentDataSource(DataSource dataSource, String topLevelName, String... additionalNames) {
        return dataSource;
    }
}

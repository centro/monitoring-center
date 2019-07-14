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
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.Timer;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.mchange.v2.c3p0.PooledDataSource;
import net.centro.rtb.monitoringcenter.config.MetricNamePostfixPolicy;
import net.centro.rtb.monitoringcenter.config.NamingConfig;
import net.centro.rtb.monitoringcenter.metrics.C3P0PooledDataSourceMetricSet;
import net.centro.rtb.monitoringcenter.metrics.GuavaCacheMetricSet;
import net.centro.rtb.monitoringcenter.metrics.instrumented.InstrumentedDataSource;
import net.centro.rtb.monitoringcenter.metrics.instrumented.InstrumentedExecutorService;
import net.centro.rtb.monitoringcenter.metrics.instrumented.InstrumentedScheduledExecutorService;
import net.centro.rtb.monitoringcenter.util.MetricNamingUtil;
import org.apache.commons.lang3.StringUtils;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

class MetricCollectorImpl implements MetricCollector {
    private static final String COUNTER_POSTFIX = "Counter";
    private static final String TIMER_POSTFIX = "Timer";
    private static final String METER_POSTFIX = "Meter";
    private static final String HISTOGRAM_POSTFIX = "Histogram";
    private static final String GAUGE_POSTFIX = "Gauge";

    private static final Set<String> COMPOSITE_TYPE_POSTFIXES = Collections.unmodifiableSet(new HashSet<String>() {{
        add(TIMER_POSTFIX);
        add(METER_POSTFIX);
        add(HISTOGRAM_POSTFIX);
    }});

    private MetricRegistry metricRegistry;
    private NamingConfig namingConfig;
    private String collectorNamespace;

    MetricCollectorImpl(MetricRegistry metricRegistry, NamingConfig namingConfig, String collectorNamespace) {
        Preconditions.checkNotNull(metricRegistry);
        Preconditions.checkNotNull(namingConfig);
        Preconditions.checkNotNull(collectorNamespace);

        this.metricRegistry = metricRegistry;
        this.namingConfig = namingConfig;
        this.collectorNamespace = collectorNamespace;
    }

    @Override
    public Counter getCounter(String topLevelName, String... additionalNames) {
        return metricRegistry.counter(buildFullName(topLevelName, additionalNames, COUNTER_POSTFIX));
    }

    @Override
    public Counter getCounter(Supplier<Counter> counterSupplier, String topLevelName, String... additionalNames) {
        Preconditions.checkNotNull(counterSupplier, "counterSupplier cannot be null");
        return metricRegistry.counter(buildFullName(topLevelName, additionalNames, COUNTER_POSTFIX), counterSupplier::get);
    }

    @Override
    public Timer getTimer(String topLevelName, String... additionalNames) {
        return metricRegistry.timer(buildFullName(topLevelName, additionalNames, TIMER_POSTFIX));
    }

    @Override
    public Timer getTimer(Supplier<Timer> timerSupplier, String topLevelName, String... additionalNames) {
        Preconditions.checkNotNull(timerSupplier, "timerSupplier cannot be null");
        return metricRegistry.timer(buildFullName(topLevelName, additionalNames, TIMER_POSTFIX), timerSupplier::get);
    }

    @Override
    public Meter getMeter(String topLevelName, String... additionalNames) {
        return metricRegistry.meter(buildFullName(topLevelName, additionalNames, METER_POSTFIX));
    }

    @Override
    public Meter getMeter(Supplier<Meter> meterSupplier, String topLevelName, String... additionalNames) {
        Preconditions.checkNotNull(meterSupplier, "meterSupplier cannot be null");
        return metricRegistry.meter(buildFullName(topLevelName, additionalNames, METER_POSTFIX), meterSupplier::get);
    }

    @Override
    public Histogram getHistogram(String topLevelName, String... additionalNames) {
        return metricRegistry.histogram(buildFullName(topLevelName, additionalNames, HISTOGRAM_POSTFIX));
    }

    @Override
    public Histogram getHistogram(Supplier<Histogram> histogramSupplier, String topLevelName, String... additionalNames) {
        Preconditions.checkNotNull(histogramSupplier, "histogramSupplier cannot be null");
        return metricRegistry.histogram(buildFullName(topLevelName, additionalNames, HISTOGRAM_POSTFIX), histogramSupplier::get);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Gauge<T> getGauge(Supplier<Gauge<T>> gaugeSupplier, String topLevelName, String... additionalNames) {
        Preconditions.checkNotNull(gaugeSupplier, "gaugeSupplier cannot be null");
        return metricRegistry.gauge(buildFullName(topLevelName, additionalNames, GAUGE_POSTFIX), gaugeSupplier::get);
    }

    @Override
    public <T> void registerGauge(Gauge<T> gauge, String topLevelName, String... additionalNames) {
        Preconditions.checkNotNull(gauge, "gauge cannot be null");
        metricRegistry.register(buildFullName(topLevelName, additionalNames, GAUGE_POSTFIX), gauge);
    }

    @Override
    public void registerMetric(Metric metric, String topLevelName, String... additionalNames) {
        Preconditions.checkNotNull(metric, "metric cannot be null");

        if (MetricSet.class.isAssignableFrom(metric.getClass())) {
            if (StringUtils.isNotBlank(topLevelName)) {
                registerMetricSet(MetricSet.class.cast(metric), MetricNamingUtil.join(topLevelName, additionalNames));
            } else {
                registerMetricSet(MetricSet.class.cast(metric));
            }
        } else {
            metricRegistry.register(buildFullName(topLevelName, additionalNames, getPostfixForMetric(metric)), metric);
        }
    }

    @Override
    public void registerMetricSet(MetricSet metricSet, String... names) {
        Preconditions.checkNotNull(metricSet, "metricSet cannot be null");

        String namespace = null;
        if (names != null && names.length > 0) {
            namespace = buildFullName(names[0], Arrays.copyOfRange(names, 1, names.length), null);
        } else {
            namespace = collectorNamespace;
        }

        registerMetricSetImpl(metricSet, namespace);
    }

    @Override
    public void removeAll() {
        metricRegistry.removeMatching(new MetricFilter() {
            @Override
            public boolean matches(String name, Metric metric) {
                return name.startsWith(collectorNamespace + MetricNamingUtil.SEPARATOR);
            }
        });
    }

    @Override
    public void removeMetric(Metric metric, String topLevelName, String... additionalNames) {
        Preconditions.checkNotNull(metric, "metric cannot be null");

        if (MetricSet.class.isAssignableFrom(metric.getClass())) {
            if (StringUtils.isNotBlank(topLevelName)) {
                removeMetricSet(MetricSet.class.cast(metric), MetricNamingUtil.join(topLevelName, additionalNames));
            } else {
                removeMetricSet(MetricSet.class.cast(metric));
            }
        } else {
            metricRegistry.remove(buildFullName(topLevelName, additionalNames, getPostfixForMetric(metric)));
        }
    }

    @Override
    public void removeMetricSet(MetricSet metricSet, String... names) {
        Preconditions.checkNotNull(metricSet, "metricSet cannot be null");

        String namespace = null;
        if (names != null && names.length > 0) {
            namespace = buildFullName(names[0], Arrays.copyOfRange(names, 1, names.length), null);
        } else {
            namespace = collectorNamespace;
        }

        removeMetricSetImpl(metricSet, namespace);
    }

    @Override
    public void replaceMetric(Metric metric, String topLevelName, String... additionalNames) {
        removeMetric(metric, topLevelName, additionalNames);
        registerMetric(metric, topLevelName, additionalNames);
    }

    @Override
    public void registerCollection(final Collection<?> collection, String topLevelName, String... additionalNames) {
        validateInstrumentationInputParams(collection, "collection", topLevelName);

        String queueNamespace = MetricNamingUtil.join(topLevelName, additionalNames);

        registerGauge(new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return collection.size();
            }
        }, MetricNamingUtil.join(queueNamespace, "size"));
    }

    @Override
    public void registerMap(final Map<?, ?> map, String topLevelName, String... additionalNames) {
        validateInstrumentationInputParams(map, "map", topLevelName);

        String queueNamespace = MetricNamingUtil.join(topLevelName, additionalNames);

        registerGauge(new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return map.size();
            }
        }, MetricNamingUtil.join(queueNamespace, "size"));
    }

    @Override
    public void registerGuavaCache(final Cache<?, ?> cache, String topLevelName, String... additionalNames) {
        validateInstrumentationInputParams(cache, "cache", topLevelName);

        registerMetricSet(new GuavaCacheMetricSet(cache), MetricNamingUtil.join(topLevelName, additionalNames));
    }

    @Override
    public void registerC3P0DataSource(PooledDataSource pooledDataSource, String topLevelName, String... additionalNames) {
        validateInstrumentationInputParams(pooledDataSource, "pooledDataSource", topLevelName);

        String dataSourceNamespace = MetricNamingUtil.join(topLevelName, additionalNames);

        registerMetricSet(new C3P0PooledDataSourceMetricSet(pooledDataSource), dataSourceNamespace);
    }

    @Override
    public <T> BlockingQueue<T> instrumentBlockingQueue(final BlockingQueue<T> blockingQueue, String topLevelName, String... additionalNames) {
        validateInstrumentationInputParams(blockingQueue, "blockingQueue", topLevelName);

        String queueNamespace = MetricNamingUtil.join(topLevelName, additionalNames);

        registerGauge(new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return blockingQueue.size();
            }
        }, MetricNamingUtil.join(queueNamespace, "size"));

        registerGauge(new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return blockingQueue.remainingCapacity();
            }
        }, MetricNamingUtil.join(queueNamespace, "remainingCapacity"));

        return blockingQueue;
    }

    @Override
    public ExecutorService instrumentExecutorService(ExecutorService executorService, String topLevelName, String... additionalNames) {
        validateInstrumentationInputParams(executorService, "executorService", topLevelName);

        String executorServiceNamespace = MetricNamingUtil.join(topLevelName, additionalNames);

        return new InstrumentedExecutorService(executorService, this, executorServiceNamespace);
    }

    @Override
    public ScheduledExecutorService instrumentScheduledExecutorService(ScheduledExecutorService scheduledExecutorService, String topLevelName, String... additionalNames) {
        validateInstrumentationInputParams(scheduledExecutorService, "scheduledExecutorService", topLevelName);

        String executorServiceNamespace = MetricNamingUtil.join(topLevelName, additionalNames);

        return new InstrumentedScheduledExecutorService(scheduledExecutorService, this, executorServiceNamespace);
    }

    @Override
    public DataSource instrumentDataSource(DataSource dataSource, String topLevelName, String... additionalNames) {
        validateInstrumentationInputParams(dataSource, "dataSource", topLevelName);

        String dataSourceNamespace = MetricNamingUtil.join(topLevelName, additionalNames);

        return new InstrumentedDataSource(dataSource, this, dataSourceNamespace);
    }

    private void registerMetricSetImpl(MetricSet metricSet, String namespace) {
        for (Map.Entry<String, Metric> entry : metricSet.getMetrics().entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }

            if (MetricSet.class.isInstance(entry.getValue())) {
                MetricSet innerMetricSet = MetricSet.class.cast(entry.getValue());
                if (namespace == null) {
                    registerMetricSet(innerMetricSet, entry.getKey());
                } else {
                    registerMetricSetImpl(innerMetricSet, MetricNamingUtil.join(namespace, entry.getKey()));
                }
            } else {
                String name = null;
                if (namespace == null) {
                    name = entry.getKey();
                } else {
                    name = MetricNamingUtil.join(namespace, entry.getKey());
                }
                name = addPostfixIfNeeded(name, getPostfixForMetric(entry.getValue()));
                metricRegistry.register(name, entry.getValue());
            }
        }
    }

    private void removeMetricSetImpl(MetricSet metricSet, String namespace) {
        for (Map.Entry<String, Metric> entry : metricSet.getMetrics().entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }

            if (MetricSet.class.isInstance(entry.getValue())) {
                MetricSet innerMetricSet = MetricSet.class.cast(entry.getValue());
                if (namespace == null) {
                    removeMetricSet(innerMetricSet, entry.getKey());
                } else {
                    removeMetricSetImpl(innerMetricSet, MetricNamingUtil.join(namespace, entry.getKey()));
                }
            } else {
                String name = null;
                if (namespace == null) {
                    name = entry.getKey();
                } else {
                    name = MetricNamingUtil.join(namespace, entry.getKey());
                }
                name = addPostfixIfNeeded(name, getPostfixForMetric(entry.getValue()));
                metricRegistry.remove(name);
            }
        }
    }

    private void validateInstrumentationInputParams(Object target, String targetName, String name) {
        Preconditions.checkNotNull(target, targetName + " cannot be null");
        Preconditions.checkArgument(StringUtils.isNotBlank(name), "name cannot be blank");
    }

    private String buildFullName(String name, String[] additionalNames, String postfix) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name), "name cannot be blank");

        String metricName = MetricNamingUtil.join(collectorNamespace, name);
        if (additionalNames != null && additionalNames.length > 0) {
            metricName = MetricNamingUtil.join(metricName, additionalNames);
        }

        metricName = addPostfixIfNeeded(metricName, postfix);

        return metricName;
    }

    private String addPostfixIfNeeded(String metricName, String postfix) {
        if (postfix != null) {
            if (!metricName.endsWith(postfix)) {
                MetricNamePostfixPolicy postfixPolicy = namingConfig.getMetricNamePostfixPolicy();
                if (postfixPolicy == MetricNamePostfixPolicy.ADD_ALL_TYPES || (postfixPolicy == MetricNamePostfixPolicy.ADD_COMPOSITE_TYPES && COMPOSITE_TYPE_POSTFIXES.contains(postfix))) {
                    metricName += postfix;
                }
            }
        }
        return metricName;
    }

    private String getPostfixForMetric(Metric metric) {
        Preconditions.checkNotNull(metric);

        Class<? extends Metric> metricClass = metric.getClass();
        if (Counter.class.isAssignableFrom(metricClass)) {
            return COUNTER_POSTFIX;
        } else if (Gauge.class.isAssignableFrom(metricClass)) {
            return GAUGE_POSTFIX;
        } else if (Timer.class.isAssignableFrom(metricClass)) {
            return TIMER_POSTFIX;
        } else if (Meter.class.isAssignableFrom(metricClass)) {
            return METER_POSTFIX;
        } else if (Histogram.class.isAssignableFrom(metricClass)) {
            return HISTOGRAM_POSTFIX;
        } else {
            return null;
        }
    }
}

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
 * This interface is responsible for providing clients of the {@link MonitoringCenter} with methods for collecting
 * metrics under a particular namespace. MetricCollector is analogous to a logger retrieved from SLF4J's LoggerFactory.
 * As such, a typical idiom for instantiating a MetricCollector is:
 * <pre>{@code
 * private static final MetricCollector metricCollector = MonitoringCenter.getMetricCollector(SampleService.class);
 * }</pre>
 * <p>
 *     In addition to the ability to instantiate or retrieve Dropwizard's {@link Metric} implementations (e.g.,
 *     Counter), this interface exposes high-level methods for registering and instrumenting collections, maps, caches,
 *     executors, and data sources. The instrumentation methods return a proxy instance that has to be employed in lieu
 *     of the original instance.
 * </p>
 *
 * <p>
 *     All metrics registered within a MetricCollector are prefixed with its namespace (defined when retrieving a
 *     MetricCollector instance from MonitoringCenter; see {@link MonitoringCenter#getMetricCollector(String, String...)}).
 *     A MetricCollector also enforces a metric naming policy specified in the MonitoringCenter's configuration
 *     (see {@link net.centro.rtb.monitoringcenter.config.MetricNamePostfixPolicy}). Thus, a histogram named "foo" may be
 *     registered as "fooHistogram", given an appropriate MetricNamePostfixPolicy (e.g.,
 *     {@link net.centro.rtb.monitoringcenter.config.MetricNamePostfixPolicy#ADD_COMPOSITE_TYPES}).
 * </p>
 * <p>In other words, all metrics registered with a MetricCollector will follow the naming schema below:
 *     <br><br>
 *     <tt>
 *         [MetricCollector's namespace].[name].[additionalNames][postfix as dictated by the MetricNamePostfixPolicy]
 *     </tt>,<br><br> where "." is the separator character expressed by
 *     {@link net.centro.rtb.monitoringcenter.util.MetricNamingUtil#SEPARATOR}.
 * </p>
 * <p>
 *     For instance, <tt>getMeter("requests", "errors", "400")</tt> registered with a MetricCollector named
 *     "SampleServlet" and given
 *     {@link net.centro.rtb.monitoringcenter.config.MetricNamePostfixPolicy#ADD_COMPOSITE_TYPES}, will yield a meter
 *     named "SampleServlet.requests.errors.400Meter".
 * </p>
 * <p>
 *     Note that the node-specific prefix (such as "bidder.east.lb10-p2e.114") is appended on demand during the
 *     reporting stage.
 * </p>
 * <p>
 *     MetricCollector abstracts away all the naming intricacies mentioned above, allowing the client to simply
 *     reference a metric by its short name ("coins" vs "SampleService.coinsMeter").
 * </p>
 */
public interface MetricCollector {
    /**
     * Retrieves a counter, creating it if one does not exist. This method is thread-safe. The actual name of the
     * counter registered with the MonitoringCenter will be prefixed with MetricCollector's namespace and, possibly,
     * postfixed with the type of the metric (i.e., Counter). The postfix enforcement is controlled by the
     * {@link net.centro.rtb.monitoringcenter.config.MetricNamePostfixPolicy} configured in the MonitoringCenter.
     * <br>
     * All illegal characters contained in the provided name parts will be escaped as documented for
     * {@link net.centro.rtb.monitoringcenter.util.MetricNamingUtil#join(String, String...)}.
     *
     * @param topLevelName top-level part of the counter's name.
     * @param additionalNames additional parts of the counter's name.
     * @return a new or existing counter.
     * @throws IllegalArgumentException if top-level part of the name is blank.
     */
    Counter getCounter(String topLevelName, String... additionalNames);

    /**
     * Retrieves a timer, creating it if one does not exist. This method is thread-safe. The actual name of the
     * timer registered with the MonitoringCenter will be prefixed with MetricCollector's namespace and, possibly,
     * postfixed with the type of the metric (i.e., Timer). The postfix enforcement is controlled by the
     * {@link net.centro.rtb.monitoringcenter.config.MetricNamePostfixPolicy} configured in the MonitoringCenter.
     * <br>
     * All illegal characters contained in the provided name parts will be escaped as documented for
     * {@link net.centro.rtb.monitoringcenter.util.MetricNamingUtil#join(String, String...)}.
     *
     * @param topLevelName top-level part of the timer's name.
     * @param additionalNames additional parts of the timer's name.
     * @return a new or existing timer.
     * @throws IllegalArgumentException if top-level part of the name is blank.
     */
    Timer getTimer(String topLevelName, String... additionalNames);

    /**
     * Retrieves a meter, creating it if one does not exist. This method is thread-safe. The actual name of the
     * meter registered with the MonitoringCenter will be prefixed with MetricCollector's namespace and, possibly,
     * postfixed with the type of the metric (i.e., Meter). The postfix enforcement is controlled by the
     * {@link net.centro.rtb.monitoringcenter.config.MetricNamePostfixPolicy} configured in the MonitoringCenter.
     * <br>
     * All illegal characters contained in the provided name parts will be escaped as documented for
     * {@link net.centro.rtb.monitoringcenter.util.MetricNamingUtil#join(String, String...)}.
     *
     * @param topLevelName top-level part of the meter's name.
     * @param additionalNames additional parts of the meter's name.
     * @return a new or existing meter.
     * @throws IllegalArgumentException if top-level part of the name is blank.
     */
    Meter getMeter(String topLevelName, String... additionalNames);

    /**
     * Retrieves a histogram, creating it if one does not exist. This method is thread-safe. The actual name of the
     * histogram registered with the MonitoringCenter will be prefixed with MetricCollector's namespace and, possibly,
     * postfixed with the type of the metric (i.e., Histogram). The postfix enforcement is controlled by the
     * {@link net.centro.rtb.monitoringcenter.config.MetricNamePostfixPolicy} configured in the MonitoringCenter.
     * <br>
     * All illegal characters contained in the provided name parts will be escaped as documented for
     * {@link net.centro.rtb.monitoringcenter.util.MetricNamingUtil#join(String, String...)}.
     *
     * @param topLevelName top-level part of the histogram's name.
     * @param additionalNames additional parts of the histogram's name.
     * @return a new or existing histogram.
     * @throws IllegalArgumentException if top-level part of the name is blank.
     */
    Histogram getHistogram(String topLevelName, String... additionalNames);

    /**
     * Registers a gauge. This method is thread-safe. An attempt to register a gauge, which has already been registered
     * will result in an IllegalArgumentException. The actual name of the gauge registered with the MonitoringCenter
     * will be prefixed with MetricCollector's namespace and, possibly, postfixed with the type of the metric
     * (i.e., Gauge). The postfix enforcement is controlled by the
     * {@link net.centro.rtb.monitoringcenter.config.MetricNamePostfixPolicy} configured in the MonitoringCenter.
     * <br>
     * All illegal characters contained in the provided name parts will be escaped as documented for
     * {@link net.centro.rtb.monitoringcenter.util.MetricNamingUtil#join(String, String...)}.
     *
     * @param gauge a gauge to register.
     * @param topLevelName top-level part of the gauge's name.
     * @param additionalNames additional parts of the gauge's name.
     * @param <T> type of the value returned by the gauge.
     * @throws NullPointerException if <tt>gauge</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException if top-level part of the name is blank.
     * @throws IllegalArgumentException if a gauge with the same name has already been registered.
     */
    <T> void registerGauge(Gauge<T> gauge, String topLevelName, String... additionalNames);

    /**
     * Registers a metric. This method is thread-safe. An attempt to register a metric, which has already been
     * registered will result in an IllegalArgumentException. The actual name of the metric registered with the
     * MonitoringCenter will be prefixed with MetricCollector's namespace and, possibly, postfixed with the type of the
     * metric (e.g., Counter). The postfix enforcement is controlled by the
     * {@link net.centro.rtb.monitoringcenter.config.MetricNamePostfixPolicy} configured in the MonitoringCenter.
     * <br>
     * All illegal characters contained in the provided name parts will be escaped as documented for
     * {@link net.centro.rtb.monitoringcenter.util.MetricNamingUtil#join(String, String...)}.
     *
     * @param metric a metric to register.
     * @param topLevelName top-level part of the metric's name.
     * @param additionalNames additional parts of the metric's name.
     * @throws NullPointerException if <tt>metric</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException if top-level part of the name is blank.
     * @throws IllegalArgumentException if a metric with the same name has already been registered.
     */
    void registerMetric(Metric metric, String topLevelName, String... additionalNames);

    /**
     * Registers all metrics contained in a MetricSet. If no name is provided, individual metrics will be registered
     * under the MetricCollector's namespace (as if registered by {@link #registerMetric(Metric, String, String...)}).
     * If a name is specified for the MetricSet, it will be employed as a namespace for all individual metrics
     * registered as a result of this method. Thus, the actual name of the individual metrics registered with the
     * MonitoringCenter will be prefixed with MetricCollector's namespace, MetricSet's namespace, and then, possibly,
     * postfixed with the type of the metric (e.g., Counter). The postfix enforcement is controlled by the
     * {@link net.centro.rtb.monitoringcenter.config.MetricNamePostfixPolicy} configured in the MonitoringCenter.
     * <br>
     * All illegal characters contained in the provided name parts will be escaped as documented for
     * {@link net.centro.rtb.monitoringcenter.util.MetricNamingUtil#join(String, String...)}.
     *
     * @param metricSet a metric set to register.
     * @param names optional name parts that form the namespace for metrics in the metric set.
     * @throws NullPointerException if <tt>metricSet</tt> is <tt>null</tt>.
     */
    void registerMetricSet(MetricSet metricSet, String... names);

    /**
     * Removes all metrics registered within this MetricCollector. This method is especially useful for unit tests.
     */
    void removeAll();

    /**
     * Removes a metric from the MetricCollector, voiding its registration with the MonitoringCenter. This method is
     * thread-safe. The name of the metric to remove will be computed in accordance with the logic documented for
     * {@link #registerMetric(Metric, String, String...)}.
     *
     * @param metric a metric to remove.
     * @param topLevelName top-level part of the metric's name.
     * @param additionalNames additional parts of the metric's name.
     * @throws NullPointerException if <tt>metric</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException if top-level part of the name is blank.
     */
    void removeMetric(Metric metric, String topLevelName, String... additionalNames);

    /**
     * Removes a metric set, effectively removing all metrics contained in it. This method acts as the counterpart of
     * {@link #registerMetricSet(MetricSet, String...)}.
     *
     * @param metricSet a metric set to remove.
     * @param names optional name parts that form the namespace for metrics in the metric set.
     * @throws NullPointerException if <tt>metricSet</tt> is <tt>null</tt>.
     */
    void removeMetricSet(MetricSet metricSet, String... names);

    /**
     * Replaces an existing metric with a new metric under the same name. Effectively, this is a convenience method for
     * combining removing and registering a metric.
     *
     * @see #registerMetric(Metric, String, String...)
     * @see #removeMetric(Metric, String, String...)
     * @param metric a metric to replace the existing metric with.
     * @param topLevelName top-level part of the metric's name.
     * @param additionalNames additional parts of the metric's name.
     * @throws NullPointerException if <tt>metric</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException if top-level part of the name is blank.
     */
    void replaceMetric(Metric metric, String topLevelName, String... additionalNames);

    /**
     * Registers a {@link Collection} with the MetricCollector, exposing its size as a gauge.
     *
     * @param collection a collection to register.
     * @param topLevelName top-level part of the collection's name.
     * @param additionalNames additional parts of the collection's name.
     * @throws NullPointerException if <tt>collection</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException if top-level part of the name is blank.
     */
    void registerCollection(Collection<?> collection, String topLevelName, String... additionalNames);

    /**
     * Registers a {@link Map} with the MetricCollector, exposing its size as a gauge.
     *
     * @param map a map to register.
     * @param topLevelName top-level part of the map's name.
     * @param additionalNames additional parts of the map's name.
     * @throws NullPointerException if <tt>map</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException if top-level part of the name is blank.
     */
    void registerMap(Map<?, ?> map, String topLevelName, String... additionalNames);

    /**
     * Registers a Guava Cache with the MetricCollector, exposing its size and stats as gauges.
     *
     * @see Cache#stats()
     * @param cache a cache to register.
     * @param topLevelName top-level part of the cache's name.
     * @param additionalNames additional parts of the cache's name.
     * @throws NullPointerException if <tt>cache</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException if top-level part of the name is blank.
     */
    void registerGuavaCache(Cache<?, ?> cache, String topLevelName, String... additionalNames);

    /**
     * Registers a C3P0 pool with the MetricCollector, exposing as gauges its various data points for connections,
     * threads, and statement cache.
     *
     * @param pooledDataSource a C3P0 data source to register.
     * @param topLevelName top-level part of the data source's name.
     * @param additionalNames additional parts of the data source's name.
     * @throws NullPointerException if <tt>pooledDataSource</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException if top-level part of the name is blank.
     */
    void registerC3P0DataSource(PooledDataSource pooledDataSource, String topLevelName, String... additionalNames);

    /**
     * Instruments a {@link BlockingQueue}. This method registers the blocking queue with the MetricCollector, exposing
     * its size and capacity as gauges.
     *
     * @param blockingQueue a blocking queue to instrument.
     * @param topLevelName top-level part of the blocking queue's name.
     * @param additionalNames additional parts of the blocking queue's name.
     * @param <T> type of the elements of the blocking queue.
     * @return a possibly proxied instance of the passed in blocking queue.
     * @throws NullPointerException if <tt>blockingQueue</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException if top-level part of the name is blank.
     */
    <T extends Object> BlockingQueue<T> instrumentBlockingQueue(BlockingQueue<T> blockingQueue, String topLevelName, String... additionalNames);

    /**
     * Instruments an {@link ExecutorService}. This method creates a proxy of a given executor service, exposing its
     * various statistics under this MetricCollector.
     *
     * @param executorService an executor service to instrument.
     * @param topLevelName top-level part of the executor service's name.
     * @param additionalNames additional parts of the executor service's name.
     * @return a proxied instance of the passed in executor service.
     * @throws NullPointerException if <tt>executorService</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException if top-level part of the name is blank.
     */
    ExecutorService instrumentExecutorService(ExecutorService executorService, String topLevelName, String... additionalNames);

    /**
     * Instruments a {@link ScheduledExecutorService}. This method creates a proxy of a given scheduled executor
     * service, exposing its various statistics under this MetricCollector.
     *
     * @param scheduledExecutorService a scheduled executor service to instrument.
     * @param topLevelName top-level part of the scheduled executor service's name.
     * @param additionalNames additional parts of the scheduled executor service's name.
     * @return a proxied instance of the passed in scheduled executor service.
     * @throws NullPointerException if <tt>scheduledExecutorService</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException if top-level part of the name is blank.
     */
    ScheduledExecutorService instrumentScheduledExecutorService(ScheduledExecutorService scheduledExecutorService, String topLevelName, String... additionalNames);

    /**
     * Instruments a {@link DataSource}, exposing the distribution of connection acquisition times.
     *
     * @param dataSource a data source to instrument.
     * @param topLevelName top-level part of the data source's name.
     * @param additionalNames additional parts of the data source's name.
     * @return a proxied instance of the passed in data source.
     * @throws NullPointerException if <tt>dataSource</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException if top-level part of the name is blank.
     */
    DataSource instrumentDataSource(DataSource dataSource, String topLevelName, String... additionalNames);
}

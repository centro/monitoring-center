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

import com.codahale.metrics.Clock;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.codahale.metrics.graphite.GraphiteSender;
import com.codahale.metrics.graphite.PickledGraphite;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.centro.rtb.monitoringcenter.config.Configurator;
import net.centro.rtb.monitoringcenter.config.GraphiteReporterConfig;
import net.centro.rtb.monitoringcenter.config.HostAndPort;
import net.centro.rtb.monitoringcenter.config.MetricCollectionConfig;
import net.centro.rtb.monitoringcenter.config.MonitoringCenterConfig;
import net.centro.rtb.monitoringcenter.config.NamingConfig;
import net.centro.rtb.monitoringcenter.infos.AppInfo;
import net.centro.rtb.monitoringcenter.infos.NodeInfo;
import net.centro.rtb.monitoringcenter.infos.SystemInfo;
import net.centro.rtb.monitoringcenter.metrics.system.SystemMetricSet;
import net.centro.rtb.monitoringcenter.metrics.system.SystemStatus;
import net.centro.rtb.monitoringcenter.metrics.tomcat.TomcatMetricSet;
import net.centro.rtb.monitoringcenter.metrics.tomcat.TomcatStatus;
import net.centro.rtb.monitoringcenter.util.ConfigFileUtil;
import net.centro.rtb.monitoringcenter.util.MetricNamingUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This singleton class is the heart of the MonitoringCenter library. Its responsibilities include
 * <ul>
 * <li>maintaining overall state of the library</li>
 * <li>keeping and reloading configuration parameters</li>
 * <li>creating and providing MetricCollector instances</li>
 * <li>exposing metrics for pull and push reporting</li>
 * <li>registration and execution of health checks</li>
 * <li>providing static information about the system (OS and JVM), network node, and application</li>
 * <li>and, finally, exposing system (OS and JVM) and Tomcat statuses.</li>
 * </ul>
 *
 * <p>MonitoringCenter is a wrapper around Dropwizard's {@link MetricRegistry} and {@link HealthCheckRegistry}.</p>
 *
 * <p>
 *     This class must be configured before any application code using it is executed. Thus, it is recommended to run
 *     {@link #configure(MonitoringCenterConfig)} as the first statement in the ContextListener or the main() method
 *     (perhaps, right after configuring your logging framework). Please note that MonitoringCenter will configure
 *     itself automatically upon calls to {@link #getMetricCollector(String, String...)} or
 *     {@link #registerHealthCheck(String, HealthCheck)}. When configured automatically, the MonitoringCenter will
 *     use the default config file as explained in {@link Configurator#defaultConfigFile()}.
 * </p>
 *
 * <p>
 *     This class is thread-safe.
 * </p>
 */
public class MonitoringCenter {
    private static final Logger logger = LoggerFactory.getLogger(MonitoringCenter.class);

    private static final int CONFIG_RELOAD_INTERVAL_IN_SECONDS = 60;
    private static final String HEALTH_CHECK_POSTFIX = "HealthCheck";

    private static final String SYSTEM_METRIC_NAMESPACE = "system";
    private static final String TOMCAT_METRIC_NAMESPACE = "tomcat";
    private static final String DB_METRIC_NAMESPACE = "dbs";

    private static final Set<String> RESERVED_NAMESPACES = Collections.unmodifiableSet(new HashSet<String>() {{
        add(SYSTEM_METRIC_NAMESPACE);
        add(TOMCAT_METRIC_NAMESPACE);
    }});

    private static final SortedMap<String, ? extends Metric> EMPTY_METRIC_MAP = new TreeMap<>();
    private static final SortedMap<String, HealthCheck.Result> EMPTY_HEALTHCHECK_MAP = new TreeMap<>();

    private static AtomicBoolean configured = new AtomicBoolean(false);
    private static AtomicBoolean shutdown = new AtomicBoolean(false);

    private static String prefix;

    private static MonitoringCenterConfig initialConfig;    // Useful for non-reloadable properties
    private static MonitoringCenterConfig currentConfig;

    private static ScheduledExecutorService executorService;

    private static MetricRegistry metricRegistry;
    private static GraphiteReporter graphiteReporter;

    private static HealthCheckRegistry healthCheckRegistry;

    private static SystemMetricSet systemMetricSet;
    private static TomcatMetricSet tomcatMetricSet;

    private static SystemInfo systemInfo;
    private static NodeInfo nodeInfo;
    private static AppInfo appInfo;

    private MonitoringCenter() {
    }

    /**
     * Configures the MonitoringCenter. The idiom of choice for using this method is via static helper methods in the
     * {@link Configurator}. This method should be called prior to any other calls to this class.
     *
     * @param config MonitoringCenter configuration.
     * @throws IllegalStateException if the MonitoringCenter has already been configured.
     */
    public static void configure(MonitoringCenterConfig config) {
        configure(config, false);
    }

    /**
     * Retrieves a {@link MetricCollector} with a given namespace. The metric collector's namespace will be constructed
     * from the simple name of the provided class and the optional additional namespaces.
     * <br><br>
     * The additional namespaces will be sanitized as described in {@link MetricNamingUtil#join(String, String...)}.
     * <br><br>
     * This method triggers the MonitoringCenter auto-configuration, if the {@link #configure(MonitoringCenterConfig)}
     * has not been called explicitly.
     *
     * @param clazz class, whose simple name will serve as the main namespace for the metric collector.
     * @param namespaces supplementary namespaces to use for the metric collector.
     * @return a new or cached metric collector with the provided namespace.
     * @throws NullPointerException if <tt>clazz</tt> is <tt>null</tt>.
     */
    public static MetricCollector getMetricCollector(final Class<?> clazz, String... namespaces) {
        Preconditions.checkNotNull(clazz, "class cannot be null");
        return getMetricCollector(clazz.getSimpleName(), namespaces);
    }

    /**
     * Retrieves a {@link MetricCollector} with a given namespace. The metric collector's namespace will be constructed
     * from the main namespace and the optional additional namespaces.
     * <br><br>
     * The main namespace will be sanitized in accordance with {@link MetricNamingUtil#sanitize(String)}. Plus, it
     * cannot be one of the reserved namespaces: "system" and "tomcat". The additional namespaces will be sanitized as
     * described in {@link MetricNamingUtil#join(String, String...)}.
     * <br><br>
     * This method triggers the MonitoringCenter auto-configuration, if the {@link #configure(MonitoringCenterConfig)}
     * has not been called explicitly.
     *
     * @param mainNamespace main namespace.
     * @param additionalNamespaces supplementary namespaces to use for the metric collector.
     * @return a new or cached metric collector with the provided namespace.
     * @throws IllegalArgumentException if <tt>mainNamespace</tt> is blank.
     */
    public static MetricCollector getMetricCollector(String mainNamespace, String... additionalNamespaces) {
        Preconditions.checkArgument(StringUtils.isNotBlank(mainNamespace), "mainNamespace cannot be blank");

        if (!configured.get()) {
            logger.debug("MonitoringCenter has not been configured. Returning a NoOpMetricCollector.");
            return new NoOpMetricCollector();
        }

        String sanitizedMainNamespace = MetricNamingUtil.sanitize(mainNamespace);

        Preconditions.checkArgument(!RESERVED_NAMESPACES.contains(sanitizedMainNamespace));

        String collectorNamespace = MetricNamingUtil.join(sanitizedMainNamespace, additionalNamespaces);

        return new MetricCollectorImpl(metricRegistry, initialConfig.getNamingConfig(), collectorNamespace);
    }

    /**
     * Retrieves a MetricCollector, whose main namespace is "dbs". This method is a convenience method for ensuring
     * consistent naming for database-related metrics.
     *
     * <br><br>
     * The additional namespaces will be sanitized as described in {@link MetricNamingUtil#join(String, String...)}.
     * <br><br>
     * This method triggers the MonitoringCenter auto-configuration, if the {@link #configure(MonitoringCenterConfig)}
     * has not been called explicitly.
     *
     * @param additionalNamespaces supplementary namespaces to use for the metric collector.
     * @return a metric collector, whose main namespace is "dbs".
     */
    public static MetricCollector getDatabaseMetricCollector(String... additionalNamespaces) {
        return getMetricCollector(DB_METRIC_NAMESPACE, additionalNamespaces);
    }

    /**
     * Retrieves a sorted map of counters by names. The returned map is sorted in accordance with natural order of
     * metric names (alphabetical ascending). The metric names will not have the node-specific prefix appended.
     *
     * @return a sorted map of counters by names or an empty map if no counters are found.
     */
    public static SortedMap<String, Counter> getCountersByNames() {
        return getMetricsByNames(false, null, Counter.class);
    }

    /**
     * Retrieves a sorted map of counters by names. The returned map is sorted in accordance with natural order of
     * metric names (alphabetical ascending).
     * <br><br>
     * If <tt>appendPrefix</tt> is <tt>true</tt>, the node-specific prefix will be appended to all metric names.
     *
     * @param appendPrefix whether to append the node-specific prefix to names of metrics or not.
     * @return a sorted map of counters by names or an empty map if no counters are found.
     */
    public static SortedMap<String, Counter> getCountersByNames(boolean appendPrefix) {
        return getMetricsByNames(appendPrefix, null, Counter.class);
    }

    /**
     * Retrieves a sorted map of counters by names. The returned map is sorted in accordance with natural order of
     * metric names (alphabetical ascending). The retrieved counters have to match the provided filters. These filters
     * can include the {@link net.centro.rtb.monitoringcenter.util.MetricNamingUtil#SEPARATOR} and multiple wildcards
     * denoted as <tt>*</tt>.
     * <br><br>
     * If <tt>appendPrefix</tt> is <tt>true</tt>, the node-specific prefix will be appended to all metric names.
     *
     * @param appendPrefix whether to append the node-specific prefix to names of metrics or not.
     * @param startsWithFilters an array of filters to apply to names of metrics.
     * @return a sorted map of counters by names or an empty map if no counters matching the constraints are found.
     */
    public static SortedMap<String, Counter> getCountersByNames(boolean appendPrefix, String[] startsWithFilters) {
        return getMetricsByNames(appendPrefix, startsWithFilters, Counter.class);
    }

    /**
     * Retrieves a sorted map of meters by names. The returned map is sorted in accordance with natural order of
     * metric names (alphabetical ascending). The metric names will not have the node-specific prefix appended.
     *
     * @return a sorted map of meters by names or an empty map if no meters are found.
     */
    public static SortedMap<String, Meter> getMetersByNames() {
        return getMetricsByNames(false, null, Meter.class);
    }

    /**
     * Retrieves a sorted map of meters by names. The returned map is sorted in accordance with natural order of
     * metric names (alphabetical ascending).
     * <br><br>
     * If <tt>appendPrefix</tt> is <tt>true</tt>, the node-specific prefix will be appended to all metric names.
     *
     * @param appendPrefix whether to append the node-specific prefix to names of metrics or not.
     * @return a sorted map of meters by names or an empty map if no meters are found.
     */
    public static SortedMap<String, Meter> getMetersByNames(boolean appendPrefix) {
        return getMetricsByNames(appendPrefix, null, Meter.class);
    }

    /**
     * Retrieves a sorted map of meters by names. The returned map is sorted in accordance with natural order of
     * metric names (alphabetical ascending). The retrieved meters have to match the provided filters. These filters
     * can include the {@link net.centro.rtb.monitoringcenter.util.MetricNamingUtil#SEPARATOR} and multiple wildcards
     * denoted as <tt>*</tt>.
     * <br><br>
     * If <tt>appendPrefix</tt> is <tt>true</tt>, the node-specific prefix will be appended to all metric names.
     *
     * @param appendPrefix whether to append the node-specific prefix to names of metrics or not.
     * @param startsWithFilters an array of filters to apply to names of metrics.
     * @return a sorted map of meters by names or an empty map if no meters matching the constraints are found.
     */
    public static SortedMap<String, Meter> getMetersByNames(boolean appendPrefix, String[] startsWithFilters) {
        return getMetricsByNames(appendPrefix, startsWithFilters, Meter.class);
    }

    /**
     * Retrieves a sorted map of histograms by names. The returned map is sorted in accordance with natural order of
     * metric names (alphabetical ascending). The metric names will not have the node-specific prefix appended.
     *
     * @return a sorted map of histograms by names or an empty map if no histograms are found.
     */
    public static SortedMap<String, Histogram> getHistogramsByNames() {
        return getMetricsByNames(false, null, Histogram.class);
    }

    /**
     * Retrieves a sorted map of histograms by names. The returned map is sorted in accordance with natural order of
     * metric names (alphabetical ascending).
     * <br><br>
     * If <tt>appendPrefix</tt> is <tt>true</tt>, the node-specific prefix will be appended to all metric names.
     *
     * @param appendPrefix whether to append the node-specific prefix to names of metrics or not.
     * @return a sorted map of histograms by names or an empty map if no histograms are found.
     */
    public static SortedMap<String, Histogram> getHistogramsByNames(boolean appendPrefix) {
        return getMetricsByNames(appendPrefix, null, Histogram.class);
    }

    /**
     * Retrieves a sorted map of histograms by names. The returned map is sorted in accordance with natural order of
     * metric names (alphabetical ascending). The retrieved histograms have to match the provided filters. These filters
     * can include the {@link net.centro.rtb.monitoringcenter.util.MetricNamingUtil#SEPARATOR} and multiple wildcards
     * denoted as <tt>*</tt>.
     * <br><br>
     * If <tt>appendPrefix</tt> is <tt>true</tt>, the node-specific prefix will be appended to all metric names.
     *
     * @param appendPrefix whether to append the node-specific prefix to names of metrics or not.
     * @param startsWithFilters an array of filters to apply to names of metrics.
     * @return a sorted map of histograms by names or an empty map if no histograms matching the constraints are found.
     */
    public static SortedMap<String, Histogram> getHistogramsByNames(boolean appendPrefix, String[] startsWithFilters) {
        return getMetricsByNames(appendPrefix, startsWithFilters, Histogram.class);
    }

    /**
     * Retrieves a sorted map of timers by names. The returned map is sorted in accordance with natural order of
     * metric names (alphabetical ascending). The metric names will not have the node-specific prefix appended.
     *
     * @return a sorted map of timers by names or an empty map if no timers are found.
     */
    public static SortedMap<String, Timer> getTimersByNames() {
        return getMetricsByNames(false, null, Timer.class);
    }

    /**
     * Retrieves a sorted map of timers by names. The returned map is sorted in accordance with natural order of
     * metric names (alphabetical ascending).
     * <br><br>
     * If <tt>appendPrefix</tt> is <tt>true</tt>, the node-specific prefix will be appended to all metric names.
     *
     * @param appendPrefix whether to append the node-specific prefix to names of metrics or not.
     * @return a sorted map of timers by names or an empty map if no timers are found.
     */
    public static SortedMap<String, Timer> getTimersByNames(boolean appendPrefix) {
        return getMetricsByNames(appendPrefix, null, Timer.class);
    }

    /**
     * Retrieves a sorted map of timers by names. The returned map is sorted in accordance with natural order of
     * metric names (alphabetical ascending). The retrieved timers have to match the provided filters. These filters
     * can include the {@link net.centro.rtb.monitoringcenter.util.MetricNamingUtil#SEPARATOR} and multiple wildcards
     * denoted as <tt>*</tt>.
     * <br><br>
     * If <tt>appendPrefix</tt> is <tt>true</tt>, the node-specific prefix will be appended to all metric names.
     *
     * @param appendPrefix whether to append the node-specific prefix to names of metrics or not.
     * @param startsWithFilters an array of filters to apply to names of metrics.
     * @return a sorted map of timers by names or an empty map if no timers matching the constraints are found.
     */
    public static SortedMap<String, Timer> getTimersByNames(boolean appendPrefix, String[] startsWithFilters) {
        return getMetricsByNames(appendPrefix, startsWithFilters, Timer.class);
    }

    /**
     * Retrieves a sorted map of gauges by names. The returned map is sorted in accordance with natural order of
     * metric names (alphabetical ascending). The metric names will not have the node-specific prefix appended.
     *
     * @return a sorted map of gauges by names or an empty map if no gauges are found.
     */
    public static SortedMap<String, Gauge> getGaugesByNames() {
        return getMetricsByNames(false, null, Gauge.class);
    }

    /**
     * Retrieves a sorted map of gauges by names. The returned map is sorted in accordance with natural order of
     * metric names (alphabetical ascending).
     * <br><br>
     * If <tt>appendPrefix</tt> is <tt>true</tt>, the node-specific prefix will be appended to all metric names.
     *
     * @param appendPrefix whether to append the node-specific prefix to names of metrics or not.
     * @return a sorted map of gauges by names or an empty map if no gauges are found.
     */
    public static SortedMap<String, Gauge> getGaugesByNames(boolean appendPrefix) {
        return getMetricsByNames(appendPrefix, null, Gauge.class);
    }

    /**
     * Retrieves a sorted map of gauges by names. The returned map is sorted in accordance with natural order of
     * metric names (alphabetical ascending). The retrieved gauges have to match the provided filters. These filters
     * can include the {@link net.centro.rtb.monitoringcenter.util.MetricNamingUtil#SEPARATOR} and multiple wildcards
     * denoted as <tt>*</tt>.
     * <br><br>
     * If <tt>appendPrefix</tt> is <tt>true</tt>, the node-specific prefix will be appended to all metric names.
     *
     * @param appendPrefix whether to append the node-specific prefix to names of metrics or not.
     * @param startsWithFilters an array of filters to apply to names of metrics.
     * @return a sorted map of gauges by names or an empty map if no gauges matching the constraints are found.
     */
    public static SortedMap<String, Gauge> getGaugesByNames(boolean appendPrefix, String[] startsWithFilters) {
        return getMetricsByNames(appendPrefix, startsWithFilters, Gauge.class);
    }

    /**
     * Retrieves a sorted map of metrics by names. The returned map is sorted in accordance with natural order of
     * metric names (alphabetical ascending). The metric names will not have the node-specific prefix appended.
     *
     * @return a sorted map of metrics by names or an empty map if no metrics are found.
     */
    public static SortedMap<String, Metric> getMetricsByNames() {
        return getMetricsByNames(false, null, null);
    }

    /**
     * Retrieves a sorted map of metrics by names. The returned map is sorted in accordance with natural order of
     * metric names (alphabetical ascending).
     * <br><br>
     * If <tt>appendPrefix</tt> is <tt>true</tt>, the node-specific prefix will be appended to all metric names.
     *
     * @param appendPrefix whether to append the node-specific prefix to names of metrics or not.
     * @return a sorted map of metrics by names or an empty map if no metrics are found.
     */
    public static SortedMap<String, Metric> getMetricsByNames(boolean appendPrefix) {
        return getMetricsByNames(appendPrefix, null, null);
    }

    /**
     * Retrieves a sorted map of metrics by names. The returned map is sorted in accordance with natural order of
     * metric names (alphabetical ascending). The retrieved metrics have to match the provided filters. These filters
     * can include the {@link net.centro.rtb.monitoringcenter.util.MetricNamingUtil#SEPARATOR} and multiple wildcards
     * denoted as <tt>*</tt>.
     * <br><br>
     * If <tt>appendPrefix</tt> is <tt>true</tt>, the node-specific prefix will be appended to all metric names.
     *
     * @param appendPrefix whether to append the node-specific prefix to names of metrics or not.
     * @param startsWithFilters an array of filters to apply to names of metrics.
     * @return a sorted map of metrics by names or an empty map if no metrics matching the constraints are found.
     */
    public static SortedMap<String, Metric> getMetricsByNames(boolean appendPrefix, String[] startsWithFilters) {
        return getMetricsByNames(appendPrefix, startsWithFilters, null);
    }

    /**
     * Removes all registered metrics. This method is especially useful for unit tests.
     */
    public static void removeAllMetrics() {
        if (!configured.get()) {
            return;
        }

        metricRegistry.removeMatching(MetricFilter.ALL);
    }

    /**
     * Registers a health check. This method is thread-safe. An attempt to register a health check, which has already
     * been registered will be a no-op. The actual name of the health check may contain a postfix, depending on the
     * value returned by {@link NamingConfig#isAppendTypeToHealthCheckNames()}.
     * <br><br>
     * All illegal characters contained in the provided name will be escaped as documented for
     * {@link net.centro.rtb.monitoringcenter.util.MetricNamingUtil#sanitize(String)}.
     * <br><br>
     * This method triggers the MonitoringCenter auto-configuration, if the {@link #configure(MonitoringCenterConfig)}
     * has not been called explicitly.
     *
     * @param name name of the health check to register.
     * @param healthCheck a health check to register.
     * @throws NullPointerException if <tt>healthCheck</tt> is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>name</tt> is blank.
     */
    public static void registerHealthCheck(String name, HealthCheck healthCheck) {
        Preconditions.checkNotNull(healthCheck, "healthCheck cannot be null");

        if (!configured.get()) {
            logger.debug("MonitoringCenter has not been configured. The health check will not be registered.");
            return;
        }

        healthCheckRegistry.register(normalizeHealthCheckName(name), healthCheck);
    }

    /**
     * Executes all registered health checks and returns their results. The returned map is sorted in accordance with
     * natural order of health check names (alphabetical ascending).
     *
     * @return sorted map of results of executed health checks by health check names; an empty map if the
     * MonitoringCenter has not been configured.
     */
    public static SortedMap<String, HealthCheck.Result> runHealthChecks() {
        if (!configured.get()) {
            return EMPTY_HEALTHCHECK_MAP;
        }

        return healthCheckRegistry.runHealthChecks();
    }

    /**
     * Executes a health check and returns its result. The actual name of the health check may contain a postfix,
     * depending on the value returned by {@link NamingConfig#isAppendTypeToHealthCheckNames()}.
     * <br><br>
     * All illegal characters contained in the provided name will be escaped as documented for
     * {@link net.centro.rtb.monitoringcenter.util.MetricNamingUtil#sanitize(String)}.
     *
     * @param name name of the health check to execute.
     * @return result of the executed health check.
     * @throws IllegalArgumentException if <tt>name</tt> is blank.
     * @throws NoSuchElementException if MonitoringCenter has not been configured or if health check could not be found.
     */
    public static HealthCheck.Result runHealthCheck(String name) {
        if (!configured.get()) {
            throw new NoSuchElementException("MonitoringCenter has not been initialized. No health checks are available.");
        }

        String normalizedName = normalizeHealthCheckName(name);
        try {
            return healthCheckRegistry.runHealthCheck(normalizedName);
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("Health check " + normalizedName + " could not be found.");
        }
    }

    /**
     * Removes all registered health checks. This method is especially useful for unit tests.
     */
    public static void removeAllHealthChecks() {
        if (!configured.get()) {
            return;
        }

        for (String name : healthCheckRegistry.getNames()) {
            healthCheckRegistry.unregister(name);
        }
    }

    /**
     * Removes a health check, effectively voiding its registration with the MonitoringCenter. The actual name of the
     * health check may contain a postfix, depending on the value returned by
     * {@link NamingConfig#isAppendTypeToHealthCheckNames()}.
     * <br><br>
     * All illegal characters contained in the provided name will be escaped as documented for
     * {@link net.centro.rtb.monitoringcenter.util.MetricNamingUtil#sanitize(String)}.
     *
     * @param name name of the health check to remove.
     * @throws IllegalArgumentException if <tt>name</tt> is blank.
     */
    public static void removeHealthCheck(String name) {
        if (!configured.get()) {
            return;
        }

        healthCheckRegistry.unregister(normalizeHealthCheckName(name));
    }

    /**
     * Retrieves the current system status, consisting of the operating system and JVM statuses.
     *
     * @return the current system status, consisting of the operating system and JVM statuses.
     */
    public static SystemStatus getSystemStatus() {
        return systemMetricSet;
    }

    /**
     * Retrieves the current status for Tomcat's executors and connectors.
     *
     * @return the current status for Tomcat's executors and connectors; <tt>null</tt> if MonitoringCenter has not been
     * configured.
     */
    public static TomcatStatus getTomcatStatus() {
        return tomcatMetricSet;
    }

    /**
     * Retrieves information about the operating system and the JVM.
     *
     * @return information about the operating system and the JVM; <tt>null</tt> if MonitoringCenter has not been
     * configured.
     */
    public static SystemInfo getSystemInfo() {
        return systemInfo;
    }

    /**
     * Retrieves information about this network node. In this context, the definition of a node is somewhat loose
     * and generally means any physical or virtual server running one or more applications.
     *
     * @return information about this network node; <tt>null</tt> if MonitoringCenter has not been configured.
     */
    public static NodeInfo getNodeInfo() {
        return nodeInfo;
    }

    /**
     * Retrieves information about the application, including the application name and build context.
     *
     * @return information about the application; <tt>null</tt> if MonitoringCenter has not been configured.
     */
    public static AppInfo getAppInfo() {
        return appInfo;
    }

    /**
     * Shuts the MonitoringCenter down, causing a cascading shutdown on all objects owned by this class. This method is
     * synchronous and it may block for a few seconds.
     */
    public static void shutdown() {
        if (shutdown.getAndSet(true)) {
            return;
        }

        if (executorService != null) {
            MoreExecutors.shutdownAndAwaitTermination(executorService, 1, TimeUnit.SECONDS);
        }

        if (graphiteReporter != null) {
            MonitoringCenterConfig config = currentConfig;
            if (config != null && config.getMetricReportingConfig() != null) {
                GraphiteReporterConfig graphiteReporterConfig = config.getMetricReportingConfig().getGraphiteReporterConfig();
                if (graphiteReporterConfig != null && graphiteReporterConfig.isReportOnShutdown()) {
                    graphiteReporter.report();
                }
            }
            graphiteReporter.stop();
        }

        if (systemMetricSet != null) {
            systemMetricSet.shutdown();
        }

        if (tomcatMetricSet != null) {
            tomcatMetricSet.shutdown();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Metric> SortedMap<String, T> getMetricsByNames(boolean appendPrefix, String[] startsWithFilters, Class<T> metricClass) {
        if (!configured.get()) {
            return (SortedMap<String, T>) EMPTY_METRIC_MAP;
        }

        SortedMap<String, T> metricsByNames = new TreeMap<>();
        for (Map.Entry<String, Metric> entry : metricRegistry.getMetrics().entrySet()) {
            if (metricClass == null || metricClass.isAssignableFrom(entry.getValue().getClass())) {
                if (startsWithFilters == null || matchesStartsWithFilters(entry.getKey(), startsWithFilters)) {
                    String metricName = (appendPrefix ? prefix + MetricNamingUtil.SEPARATOR : StringUtils.EMPTY) + entry.getKey();
                    metricsByNames.put(metricName, (T) entry.getValue());
                }
            }
        }
        return metricsByNames;
    }

    private static synchronized void configure(MonitoringCenterConfig config, boolean internalCall) {
        if (configured.get()) {
            if (internalCall) {
                return;
            } else {
                throw new IllegalStateException("MonitoringCenter has already been configured!");
            }
        }

        Preconditions.checkNotNull(config, "config cannot be null");

        NamingConfig namingConfig = config.getNamingConfig();
        prefix = new StringBuilder()
                .append(namingConfig.getApplicationName()).append(MetricNamingUtil.SEPARATOR)
                .append(namingConfig.getDatacenterName()).append(MetricNamingUtil.SEPARATOR)
                .append(namingConfig.getNodeGroupName()).append(MetricNamingUtil.SEPARATOR)
                .append(namingConfig.getNodeId()).toString();

        initialConfig = config;
        currentConfig = config;
        metricRegistry = new MetricRegistry();
        healthCheckRegistry = new HealthCheckRegistry();

        // Set up default metric sets
        MetricCollectionConfig metricCollectionConfig = config.getMetricCollectionConfig();
        if (metricCollectionConfig != null) {
            if (metricCollectionConfig.isEnableSystemMetrics()) {
                systemMetricSet = new SystemMetricSet();
                metricRegistry.register(SYSTEM_METRIC_NAMESPACE, systemMetricSet);
            }

            if (metricCollectionConfig.isEnableTomcatMetrics()) {
                tomcatMetricSet = new TomcatMetricSet();
                metricRegistry.register(TOMCAT_METRIC_NAMESPACE, tomcatMetricSet);
            }
        }

        // Configure reporters
        if (config.getMetricReportingConfig() != null) {
            GraphiteReporterConfig graphiteReporterConfig = config.getMetricReportingConfig().getGraphiteReporterConfig();
            if (graphiteReporterConfig != null && graphiteReporterConfig.isEnableReporter()) {
                initGraphiteReporter(graphiteReporterConfig);
                logger.info("Started GraphiteReporter: {}", graphiteReporterConfig.toString());
            }
        }

        // Init infos
        systemInfo = SystemInfo.create();
        nodeInfo = NodeInfo.create(namingConfig);
        appInfo = AppInfo.create(namingConfig.getApplicationName());

        // Set up config file reloading
        if (config.getConfigFile() != null && config.getConfigFile().exists()) {
            ConfigFileUtil.createEffectiveConfigFile(config);

            executorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("MonitoringCenter-%d").build());
            executorService.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    try {
                        reloadConfig();
                    } catch (Exception e) {
                        if (InterruptedException.class.isInstance(e)) {
                            Thread.currentThread().interrupt();
                        } else {
                            logger.error("Uncaught exception occurred while reloading the MonitoringCenter config", e);
                        }
                    }
                }
            }, CONFIG_RELOAD_INTERVAL_IN_SECONDS, CONFIG_RELOAD_INTERVAL_IN_SECONDS, TimeUnit.SECONDS);
        }

        logger.info("MonitoringCenter has been configured: {}", initialConfig.toString());
        configured.set(true);
    }

    protected static void reloadConfig() {
        if (!configured.get() || initialConfig.getConfigFile() == null) {
            return;
        }

        File effectiveConfigFile = new File(ConfigFileUtil.getEffectiveConfigFileFullPath(initialConfig));
        MonitoringCenterConfig newConfig = null;
        try {
            newConfig = Configurator.configFile(effectiveConfigFile).build();
        } catch (Exception e) {
            logger.error("Error while reloading MonitoringCenter config from {}", effectiveConfigFile.toString(), e);

            if (InterruptedException.class.isInstance(e)) {
                Thread.currentThread().interrupt();
            }

            return;
        }

        // Update metric collection, if needed
        MetricCollectionConfig newMetricCollectionConfig = newConfig.getMetricCollectionConfig();
        if (newMetricCollectionConfig != null) {     // If it's null, something is wrong, so keep old values
            MetricCollectionConfig oldMetricCollectionConfig = currentConfig.getMetricCollectionConfig();

            if (newMetricCollectionConfig.isEnableSystemMetrics()) {
                if (oldMetricCollectionConfig == null || !oldMetricCollectionConfig.isEnableSystemMetrics()) {
                    systemMetricSet = new SystemMetricSet();
                    metricRegistry.register(SYSTEM_METRIC_NAMESPACE, systemMetricSet);
                }
            } else {
                if (oldMetricCollectionConfig != null && oldMetricCollectionConfig.isEnableSystemMetrics()) {
                    if (systemMetricSet != null) {
                        metricRegistry.removeMatching(new MetricFilter() {
                            @Override
                            public boolean matches(String name, Metric metric) {
                                return name.startsWith(SYSTEM_METRIC_NAMESPACE + MetricNamingUtil.SEPARATOR);
                            }
                        });
                        systemMetricSet.shutdown();
                        systemMetricSet = null;
                    }
                }
            }

            if (newMetricCollectionConfig.isEnableTomcatMetrics()) {
                if (oldMetricCollectionConfig == null || !oldMetricCollectionConfig.isEnableTomcatMetrics()) {
                    tomcatMetricSet = new TomcatMetricSet();
                    metricRegistry.register(TOMCAT_METRIC_NAMESPACE, tomcatMetricSet);
                }
            } else {
                if (oldMetricCollectionConfig != null && oldMetricCollectionConfig.isEnableTomcatMetrics()) {
                    if (tomcatMetricSet != null) {
                        metricRegistry.removeMatching(new MetricFilter() {
                            @Override
                            public boolean matches(String name, Metric metric) {
                                return name.startsWith(TOMCAT_METRIC_NAMESPACE + MetricNamingUtil.SEPARATOR);
                            }
                        });
                        tomcatMetricSet.shutdown();
                        tomcatMetricSet = null;
                    }
                }
            }
        }

        // Reload GraphiteReporter
        GraphiteReporterConfig oldGraphiteReporterConfig = null;
        if (currentConfig.getMetricReportingConfig() != null) {
            oldGraphiteReporterConfig = currentConfig.getMetricReportingConfig().getGraphiteReporterConfig();
        }

        GraphiteReporterConfig newGraphiteReporterConfig = null;
        if (newConfig.getMetricReportingConfig() != null) {
            newGraphiteReporterConfig = newConfig.getMetricReportingConfig().getGraphiteReporterConfig();
        }

        if (graphiteReporter != null && (oldGraphiteReporterConfig != null && oldGraphiteReporterConfig.isEnableReporter())) {
            if (newGraphiteReporterConfig == null || !newGraphiteReporterConfig.equals(oldGraphiteReporterConfig)) {
                graphiteReporter.stop();
                graphiteReporter = null;

                if (newGraphiteReporterConfig != null && newGraphiteReporterConfig.isEnableReporter()) {
                    initGraphiteReporter(newGraphiteReporterConfig);
                    logger.info("GraphiteReporter has been updated: {}", newGraphiteReporterConfig.toString());
                } else {
                    logger.info("GraphiteReporter has been turned off");
                }
            }
        } else {
            if (newGraphiteReporterConfig != null && newGraphiteReporterConfig.isEnableReporter()) {
                initGraphiteReporter(newGraphiteReporterConfig);
                logger.info("Started GraphiteReporter: {}", newGraphiteReporterConfig.toString());
            }
        }

        currentConfig = newConfig;
    }

    private static void initGraphiteReporter(final GraphiteReporterConfig graphiteReporterConfig) {
        HostAndPort hostAndPort = graphiteReporterConfig.getAddress();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(hostAndPort.getHost(), hostAndPort.getPort());

        GraphiteSender graphiteSender = graphiteReporterConfig.isEnableBatching()
                ? new PickledGraphite(inetSocketAddress)
                : new Graphite(inetSocketAddress);

        graphiteReporter = GraphiteReporter.forRegistry(metricRegistry)
                .prefixedWith(prefix)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MICROSECONDS)
                .withClock(new Clock() {
                    private long lastReportingTime = 0;

                    @Override
                    public long getTick() {
                        return System.nanoTime();
                    }

                    @Override
                    public synchronized long getTime() {
                        if (lastReportingTime == 0) {
                            lastReportingTime = System.currentTimeMillis();
                            return lastReportingTime;
                        }
                        lastReportingTime += graphiteReporterConfig.getReportingIntervalInSeconds() * 1000;
                        return lastReportingTime;
                    }
                })
                .filter(new MetricFilter() {
                    @Override
                    public boolean matches(String name, Metric metric) {
                        boolean passedWhitelist = false;
                        if (graphiteReporterConfig.getStartsWithFilters().isEmpty()) {
                            passedWhitelist = true;
                        } else {
                            passedWhitelist = matchesStartsWithFilters(name, graphiteReporterConfig.getStartsWithFilters().toArray(new String[] {}));
                        }

                        if (!passedWhitelist) {
                            return false;
                        }

                        if (graphiteReporterConfig.getBlockedStartsWithFilters().isEmpty()) {
                            return true;
                        } else {
                            return !matchesStartsWithFilters(name, graphiteReporterConfig.getBlockedStartsWithFilters().toArray(new String[] {}));
                        }
                    }
                })
                .build(graphiteSender);

        graphiteReporter.start(graphiteReporterConfig.getReportingIntervalInSeconds(), TimeUnit.SECONDS);
    }

    private static String normalizeHealthCheckName(String name) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name), "name cannot be blank");

        String sanitizedName = MetricNamingUtil.sanitize(name);
        if (initialConfig.getNamingConfig() != null && initialConfig.getNamingConfig().isAppendTypeToHealthCheckNames()) {
            if (!sanitizedName.endsWith(HEALTH_CHECK_POSTFIX)) {
                sanitizedName += HEALTH_CHECK_POSTFIX;
            }
        }

        return sanitizedName;
    }

    private static boolean matchesStartsWithFilters(String name, String[] startsWithFilters) {
        if (startsWithFilters == null || startsWithFilters.length == 0) {
            return true;
        }

        for (String startsWithFilter : startsWithFilters) {
            if (matchesStartsWithFilter(name, startsWithFilter)) {
                return true;
            }
        }

        return false;
    }

    private static boolean matchesStartsWithFilter(String name, String startsWithFilter) {
        startsWithFilter = StringUtils.trimToNull(startsWithFilter);
        if (startsWithFilter == null) {
            return true;
        }

        if (startsWithFilter.indexOf('*') == -1) {
            return name.startsWith(startsWithFilter);
        } else {
            if (!startsWithFilter.endsWith("*")) {
                startsWithFilter += "*";
            }

            String regex = ("\\Q" + startsWithFilter + "\\E").replace("*", "\\E.*\\Q");
            return name.matches(regex);
        }
    }
}

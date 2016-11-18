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

package net.centro.rtb.monitoringcenter.metrics.tomcat;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.centro.rtb.monitoringcenter.util.MetricNamingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class TomcatMetricSet implements MetricSet, TomcatStatus {
    private static final Logger logger = LoggerFactory.getLogger(TomcatMetricSet.class);

    private MBeanServer mBeanServer;
    private Map<String, Metric> metricsByNames;

    // For QPS calc
    private ScheduledExecutorService executorService;

    private List<TomcatExecutorStatus> executorStatuses;
    private List<TomcatConnectorStatus> connectorStatuses;

    private AtomicBoolean shutdown;

    public TomcatMetricSet() {
        this.mBeanServer = ManagementFactory.getPlatformMBeanServer();

        Map<String, Metric> metricsByNames = new HashMap<>();

        // Executors
        List<TomcatExecutorStatus> executorStatuses = new ArrayList<>();

        Set<ObjectName> executorObjectNames = null;
        try {
            executorObjectNames = mBeanServer.queryNames(new ObjectName("Catalina:type=Executor,*"), null);
        } catch (MalformedObjectNameException e) {
            logger.debug("Invalid ObjectName defined for the Tomcat's Executor MxBean", e);
        }

        if (executorObjectNames != null && !executorObjectNames.isEmpty()) {
            for (final ObjectName executorObjectName : executorObjectNames) {
                TomcatExecutorMetricSet executorMetricSet = new TomcatExecutorMetricSet(executorObjectName);
                if (!executorMetricSet.getMetrics().isEmpty()) {
                    metricsByNames.put(MetricNamingUtil.join("executors", executorMetricSet.getName()), executorMetricSet);
                }
                executorStatuses.add(executorMetricSet);
            }
        }

        this.executorStatuses = executorStatuses;

        // Thread Pools
        final List<TomcatConnectorMetricSet> connectorMetricSets = new ArrayList<>();
        List<TomcatConnectorStatus> connectorStatuses = new ArrayList<>();

        Set<ObjectName> threadPoolObjectNames = null;
        try {
            threadPoolObjectNames = mBeanServer.queryNames(new ObjectName("Catalina:type=ThreadPool,*"), null);
        } catch (MalformedObjectNameException e) {
            logger.debug("Invalid ObjectName defined for the Tomcat's ThreadPool MxBean", e);
        }

        if (threadPoolObjectNames != null && !threadPoolObjectNames.isEmpty()) {
            for (final ObjectName threadPoolObjectName : threadPoolObjectNames) {
                TomcatConnectorMetricSet connectorMetricSet = new TomcatConnectorMetricSet(threadPoolObjectName);
                if (!connectorMetricSet.getMetrics().isEmpty()) {
                    metricsByNames.put(MetricNamingUtil.join("connectors", connectorMetricSet.getName()), connectorMetricSet);
                    connectorMetricSets.add(connectorMetricSet);
                }
                connectorStatuses.add(connectorMetricSet);
            }
        }

        this.connectorStatuses = connectorStatuses;

        if (!connectorMetricSets.isEmpty()) {
            this.executorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("TomcatMetricSet-%d").build());
            this.executorService.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    for (TomcatConnectorMetricSet connectorMetricSet : connectorMetricSets) {
                        try {
                            connectorMetricSet.updateQps();
                        } catch (Exception e) {
                            logger.debug("Error while updating QPS for connector {}", connectorMetricSet.getName(), e);
                        }
                    }
                }
            }, 1, 1, TimeUnit.SECONDS);
        }

        this.metricsByNames = metricsByNames;

        this.shutdown = new AtomicBoolean(false);
    }

    @Override
    public Map<String, Metric> getMetrics() {
        return Collections.unmodifiableMap(metricsByNames);
    }

    @JsonProperty
    @Override
    public List<TomcatExecutorStatus> getExecutorStatuses() {
        return Collections.unmodifiableList(executorStatuses);
    }

    @JsonProperty
    @Override
    public List<TomcatConnectorStatus> getConnectorStatuses() {
        return Collections.unmodifiableList(connectorStatuses);
    }

    public void shutdown() {
        if (shutdown.getAndSet(true)) {
            return;
        }

        if (executorService != null) {
            MoreExecutors.shutdownAndAwaitTermination(executorService, 1, TimeUnit.SECONDS);
        }
    }
}

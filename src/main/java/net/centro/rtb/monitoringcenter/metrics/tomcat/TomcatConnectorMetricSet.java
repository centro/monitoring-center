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

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import net.centro.rtb.monitoringcenter.util.JmxUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE)
class TomcatConnectorMetricSet implements MetricSet, TomcatConnectorStatus {
    private static final Logger logger = LoggerFactory.getLogger(TomcatConnectorStatus.class);

    private String name;
    private Integer port;
    private boolean isInternalPort;
    private boolean isSecure;
    private boolean isAjp;

    private int previousRequestCount;
    private AtomicInteger qpsHolder;

    private Gauge<Integer> currentPoolSizeGauge;
    private Gauge<Integer> maxPoolSizeGauge;
    private Gauge<Integer> busyThreadsGauge;
    private Gauge<Long> activeConnectionsGauge;
    private Gauge<Integer> maxConnectionsGauge;
    private Gauge<Integer> totalRequestsGauge;
    private Gauge<Integer> errorsGauge;
    private Gauge<Integer> qpsGauge;
    private Gauge<Long> receivedBytesGauge;
    private Gauge<Long> sentBytesGauge;

    private Map<String, Metric> metricsByNames;

    TomcatConnectorMetricSet(ObjectName threadPoolObjectName) {
        Preconditions.checkNotNull(threadPoolObjectName);

        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

        this.name = threadPoolObjectName.getKeyProperty("name");
        this.port = JmxUtil.getJmxAttribute(threadPoolObjectName, "port", Integer.class, null);

        Set<ObjectName> connectorObjectNames = null;
        try {
            connectorObjectNames = mBeanServer.queryNames(new ObjectName("Catalina:type=Connector,port=" + port), null);
        } catch (MalformedObjectNameException e) {
            logger.debug("Invalid ObjectName defined for the Tomcat's Connector MxBean for the {} thread pool", name, e);
        }

        if (connectorObjectNames != null && !connectorObjectNames.isEmpty()) {
            ObjectName connectorObjectName = connectorObjectNames.iterator().next();
            String internalPortStr = JmxUtil.getJmxAttribute(connectorObjectName, "internalPort", String.class, Boolean.FALSE.toString());
            this.isInternalPort = Boolean.TRUE.toString().equalsIgnoreCase(internalPortStr);
        }

        this.isSecure = JmxUtil.getJmxAttribute(threadPoolObjectName, "sSLEnabled", Boolean.class, Boolean.FALSE);
        this.isAjp = isAjpFromName(name);

        Map<String, Metric> metricsByNames = new HashMap<>();

        this.currentPoolSizeGauge = JmxUtil.getJmxAttributeAsGauge(threadPoolObjectName, "currentThreadCount", Integer.class, 0);
        if (currentPoolSizeGauge != null) {
            metricsByNames.put("currentPoolSize", currentPoolSizeGauge);
        }

        this.maxPoolSizeGauge = JmxUtil.getJmxAttributeAsGauge(threadPoolObjectName, "maxThreads", Integer.class, 0);
        if (maxPoolSizeGauge != null) {
            metricsByNames.put("maxPoolSize", maxPoolSizeGauge);
        }

        this.busyThreadsGauge = JmxUtil.getJmxAttributeAsGauge(threadPoolObjectName, "currentThreadsBusy", Integer.class, 0);
        if (busyThreadsGauge != null) {
            metricsByNames.put("busyThreads", busyThreadsGauge);
        }

        this.activeConnectionsGauge = JmxUtil.getJmxAttributeAsGauge(threadPoolObjectName, "connectionCount", Long.class, 0L);
        if (activeConnectionsGauge != null) {
            metricsByNames.put("activeConnections", activeConnectionsGauge);
        }

        this.maxConnectionsGauge = JmxUtil.getJmxAttributeAsGauge(threadPoolObjectName, "maxConnections", Integer.class, 0);
        if (maxConnectionsGauge != null) {
            metricsByNames.put("maxConnections", maxConnectionsGauge);
        }

        Set<ObjectName> globalRequestProcessorObjectNames = null;
        try {
            globalRequestProcessorObjectNames = mBeanServer.queryNames(new ObjectName("Catalina:type=GlobalRequestProcessor,name=" + name), null);
        } catch (MalformedObjectNameException e) {
            logger.debug("Invalid ObjectName defined for the Tomcat's GlobalRequestProcessor MxBean for the {} thread pool", name, e);
        }

        if (globalRequestProcessorObjectNames != null && !globalRequestProcessorObjectNames.isEmpty()) {
            ObjectName globalRequestProcessorObjectName = globalRequestProcessorObjectNames.iterator().next();
            this.totalRequestsGauge = JmxUtil.getJmxAttributeAsGauge(globalRequestProcessorObjectName, "requestCount", Integer.class, 0);

            if (totalRequestsGauge != null) {
                metricsByNames.put("totalRequests", totalRequestsGauge);

                this.qpsHolder = new AtomicInteger();
                this.previousRequestCount = totalRequestsGauge.getValue();

                this.qpsGauge = new Gauge<Integer>() {
                    @Override
                    public Integer getValue() {
                        return qpsHolder.get();
                    }
                };
                metricsByNames.put("qps", qpsGauge);
            }

            this.errorsGauge = JmxUtil.getJmxAttributeAsGauge(globalRequestProcessorObjectName, "errorCount", Integer.class, 0);
            if (errorsGauge != null) {
                metricsByNames.put("errors", errorsGauge);
            }

            this.receivedBytesGauge = JmxUtil.getJmxAttributeAsGauge(globalRequestProcessorObjectName, "bytesReceived", Long.class, 0L);
            if (receivedBytesGauge != null) {
                metricsByNames.put("receivedBytes", receivedBytesGauge);
            }

            this.sentBytesGauge = JmxUtil.getJmxAttributeAsGauge(globalRequestProcessorObjectName, "bytesSent", Long.class, 0L);
            if (sentBytesGauge != null) {
                metricsByNames.put("sentBytes", sentBytesGauge);
            }
        }

        this.metricsByNames = metricsByNames;
    }

    // Not thread-safe
    void updateQps() {
        if (totalRequestsGauge == null) {
            return;
        }

        int currentRequestCount = totalRequestsGauge.getValue();
        qpsHolder.set(currentRequestCount - previousRequestCount);
        previousRequestCount = currentRequestCount;
    }

    @Override
    public Map<String, Metric> getMetrics() {
        return Collections.unmodifiableMap(metricsByNames);
    }

    @JsonProperty
    @Override
    public String getName() {
        return name;
    }

    @JsonProperty
    @Override
    public Integer getPort() {
        return port;
    }

    @JsonProperty
    @Override
    public boolean isInternalPort() {
        return isInternalPort;
    }

    @JsonProperty
    @Override
    public boolean isSecure() {
        return isSecure;
    }

    @JsonProperty
    @Override
    public boolean isAjp() {
        return isAjp;
    }

    @JsonProperty
    @Override
    public Gauge<Integer> getCurrentPoolSizeGauge() {
        return currentPoolSizeGauge;
    }

    @JsonProperty
    @Override
    public Gauge<Integer> getMaxPoolSizeGauge() {
        return maxPoolSizeGauge;
    }

    @JsonProperty
    @Override
    public Gauge<Integer> getBusyThreadsGauge() {
        return busyThreadsGauge;
    }

    @JsonProperty
    @Override
    public Gauge<Long> getActiveConnectionsGauge() {
        return activeConnectionsGauge;
    }

    @JsonProperty
    @Override
    public Gauge<Integer> getMaxConnectionsGauge() {
        return maxConnectionsGauge;
    }

    @JsonProperty
    @Override
    public Gauge<Integer> getTotalRequestsGauge() {
        return totalRequestsGauge;
    }

    @JsonProperty
    @Override
    public Gauge<Integer> getErrorsGauge() {
        return errorsGauge;
    }

    @JsonProperty
    @Override
    public Gauge<Integer> getQpsGauge() {
        return qpsGauge;
    }

    @JsonProperty
    @Override
    public Gauge<Long> getReceivedBytesGauge() {
        return receivedBytesGauge;
    }

    @JsonProperty
    @Override
    public Gauge<Long> getSentBytesGauge() {
        return sentBytesGauge;
    }

    private static boolean isAjpFromName(String connectorName) {
        if (connectorName == null) {
            return false;
        }

        return connectorName.startsWith("jk-") || connectorName.startsWith("\"jk-") || connectorName.startsWith("ajp-bio-") || connectorName.startsWith("\"ajp-bio-");
    }
}

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

package net.centro.rtb.monitoringcenter.config;

/**
 * This class holds the configuration parameters describing the naming for metrics and health checks. None of the naming
 * configuration parameters can be modified dynamically--in other words, they are not reloadable.
 */
public class NamingConfig {
    private String applicationName;
    private String datacenterName;
    private String nodeGroupName;
    private String nodeId;

    private MetricNamePostfixPolicy metricNamePostfixPolicy;
    private boolean appendTypeToHealthCheckNames;

    NamingConfig(String applicationName, String datacenterName, String nodeGroupName, String nodeId, MetricNamePostfixPolicy metricNamePostfixPolicy, boolean appendTypeToHealthCheckNames) {
        this.applicationName = applicationName;
        this.datacenterName = datacenterName;
        this.nodeGroupName = nodeGroupName;
        this.nodeId = nodeId;

        this.metricNamePostfixPolicy = metricNamePostfixPolicy;
        this.appendTypeToHealthCheckNames = appendTypeToHealthCheckNames;
    }

    /**
     * Retrieves the application name to be used in the node-specific prefix for metrics. The application name will also
     * be employed in the {@link net.centro.rtb.monitoringcenter.infos.AppInfo}. This value is guaranteed to be
     * non-null by the {@link net.centro.rtb.monitoringcenter.config.MonitoringCenterConfig.Builder}.
     *
     * @return the application name.
     */
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * Retrieves the data center name to be used in the node-specific prefix for metrics. The data center name will also
     * feature in the {@link net.centro.rtb.monitoringcenter.infos.NodeInfo}. This value is guaranteed to be non-null
     * with the default being {@link net.centro.rtb.monitoringcenter.config.MonitoringCenterConfig.Builder#NONE}.
     *
     * @return the data center name.
     */
    public String getDatacenterName() {
        return datacenterName;
    }

    /**
     * Retrieves the node group name to be used in the node-specific prefix for metrics. The node group name will also
     * feature in the {@link net.centro.rtb.monitoringcenter.infos.NodeInfo}. This value is guaranteed to be non-null
     * with the default being {@link net.centro.rtb.monitoringcenter.config.MonitoringCenterConfig.Builder#NONE}.
     *
     * @return the node group name.
     */
    public String getNodeGroupName() {
        return nodeGroupName;
    }

    /**
     * Retrieves the node ID to be used in the node-specific prefix for metrics. The node ID will also feature in the
     * {@link net.centro.rtb.monitoringcenter.infos.NodeInfo}. This value is guaranteed to be non-null with the default
     * being {@link net.centro.rtb.monitoringcenter.config.MonitoringCenterConfig.Builder#NONE}.
     *
     * @return the node ID.
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * Retrieves the policy for appending metric types to metric names. By default, the metric type will only be
     * appended to composite metrics (i.e., timers, histograms, and meters).
     *
     * @see MetricNamePostfixPolicy
     * @return the policy for appending metric types to metric names.
     */
    public MetricNamePostfixPolicy getMetricNamePostfixPolicy() {
        return metricNamePostfixPolicy;
    }

    /**
     * Indicates whether type ("HealthCheck") should be appended to health check names or not. By default, this feature
     * is disabled.
     *
     * @return whether type ("HealthCheck") should be appended to health check names or not.
     */
    public boolean isAppendTypeToHealthCheckNames() {
        return appendTypeToHealthCheckNames;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NamingConfig that = (NamingConfig) o;

        if (appendTypeToHealthCheckNames != that.appendTypeToHealthCheckNames) return false;
        if (applicationName != null ? !applicationName.equals(that.applicationName) : that.applicationName != null)
            return false;
        if (datacenterName != null ? !datacenterName.equals(that.datacenterName) : that.datacenterName != null)
            return false;
        if (nodeGroupName != null ? !nodeGroupName.equals(that.nodeGroupName) : that.nodeGroupName != null)
            return false;
        if (nodeId != null ? !nodeId.equals(that.nodeId) : that.nodeId != null) return false;
        return metricNamePostfixPolicy == that.metricNamePostfixPolicy;
    }

    @Override
    public int hashCode() {
        int result = applicationName != null ? applicationName.hashCode() : 0;
        result = 31 * result + (datacenterName != null ? datacenterName.hashCode() : 0);
        result = 31 * result + (nodeGroupName != null ? nodeGroupName.hashCode() : 0);
        result = 31 * result + (nodeId != null ? nodeId.hashCode() : 0);
        result = 31 * result + (metricNamePostfixPolicy != null ? metricNamePostfixPolicy.hashCode() : 0);
        result = 31 * result + (appendTypeToHealthCheckNames ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("NamingConfig{");
        sb.append("applicationName='").append(applicationName).append('\'');
        sb.append(", datacenterName='").append(datacenterName).append('\'');
        sb.append(", nodeGroupName='").append(nodeGroupName).append('\'');
        sb.append(", nodeId='").append(nodeId).append('\'');
        sb.append(", metricNamePostfixPolicy=").append(metricNamePostfixPolicy);
        sb.append(", appendTypeToHealthCheckNames=").append(appendTypeToHealthCheckNames);
        sb.append('}');
        return sb.toString();
    }
}

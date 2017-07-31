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

import net.centro.rtb.monitoringcenter.util.MetricNamingUtil;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletContext;
import java.io.File;

/**
 * MonitoringCenterConfig encapsulates the configuration parameters for the MonitoringCenter. It is intended to be
 * built by means of the builder which can be instantiated using {@link Configurator}.
 */
public class MonitoringCenterConfig {
    private File configFile;

    private NamingConfig namingConfig;
    private MetricCollectionConfig metricCollectionConfig;
    private MetricReportingConfig metricReportingConfig;
    private ServletContext servletContext;

    private MonitoringCenterConfig(Builder builder) {
        this.configFile = builder.configFile;

        this.namingConfig = new NamingConfig(builder.applicationName, builder.datacenterName, builder.nodeGroupName,
                builder.nodeId, builder.metricNamePostfixPolicy, builder.appendTypeToHealthCheckNames);
        this.metricCollectionConfig = new MetricCollectionConfig(builder.enableSystemMetrics, builder.enableTomcatMetrics, builder.enableWebAppMetrics);
        this.metricReportingConfig = new MetricReportingConfig(builder.graphiteReporterConfig, builder.jmxReporterConfig);
        this.servletContext = builder.servletContext;
    }

    /**
     * Retrieves the config file used to source the configuration parameters from.
     *
     * @return the config file used to source the configuration parameters from; <tt>null</tt> if no config file was
     * used to instantiate this class.
     */
    public File getConfigFile() {
        return configFile;
    }

    /**
     * Retrieves the naming config. This method is guaranteed to return a non-null value.
     *
     * @return the naming config.
     */
    public NamingConfig getNamingConfig() {
        return namingConfig;
    }

    /**
     * Retrieves the metric collection config. This method is guaranteed to return a non-null value.
     *
     * @return the metric collection config.
     */
    public MetricCollectionConfig getMetricCollectionConfig() {
        return metricCollectionConfig;
    }

    /**
     * Retrieves the metric reporting config. This method is guaranteed to return a non-null value.
     *
     * @return the metric reporting config.
     */
    public MetricReportingConfig getMetricReportingConfig() {
        return metricReportingConfig;
    }

    /**
     * Retrieves the injected servlet context.
     * The context could be null if client doesn't enable webApp metrics and it must be not null when webApp metrics enabled.
     *
     * @return servlet context
     */
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MonitoringCenterConfig{");
        sb.append("namingConfig=").append(namingConfig);
        sb.append(", metricCollectionConfig=").append(metricCollectionConfig);
        sb.append(", metricReportingConfig=").append(metricReportingConfig);
        sb.append(", servletContext=").append(servletContext);
        sb.append('}');
        return sb.toString();
    }

    protected static Builder builder() {
        return new Builder(null);
    }

    protected static Builder builder(File configFile) {
        return new Builder(configFile);
    }

    public static class Builder {
        public static final String DC_ENV_VARIABLE_NAME = "METRICS_DATACENTER";
        public static final String NODE_GROUP_ENV_VARIABLE_NAME = "METRICS_NODE_GROUP";
        public static final String NODE_ID_ENV_VARIABLE_NAME = "METRICS_NODE_ID";

        public static final String NONE = "none";

        private String applicationName;
        private String datacenterName;
        private String nodeGroupName;
        private String nodeId;
        private MetricNamePostfixPolicy metricNamePostfixPolicy;
        private Boolean appendTypeToHealthCheckNames;

        private ServletContext servletContext;

        private boolean enableSystemMetrics;
        private boolean enableTomcatMetrics;
        private boolean enableWebAppMetrics;

        private GraphiteReporterConfig graphiteReporterConfig;
        private JmxReporterConfig jmxReporterConfig;

        private File configFile;

        private Builder(File configFile) {
            this.configFile = configFile;

            // Set defaults
            this.datacenterName = StringUtils.defaultIfBlank(MetricNamingUtil.sanitize(System.getenv(DC_ENV_VARIABLE_NAME)), NONE);
            this.nodeGroupName = StringUtils.defaultIfBlank(MetricNamingUtil.sanitize(System.getenv(NODE_GROUP_ENV_VARIABLE_NAME)), NONE);
            this.nodeId = StringUtils.defaultIfBlank(MetricNamingUtil.sanitize(System.getenv(NODE_ID_ENV_VARIABLE_NAME)), NONE);
            this.metricNamePostfixPolicy = MetricNamePostfixPolicy.ADD_COMPOSITE_TYPES;
            this.appendTypeToHealthCheckNames = false;

            this.enableSystemMetrics = false;
            this.enableTomcatMetrics = false;
            this.enableWebAppMetrics = false;
        }

        /**
         * Sets the application name to be used in the node-specific prefix for metrics. The application name will also
         * be employed in the {@link net.centro.rtb.monitoringcenter.infos.AppInfo}. For instance, "bidder".
         *
         * This is a required field.
         *
         * Note that the passed in value will be sanitized using {@link MetricNamingUtil#sanitize(String)}.
         *
         * @param applicationName application name.
         * @return this builder.
         * @throws IllegalArgumentException if <tt>applicationName</tt> is blank.
         */
        public Builder applicationName(String applicationName) {
            if (StringUtils.isBlank(applicationName)) {
                throw new IllegalArgumentException("applicationName cannot be blank");
            }
            this.applicationName = MetricNamingUtil.sanitize(applicationName);
            return this;
        }

        /**
         * Sets the data center name to be used in the node-specific prefix for metrics. The data center name will also
         * feature in the {@link net.centro.rtb.monitoringcenter.infos.NodeInfo}. For instance, "east".
         * <br><br>
         * If not specified, the value will be retrieved from an environment variable, whose name is held in the
         * {@link #DC_ENV_VARIABLE_NAME} constant. In case the environment variable is not specified, {@link #NONE}
         * will be used as the value.
         *
         * Note that the passed in value will be sanitized using {@link MetricNamingUtil#sanitize(String)}.
         *
         * @param datacenterName data center name.
         * @return this builder.
         */
        public Builder datacenterName(String datacenterName) {
            this.datacenterName = StringUtils.defaultIfBlank(MetricNamingUtil.sanitize(datacenterName), this.datacenterName);
            return this;
        }

        /**
         * Sets the node group name to be used in the node-specific prefix for metrics. The node group name will also
         * feature in the {@link net.centro.rtb.monitoringcenter.infos.NodeInfo}. For instance, "lb3.p2e" or "staging".
         * <br><br>
         * If not specified, the value will be retrieved from an environment variable, whose name is held in the
         * {@link #NODE_GROUP_ENV_VARIABLE_NAME} constant. In case the environment variable is not specified,
         * {@link #NONE} will be used as the value.
         *
         * Note that the passed in value will be sanitized using {@link MetricNamingUtil#sanitize(String)}.
         *
         * @param nodeGroupName node group name.
         * @return this builder.
         */
        public Builder nodeGroupName(String nodeGroupName) {
            this.nodeGroupName = StringUtils.defaultIfBlank(MetricNamingUtil.sanitize(nodeGroupName), this.nodeGroupName);
            return this;
        }

        /**
         * Sets the node ID to be used in the node-specific prefix for metrics. The node ID will also feature in the
         * {@link net.centro.rtb.monitoringcenter.infos.NodeInfo}. For instance, "58" or "node59".
         * <br><br>
         * If not specified, the value will be retrieved from an environment variable, whose name is held in the
         * {@link #NODE_ID_ENV_VARIABLE_NAME} constant. In case the environment variable is not specified {@link #NONE}
         * will be used as the value.
         *
         * Note that the passed in value will be sanitized using {@link MetricNamingUtil#sanitize(String)}.
         *
         * @param nodeId ID of the node.
         * @return this builder.
         */
        public Builder nodeId(String nodeId) {
            this.nodeId = StringUtils.defaultIfBlank(MetricNamingUtil.sanitize(nodeId), this.nodeId);
            return this;
        }

        /**
         * Sets the policy for appending metric types to metric names. By default, the policy will be set to
         * {@link MetricNamePostfixPolicy#ADD_COMPOSITE_TYPES}, which will enforce metric types to be appended to names
         * of composite metrics (i.e., timers, histograms, and meters).
         * <br><br>
         * If the metric name already ends with the type string, it will be left intact--e.g., a counter with name
         * "errorCounter" will remain "errorCounter".
         *
         * @param metricNamePostfixPolicy the postfix policy to use for metric names.
         * @return this builder.
         */
        public Builder metricNamePostfixPolicy(MetricNamePostfixPolicy metricNamePostfixPolicy) {
            if (metricNamePostfixPolicy == null) {
                metricNamePostfixPolicy = MetricNamePostfixPolicy.ADD_COMPOSITE_TYPES;
            }
            this.metricNamePostfixPolicy = metricNamePostfixPolicy;
            return this;
        }

        /**
         * Indicates whether "HealthCheck" should automatically be appended to health check names or not. By default,
         * this feature is disabled. For instance, for a health check named "factual", the resulting name would be
         * "factualHealthCheck". If the health check name already ends with the type string, it will be left intact.
         *
         * @param appendTypeToHealthCheckNames whether type should be appended to health check names or not.
         * @return this builder.
         */
        public Builder appendTypeToHealthCheckNames(boolean appendTypeToHealthCheckNames) {
            this.appendTypeToHealthCheckNames = appendTypeToHealthCheckNames;
            return this;
        }

        /**
         * The servlet context is configured by the application which wants to instrument the http filter.
         * The context will not be utilized only when webApp metrics are enabled. Current implementation only supports
         * {@Link com.codahale.metrics.servlet.InstrumentedFilter} and hence the web application itself should
         * configure the filter in web.xml separately.
         *
         * @param servletContext the servlet context should be utilized for webApp metrics
         * @return this builder
         */
        public Builder servletContext(ServletContext servletContext) {
            this.servletContext = servletContext;
            return this;
        }

        /**
         * Indicates whether the collection of system metrics should be enabled or not. System metrics include JVM- and
         * OS-level data points. By default, system metrics will not be collected.
         *
         * @param enableSystemMetrics whether the collection of system metrics should be enabled or not.
         * @return this builder.
         */
        public Builder enableSystemMetrics(boolean enableSystemMetrics) {
            this.enableSystemMetrics = enableSystemMetrics;
            return this;
        }

        /**
         * Indicates whether Tomcat metrics should be collected or not. Such metrics will only be collected if Tomcat's
         * JMX beans are available. By default, Tomcat metrics will not be collected.
         *
         * @param enableTomcatMetrics whether the collection of Tomcat metrics should be enabled or not.
         * @return this builder.
         */
        public Builder enableTomcatMetrics(boolean enableTomcatMetrics) {
            this.enableTomcatMetrics = enableTomcatMetrics;
            return this;
        }

        public Builder enableWebAppMetrics(boolean enableWebAppMetrics) {
            this.enableWebAppMetrics = enableWebAppMetrics;
            return this;
        }

        /**
         * Sets the configuration for the GraphiteReporter. By default, the GraphiteReporter will not be configured.
         *
         * @param graphiteReporterConfig a Graphite reporter configuration.
         * @return this builder.
         */
        public Builder graphiteReporterConfig(GraphiteReporterConfig graphiteReporterConfig) {
            this.graphiteReporterConfig = graphiteReporterConfig;
            return this;
        }

        /**
         * Sets the configuration for the JmxReporter. By default, the JmxReporter will not be configured.
         *
         * @param jmxReporterConfig a JMX reporter configuration.
         * @return this builder.
         */
        public Builder jmxReporterConfig(JmxReporterConfig jmxReporterConfig) {
            this.jmxReporterConfig = jmxReporterConfig;
            return this;
        }

        public MonitoringCenterConfig build() {
            if (applicationName == null) {
                throw new IllegalStateException("applicationName cannot be blank");
            }
            return new MonitoringCenterConfig(this);
        }
    }
}

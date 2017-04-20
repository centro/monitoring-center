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

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * This class encompasses the configuration parameters for the Graphite push reporter. By default, this reporter is not
 * configured--that is, it not enabled in the MonitoringCenter, unless explicitly indicated in the config file or
 * programmatically. All configuration parameters in this class can be reloaded dynamically, if the reloading is
 * enabled (i.e., a config file was used).
 */
public class GraphiteReporterConfig {
    private boolean enableReporter;
    private HostAndPort address;
    private long reportingIntervalInSeconds;
    private boolean enableBatching;
    private boolean reportOnShutdown;
    private Set<String> startsWithFilters;
    private Set<String> blockedStartsWithFilters;

    private GraphiteReporterConfig(Builder builder) {
        this.enableReporter = builder.enableReporter;
        this.address = builder.address;
        this.reportingIntervalInSeconds = builder.reportingIntervalInSeconds;
        this.enableBatching = builder.enableBatching;
        this.reportOnShutdown = builder.reportOnShutdown;
        this.startsWithFilters = Collections.unmodifiableSet(builder.startsWithFilters);
        this.blockedStartsWithFilters = Collections.unmodifiableSet(builder.blockedStartsWithFilters);
    }

    /**
     * Indicates whether the reporter should be enabled or not. By default, the reporter is enabled, if configured.
     *
     * @return whether the reporter should be enabled or not.
     */
    public boolean isEnableReporter() {
        return enableReporter;
    }

    /**
     * Retrieves the address (host and port) of the Graphite instance to push metrics to. This field is guaranteed to be
     * non-null.
     *
     * @return the address (host and port) of the Graphite instance to push metrics to.
     */
    public HostAndPort getAddress() {
        return address;
    }

    /**
     * Retrieves the reporting interval in seconds. By default, this interval is
     * {@link Builder#DEFAULT_REPORTING_INTERVAL_IN_SECONDS}.
     *
     * @return the reporting interval in seconds.
     */
    public long getReportingIntervalInSeconds() {
        return reportingIntervalInSeconds;
    }

    /**
     * Indicates whether the Graphite reporter should employ batching or not. By default, batching is enabled.
     *
     * @return whether the Graphite reporter should employ batching or not.
     */
    public boolean isEnableBatching() {
        return enableBatching;
    }

    /**
     * Indicates whether metrics should be published to Graphite prior to shutting down the reporter or not.
     * By default, this feature is enabled. One may want to disable this feature to avoid inconsistencies when
     * Carbon aggregations are enabled on the Graphite side.
     *
     * @return whether metrics should be reported prior to shutting down or not.
     */
    public boolean isReportOnShutdown() {
        return reportOnShutdown;
    }

    /**
     * Retrieves the filters to apply to metrics in order to decide which metrics should be reported to Graphite. If no
     * filters are specified, an empty set is returned.
     *
     * @return the whitelist filters to apply to metrics.
     */
    public Set<String> getStartsWithFilters() {
        return startsWithFilters;
    }

    /**
     * Retrieves the filters to apply to metrics in order to decide which metrics should not be reported to Graphite.
     * If no filters are specified, an empty set is returned.
     *
     * @return the blacklist filters to apply to metrics.
     */
    public Set<String> getBlockedStartsWithFilters() {
        return blockedStartsWithFilters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GraphiteReporterConfig that = (GraphiteReporterConfig) o;

        if (enableReporter != that.enableReporter) return false;
        if (reportingIntervalInSeconds != that.reportingIntervalInSeconds) return false;
        if (enableBatching != that.enableBatching) return false;
        if (reportOnShutdown != that.reportOnShutdown) return false;
        if (address != null ? !address.equals(that.address) : that.address != null) return false;
        if (startsWithFilters != null ? !startsWithFilters.equals(that.startsWithFilters) : that.startsWithFilters != null)
            return false;
        return blockedStartsWithFilters != null ? blockedStartsWithFilters.equals(that.blockedStartsWithFilters) : that.blockedStartsWithFilters == null;
    }

    @Override
    public int hashCode() {
        int result = (enableReporter ? 1 : 0);
        result = 31 * result + (address != null ? address.hashCode() : 0);
        result = 31 * result + (int) (reportingIntervalInSeconds ^ (reportingIntervalInSeconds >>> 32));
        result = 31 * result + (enableBatching ? 1 : 0);
        result = 31 * result + (reportOnShutdown ? 1 : 0);
        result = 31 * result + (startsWithFilters != null ? startsWithFilters.hashCode() : 0);
        result = 31 * result + (blockedStartsWithFilters != null ? blockedStartsWithFilters.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GraphiteReporterConfig{");
        sb.append("enableReporter=").append(enableReporter);
        sb.append(", address=").append(address);
        sb.append(", reportingIntervalInSeconds=").append(reportingIntervalInSeconds);
        sb.append(", enableBatching=").append(enableBatching);
        sb.append(", reportOnShutdown=").append(reportOnShutdown);
        sb.append(", startsWithFilters=").append(startsWithFilters);
        sb.append(", blockedStartsWithFilters=").append(blockedStartsWithFilters);
        sb.append('}');
        return sb.toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        public static final long DEFAULT_REPORTING_INTERVAL_IN_SECONDS = TimeUnit.MINUTES.toSeconds(1);

        private boolean enableReporter;
        private HostAndPort address;
        private long reportingIntervalInSeconds;
        private boolean enableBatching;
        private boolean reportOnShutdown;
        private Set<String> startsWithFilters;
        private Set<String> blockedStartsWithFilters;

        public Builder() {
            this.enableReporter = true;
            this.reportingIntervalInSeconds = DEFAULT_REPORTING_INTERVAL_IN_SECONDS;
            this.enableBatching = true;
            this.reportOnShutdown = true;
            this.startsWithFilters = Collections.emptySet();
            this.blockedStartsWithFilters = Collections.emptySet();
        }

        /**
         * Indicates whether the reporter should be enabled or not. By default, the reporter will be enabled.
         *
         * @param enableReporter indicates whether the reporter should be enabled or not.
         * @return this builder.
         */
        public Builder enableReporter(boolean enableReporter) {
            this.enableReporter = enableReporter;
            return this;
        }

        /**
         * Sets the host and port for connecting to the Graphite instance.
         *
         * This is a required field.
         *
         * @param host a hostname or IP address.
         * @param port a port number.
         * @return this builder.
         * @throws IllegalArgumentException if <tt>host</tt> is blank.
         * @throws IllegalArgumentException if <tt>port</tt> is outside of the valid range of [0, 65535].
         */
        public Builder address(String host, int port) {
            this.address = HostAndPort.of(host, port);
            return this;
        }

        /**
         * Sets the reporting interval. By default, the reporting interval is 1 minute.
         *
         * @param period an interval at which to push data to Graphite.
         * @param timeUnit the unit for <tt>period</tt>.
         * @return this builder.
         * @throws IllegalArgumentException if <tt>period</tt> is less than or equal to 0.
         * @throws NullPointerException if <tt>timeUnit</tt> is <tt>null</tt>.
         */
        public Builder reportingInterval(long period, TimeUnit timeUnit) {
            if (period <= 0) {
                throw new IllegalArgumentException("period must be positive");
            }

            if (timeUnit == null) {
                throw new IllegalArgumentException("timeUnit cannot be null");
            }

            this.reportingIntervalInSeconds = timeUnit.toSeconds(period);
            return this;
        }

        /**
         * Indicates whether batching should be enabled or not. By default, batching is enabled.
         *
         * @param enableBatching indicates whether batching should be enabled or not.
         * @return this builder.
         */
        public Builder enableBatching(boolean enableBatching) {
            this.enableBatching = enableBatching;
            return this;
        }

        /**
         * Indicates whether metrics should be published to Graphite prior to shutting down the reporter or not.
         * By default, this feature is enabled. One may want to disable this feature to avoid inconsistencies when
         * Carbon aggregations are enabled on the Graphite side.
         *
         * @param reportOnShutdown indicates whether metrics should be reported prior to shutting down or not.
         * @return this builder.
         */
        public Builder reportOnShutdown(boolean reportOnShutdown) {
            this.reportOnShutdown = reportOnShutdown;
            return this;
        }

        /**
         * Sets the whitelist filters to be applied to metric names. These filters can include the
         * {@link net.centro.rtb.monitoringcenter.util.MetricNamingUtil#SEPARATOR} and multiple wildcards denoted as
         * <tt>*</tt>.
         *
         * @param startsWithFilters whitelist filters to be applied to metric names.
         * @return this builder.
         */
        public Builder startsWithFilters(Set<String> startsWithFilters) {
            if (startsWithFilters == null) {
                this.startsWithFilters = Collections.emptySet();
            } else {
                this.startsWithFilters = startsWithFilters;
            }
            return this;
        }

        /**
         * Sets the blacklist filters to be applied to metric names. These filters can include the
         * {@link net.centro.rtb.monitoringcenter.util.MetricNamingUtil#SEPARATOR} and multiple wildcards denoted as
         * <tt>*</tt>.
         *
         * @param blockedStartsWithFilters blacklist filters to be applied to metric names.
         * @return this builder.
         */
        public Builder blockedStartsWithFilters(Set<String> blockedStartsWithFilters) {
            if (blockedStartsWithFilters == null) {
                this.blockedStartsWithFilters = Collections.emptySet();
            } else {
                this.blockedStartsWithFilters = blockedStartsWithFilters;
            }
            return this;
        }

        public GraphiteReporterConfig build() {
            if (address == null) {
                throw new IllegalStateException("address must be set");
            }
            return new GraphiteReporterConfig(this);
        }
    }
}

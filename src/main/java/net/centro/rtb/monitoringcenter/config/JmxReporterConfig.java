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

/**
 * This class encompasses the configuration parameters for the JMX reporter. By default, this reporter is not
 * configured--that is, it not enabled in the MonitoringCenter, unless explicitly indicated in the config file or
 * programmatically. All configuration parameters in this class can be reloaded dynamically, if the reloading is
 * enabled (i.e., a config file was used).
 */
public class JmxReporterConfig {
    private boolean enableReporter;
    private Set<String> startsWithFilters;
    private Set<String> blockedStartsWithFilters;

    private JmxReporterConfig(Builder builder) {
        this.enableReporter = builder.enableReporter;
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
     * Retrieves the filters to apply to metrics in order to decide which metrics should be exposed via JMX. If no
     * filters are specified, an empty set is returned.
     *
     * @return the whitelist filters to apply to metrics.
     */
    public Set<String> getStartsWithFilters() {
        return startsWithFilters;
    }

    /**
     * Retrieves the filters to apply to metrics in order to decide which metrics should not be exposed via JMX.
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

        JmxReporterConfig that = (JmxReporterConfig) o;

        if (enableReporter != that.enableReporter) return false;
        if (startsWithFilters != null ? !startsWithFilters.equals(that.startsWithFilters) : that.startsWithFilters != null)
            return false;
        return blockedStartsWithFilters != null ? blockedStartsWithFilters.equals(that.blockedStartsWithFilters) : that.blockedStartsWithFilters == null;
    }

    @Override
    public int hashCode() {
        int result = (enableReporter ? 1 : 0);
        result = 31 * result + (startsWithFilters != null ? startsWithFilters.hashCode() : 0);
        result = 31 * result + (blockedStartsWithFilters != null ? blockedStartsWithFilters.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("JmxReporterConfig{");
        sb.append("enableReporter=").append(enableReporter);
        sb.append(", startsWithFilters=").append(startsWithFilters);
        sb.append(", blockedStartsWithFilters=").append(blockedStartsWithFilters);
        sb.append('}');
        return sb.toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean enableReporter;
        private Set<String> startsWithFilters;
        private Set<String> blockedStartsWithFilters;

        public Builder() {
            this.enableReporter = true;
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

        public JmxReporterConfig build() {
            return new JmxReporterConfig(this);
        }
    }
}

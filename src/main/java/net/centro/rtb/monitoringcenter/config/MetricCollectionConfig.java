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
 * This class encompasses the configuration parameters for metric collection. All configuration
 * parameters in this class can be reloaded dynamically, if reloading is enabled (i.e., a config file was used).
 */
public class MetricCollectionConfig {
    private boolean enableSystemMetrics;
    private boolean enableTomcatMetrics;
    private boolean enableWebAppMetrics;

    MetricCollectionConfig(boolean enableSystemMetrics, boolean enableTomcatMetrics, boolean enableWebAppMetrics) {
        this.enableSystemMetrics = enableSystemMetrics;
        this.enableTomcatMetrics = enableTomcatMetrics;
        this.enableWebAppMetrics = enableWebAppMetrics;
    }

    /**
     * Indicates whether the collection of system metrics should be enabled or not. System metrics include operating
     * system and JVM statistics. By default, system metrics are disabled.
     *
     * @return whether the collection of system metrics should be enabled or not.
     */
    public boolean isEnableSystemMetrics() {
        return enableSystemMetrics;
    }

    /**
     * Indicates whether the collection of Tomcat metrics should be enabled or not. Tomcat metrics, even when enabled,
     * will, obviously, be collected only when Tomcat's JMX beans are available. By default, Tomcat metrics are
     * disabled.
     *
     * @return whether the collection of Tomcat metrics should be enabled or not.
     */
    public boolean isEnableTomcatMetrics() {
        return enableTomcatMetrics;
    }

    public boolean isEnableWebAppMetrics() {
        return enableWebAppMetrics;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MetricCollectionConfig that = (MetricCollectionConfig) o;

        if (enableSystemMetrics != that.enableSystemMetrics) return false;
        if (enableWebAppMetrics != that.enableWebAppMetrics) return false;
        return enableTomcatMetrics == that.enableTomcatMetrics;
    }

    @Override
    public int hashCode() {
        int result = (enableSystemMetrics ? 1 : 0);
        result = 31 * result + (enableTomcatMetrics ? 1 : 0);
        result = 31 * result + (enableWebAppMetrics ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MetricCollectionConfig{");
        sb.append("enableSystemMetrics=").append(enableSystemMetrics);
        sb.append(", enableTomcatMetrics=").append(enableTomcatMetrics);
        sb.append(", enableWebAppMetrics=").append(enableWebAppMetrics);
        sb.append('}');
        return sb.toString();
    }
}

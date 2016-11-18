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
 * A holder for reporter configurations and global metric reporting configuration parameters. All configuration
 * parameters in this class can be reloaded dynamically, if reloading is enabled (i.e., a config file was used).
 */
public class MetricReportingConfig {
    private GraphiteReporterConfig graphiteReporterConfig;

    MetricReportingConfig(GraphiteReporterConfig graphiteReporterConfig) {
        this.graphiteReporterConfig = graphiteReporterConfig;
    }

    /**
     * Retrieves the graphite reporter configuration.
     *
     * @return the graphite reporter configuration.
     */
    public GraphiteReporterConfig getGraphiteReporterConfig() {
        return graphiteReporterConfig;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MetricReportingConfig{");
        sb.append("graphiteReporterConfig=").append(graphiteReporterConfig);
        sb.append('}');
        return sb.toString();
    }
}

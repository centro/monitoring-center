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

package net.centro.rtb.monitoringcenter.config.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import net.centro.rtb.monitoringcenter.config.HostAndPort;

import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphiteReporterConfigDto {
    private Boolean enableReporter;
    private HostAndPort address;
    private Long reportingIntervalInSeconds;
    private Boolean enableBatching;
    private Boolean reportOnShutdown;
    private Set<String> startsWithFilters;
    private Set<String> blockedStartsWithFilters;

    public Boolean getEnableReporter() {
        return enableReporter;
    }

    public void setEnableReporter(Boolean enableReporter) {
        this.enableReporter = enableReporter;
    }

    public HostAndPort getAddress() {
        return address;
    }

    public void setAddress(HostAndPort address) {
        this.address = address;
    }

    public Long getReportingIntervalInSeconds() {
        return reportingIntervalInSeconds;
    }

    public void setReportingIntervalInSeconds(Long reportingIntervalInSeconds) {
        this.reportingIntervalInSeconds = reportingIntervalInSeconds;
    }

    public Boolean getEnableBatching() {
        return enableBatching;
    }

    public void setEnableBatching(Boolean enableBatching) {
        this.enableBatching = enableBatching;
    }

    public Boolean getReportOnShutdown() {
        return reportOnShutdown;
    }

    public void setReportOnShutdown(Boolean reportOnShutdown) {
        this.reportOnShutdown = reportOnShutdown;
    }

    public Set<String> getStartsWithFilters() {
        return startsWithFilters;
    }

    public void setStartsWithFilters(Set<String> startsWithFilters) {
        this.startsWithFilters = startsWithFilters;
    }

    public Set<String> getBlockedStartsWithFilters() {
        return blockedStartsWithFilters;
    }

    public void setBlockedStartsWithFilters(Set<String> blockedStartsWithFilters) {
        this.blockedStartsWithFilters = blockedStartsWithFilters;
    }
}

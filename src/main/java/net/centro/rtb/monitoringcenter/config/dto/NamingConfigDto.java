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

@JsonIgnoreProperties(ignoreUnknown = true)
public class NamingConfigDto {
    private String applicationName;
    private String datacenterName;
    private String nodeGroupName;
    private String nodeId;

    private MetricNamePostfixPolicyDto metricNamePostfixPolicy;
    private Boolean appendTypeToHealthCheckNames;

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getDatacenterName() {
        return datacenterName;
    }

    public void setDatacenterName(String datacenterName) {
        this.datacenterName = datacenterName;
    }

    public String getNodeGroupName() {
        return nodeGroupName;
    }

    public void setNodeGroupName(String nodeGroupName) {
        this.nodeGroupName = nodeGroupName;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public MetricNamePostfixPolicyDto getMetricNamePostfixPolicy() {
        return metricNamePostfixPolicy;
    }

    public void setMetricNamePostfixPolicy(MetricNamePostfixPolicyDto metricNamePostfixPolicy) {
        this.metricNamePostfixPolicy = metricNamePostfixPolicy;
    }

    public Boolean getAppendTypeToHealthCheckNames() {
        return appendTypeToHealthCheckNames;
    }

    public void setAppendTypeToHealthCheckNames(Boolean appendTypeToHealthCheckNames) {
        this.appendTypeToHealthCheckNames = appendTypeToHealthCheckNames;
    }
}

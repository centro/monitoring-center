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

package net.centro.rtb.monitoringcenter.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import net.centro.rtb.monitoringcenter.config.GraphiteReporterConfig;
import net.centro.rtb.monitoringcenter.config.HostAndPort;
import net.centro.rtb.monitoringcenter.config.MetricCollectionConfig;
import net.centro.rtb.monitoringcenter.config.MetricNamePostfixPolicy;
import net.centro.rtb.monitoringcenter.config.MetricReportingConfig;
import net.centro.rtb.monitoringcenter.config.MonitoringCenterConfig;
import net.centro.rtb.monitoringcenter.config.NamingConfig;
import net.centro.rtb.monitoringcenter.config.dto.GraphiteReporterConfigDto;
import net.centro.rtb.monitoringcenter.config.dto.MetricCollectionConfigDto;
import net.centro.rtb.monitoringcenter.config.dto.MetricNamePostfixPolicyDto;
import net.centro.rtb.monitoringcenter.config.dto.MetricReportingConfigDto;
import net.centro.rtb.monitoringcenter.config.dto.MonitoringCenterConfigDto;
import net.centro.rtb.monitoringcenter.config.dto.NamingConfigDto;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ConfigFileUtil {
    private static final String EFFECTIVE_CONFIG_FILE_NAME_TEMPLATE = "monitoringCenter-%s-current.yaml";

    private static ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory())
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private ConfigFileUtil() {
    }

    /**
     * Creates a file containing the effective config for the MonitoringCenter. The effective config contains the
     * properties that were determined during the initialization of the MonitoringCenter; these properties include both
     * the ones specified programmatically and via a config file. Persisting the effective config is necessary to enable
     * dynamic changes (i.e., reload) to the config.
     *
     * @param config a MonitoringCenter config to serialize into a file.
     */
    public static void createEffectiveConfigFile(MonitoringCenterConfig config) {
        // Convert to DTO
        MonitoringCenterConfigDto configDto = new MonitoringCenterConfigDto();

        NamingConfig namingConfig = config.getNamingConfig();
        if (namingConfig != null) {
            NamingConfigDto namingConfigDto = new NamingConfigDto();
            namingConfigDto.setApplicationName(namingConfig.getApplicationName());
            namingConfigDto.setDatacenterName(namingConfig.getDatacenterName());
            namingConfigDto.setNodeGroupName(namingConfig.getNodeGroupName());
            namingConfigDto.setNodeId(namingConfig.getNodeId());
            namingConfigDto.setMetricNamePostfixPolicy(convertMetricNamePostfixPolicyToDto(namingConfig.getMetricNamePostfixPolicy()));
            namingConfigDto.setAppendTypeToHealthCheckNames(namingConfig.isAppendTypeToHealthCheckNames());
            configDto.setNamingConfig(namingConfigDto);
        }

        MetricCollectionConfig metricCollectionConfig = config.getMetricCollectionConfig();
        if (metricCollectionConfig != null) {
            MetricCollectionConfigDto metricCollectionConfigDto = new MetricCollectionConfigDto();
            metricCollectionConfigDto.setEnableSystemMetrics(metricCollectionConfig.isEnableSystemMetrics());
            metricCollectionConfigDto.setEnableTomcatMetrics(metricCollectionConfig.isEnableTomcatMetrics());
            configDto.setMetricCollectionConfig(metricCollectionConfigDto);
        }

        MetricReportingConfig metricReportingConfig = config.getMetricReportingConfig();
        if (metricReportingConfig != null) {
            MetricReportingConfigDto metricReportingConfigDto = new MetricReportingConfigDto();

            GraphiteReporterConfig graphiteReporterConfig = metricReportingConfig.getGraphiteReporterConfig();
            if (graphiteReporterConfig != null) {
                GraphiteReporterConfigDto graphiteReporterConfigDto = new GraphiteReporterConfigDto();
                graphiteReporterConfigDto.setEnableReporter(graphiteReporterConfig.isEnableReporter());
                graphiteReporterConfigDto.setAddress(graphiteReporterConfig.getAddress());
                graphiteReporterConfigDto.setEnableBatching(graphiteReporterConfig.isEnableBatching());
                graphiteReporterConfigDto.setReportOnShutdown(graphiteReporterConfig.isReportOnShutdown());
                graphiteReporterConfigDto.setReportingIntervalInSeconds(graphiteReporterConfig.getReportingIntervalInSeconds());
                graphiteReporterConfigDto.setStartsWithFilters(graphiteReporterConfig.getStartsWithFilters());
                graphiteReporterConfigDto.setBlockedStartsWithFilters(graphiteReporterConfig.getBlockedStartsWithFilters());
                metricReportingConfigDto.setGraphiteReporterConfig(graphiteReporterConfigDto);
            }

            configDto.setMetricReportingConfig(metricReportingConfigDto);
        }

        // Come up with a filename
        String effectiveConfigFileFullPath = getEffectiveConfigFileFullPath(config);

        try {
            objectMapper.writeValue(new File(effectiveConfigFileFullPath), configDto);
        } catch (IOException e) {
            throw new ConfigException("Could not create the effective config file: " + effectiveConfigFileFullPath, e);
        }
    }

    public static String getEffectiveConfigFileFullPath(MonitoringCenterConfig config) {
        String directory = config.getConfigFile().getParent();
        String fileName = String.format(EFFECTIVE_CONFIG_FILE_NAME_TEMPLATE, config.getNamingConfig() != null ? config.getNamingConfig().getApplicationName() : null);
        return directory + "/" + fileName;
    }

    public static void fillConfigBuilderFromFile(File configFile, MonitoringCenterConfig.Builder configBuilder) {
        MonitoringCenterConfigDto configFromFile = null;
        if (configFile != null && configFile.exists()) {
            try {
                configFromFile = objectMapper.readValue(configFile, MonitoringCenterConfigDto.class);
            } catch (IOException e) {
                throw new ConfigException("Could not process config file: " + configFile.toString(), e);
            }
        }

        if (configFromFile != null) {
            NamingConfigDto namingConfigFromFile = configFromFile.getNamingConfig();
            if (namingConfigFromFile != null) {
                if (namingConfigFromFile.getApplicationName() != null) {
                    configBuilder.applicationName(namingConfigFromFile.getApplicationName());
                }

                configBuilder.datacenterName(namingConfigFromFile.getDatacenterName());
                configBuilder.nodeGroupName(namingConfigFromFile.getNodeGroupName());
                configBuilder.nodeId(namingConfigFromFile.getNodeId());

                if (namingConfigFromFile.getMetricNamePostfixPolicy() != null && namingConfigFromFile.getMetricNamePostfixPolicy() != MetricNamePostfixPolicyDto.UNKNOWN) {
                    configBuilder.metricNamePostfixPolicy(convertMetricNamePostfixPolicyToEntity(namingConfigFromFile.getMetricNamePostfixPolicy()));
                }

                if (namingConfigFromFile.getAppendTypeToHealthCheckNames() != null) {
                    configBuilder.appendTypeToHealthCheckNames(namingConfigFromFile.getAppendTypeToHealthCheckNames());
                }
            }

            MetricCollectionConfigDto collectionConfigFromFile = configFromFile.getMetricCollectionConfig();
            if (collectionConfigFromFile != null) {
                if (collectionConfigFromFile.getEnableSystemMetrics() != null) {
                    configBuilder.enableSystemMetrics(collectionConfigFromFile.getEnableSystemMetrics());
                }
                if (collectionConfigFromFile.getEnableTomcatMetrics() != null) {
                    configBuilder.enableTomcatMetrics(collectionConfigFromFile.getEnableTomcatMetrics());
                }
            }

            MetricReportingConfigDto reportingConfigFromFile = configFromFile.getMetricReportingConfig();
            if (reportingConfigFromFile != null) {
                GraphiteReporterConfigDto graphiteReporterConfigFromFile = reportingConfigFromFile.getGraphiteReporterConfig();
                if (graphiteReporterConfigFromFile != null) {
                    GraphiteReporterConfig.Builder graphiteReporterConfigBuilder = GraphiteReporterConfig.builder();
                    if (graphiteReporterConfigFromFile.getEnableReporter() != null) {
                        graphiteReporterConfigBuilder.enableReporter(graphiteReporterConfigFromFile.getEnableReporter());
                    }
                    if (graphiteReporterConfigFromFile.getEnableBatching() != null) {
                        graphiteReporterConfigBuilder.enableBatching(graphiteReporterConfigFromFile.getEnableBatching());
                    }
                    if (graphiteReporterConfigFromFile.getReportOnShutdown() != null) {
                        graphiteReporterConfigBuilder.reportOnShutdown(graphiteReporterConfigFromFile.getReportOnShutdown());
                    }
                    if (graphiteReporterConfigFromFile.getReportingIntervalInSeconds() != null) {
                        graphiteReporterConfigBuilder.reportingInterval(graphiteReporterConfigFromFile.getReportingIntervalInSeconds(), TimeUnit.SECONDS);
                    }
                    HostAndPort address = graphiteReporterConfigFromFile.getAddress();
                    if (address != null) {
                        graphiteReporterConfigBuilder.address(address.getHost(), address.getPort());
                    }
                    if (graphiteReporterConfigFromFile.getStartsWithFilters() != null) {
                        graphiteReporterConfigBuilder.startsWithFilters(graphiteReporterConfigFromFile.getStartsWithFilters());
                    }
                    if (graphiteReporterConfigFromFile.getBlockedStartsWithFilters() != null) {
                        graphiteReporterConfigBuilder.blockedStartsWithFilters(graphiteReporterConfigFromFile.getBlockedStartsWithFilters());
                    }
                    configBuilder.graphiteReporterConfig(graphiteReporterConfigBuilder.build());
                }
            }
        }
    }

    private static MetricNamePostfixPolicyDto convertMetricNamePostfixPolicyToDto(MetricNamePostfixPolicy entity) {
        if (entity == MetricNamePostfixPolicy.ADD_ALL_TYPES) {
            return MetricNamePostfixPolicyDto.ADD_ALL_TYPES;
        } else if (entity == MetricNamePostfixPolicy.ADD_COMPOSITE_TYPES) {
            return MetricNamePostfixPolicyDto.ADD_COMPOSITE_TYPES;
        } else if (entity == MetricNamePostfixPolicy.OFF) {
            return MetricNamePostfixPolicyDto.OFF;
        } else {
            return MetricNamePostfixPolicyDto.UNKNOWN;
        }
    }

    private static MetricNamePostfixPolicy convertMetricNamePostfixPolicyToEntity(MetricNamePostfixPolicyDto dto) {
        if (dto == MetricNamePostfixPolicyDto.ADD_ALL_TYPES) {
            return MetricNamePostfixPolicy.ADD_ALL_TYPES;
        } else if (dto == MetricNamePostfixPolicyDto.ADD_COMPOSITE_TYPES) {
            return MetricNamePostfixPolicy.ADD_COMPOSITE_TYPES;
        } else if (dto == MetricNamePostfixPolicyDto.OFF) {
            return MetricNamePostfixPolicy.OFF;
        } else {
            return null;
        }
    }
}

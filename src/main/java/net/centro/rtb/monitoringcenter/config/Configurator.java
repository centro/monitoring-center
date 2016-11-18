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

import com.google.common.base.Preconditions;
import net.centro.rtb.monitoringcenter.util.ConfigException;
import net.centro.rtb.monitoringcenter.util.ConfigFileUtil;

import java.io.File;
import java.net.URL;

/**
 * A bootstrapping class, allowing for convenient configuration of
 * {@link net.centro.rtb.monitoringcenter.MonitoringCenter}. This class provides an ability to configure
 * MonitoringCenter using a provided config file, a default config file, or indicate that no config file is to be used.
 * <br><br>
 * In all cases, the clients will be able to override any config file parameters programmatically.
 */
public class Configurator {
    public static final String DEFAULT_CONFIG_FILE_NAME = "monitoringCenter.yaml";

    /**
     * Obtains a MonitoringCenterConfig builder, pre-filled with a given config file in the YAML format. The client
     * can override any config file property programmatically via a builder's method. To finalize the configuration,
     * the {@link MonitoringCenterConfig.Builder#build()} method should be called.
     *
     * @param configFile a config file in the YAML format.
     * @return a MonitoringCenterConfig builder pre-filled with the properties from the config file.
     * @throws NullPointerException if config file is null.
     * @throws IllegalArgumentException if config file does not exist.
     */
    public static MonitoringCenterConfig.Builder configFile(File configFile) {
        Preconditions.checkNotNull(configFile, "configFile cannot be null");
        Preconditions.checkArgument(configFile.exists(), "configFile " + configFile.toString() + " does not exist");

        MonitoringCenterConfig.Builder configBuilder = MonitoringCenterConfig.builder(configFile);
        ConfigFileUtil.fillConfigBuilderFromFile(configFile, configBuilder);
        return configBuilder;
    }

    /**
     * Obtains a MonitoringCenterConfig builder with default parameters. This method should be used when exclusively
     * programmatic configuration is desired. When no config file is provided, the MonitoringCenter's configuration
     * cannot be changed dynamically.
     *
     * To finalize the configuration, the {@link MonitoringCenterConfig.Builder#build()} method should be called.
     *
     * @return a MonitoringCenterConfig builder with default parameters.
     */
    public static MonitoringCenterConfig.Builder noConfigFile() {
        return MonitoringCenterConfig.builder();
    }

    /**
     * Obtains a MonitoringCenterConfig builder, pre-filled with the default config file in the YAML format. The default
     * config file should be named {@link #DEFAULT_CONFIG_FILE_NAME} and it will be looked up on the classpath.
     * <br><br>
     * The client can override any config file property programmatically via a builder's method. To finalize the
     * configuration, the {@link MonitoringCenterConfig.Builder#build()} method should be called.
     *
     * @return a MonitoringCenterConfig builder pre-filled with the properties from the default config file.
     * @throws ConfigException if config file does not exist.
     */
    public static MonitoringCenterConfig.Builder defaultConfigFile() {
        URL configFileResource = Configurator.class.getClassLoader().getResource(DEFAULT_CONFIG_FILE_NAME);

        if (configFileResource == null) {
            throw new ConfigException("Default config file " + DEFAULT_CONFIG_FILE_NAME + " was not found on the classpath");
        }

        File defaultConfigFile = new File(configFileResource.getFile());

        return configFile(defaultConfigFile);
    }
}

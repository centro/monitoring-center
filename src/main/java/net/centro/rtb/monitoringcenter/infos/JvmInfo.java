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

package net.centro.rtb.monitoringcenter.infos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.SystemUtils;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * This class encompasses the information about the Java Virtual Machine (JVM) in which this application is running.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JvmInfo {
    private String specVersion;
    private String classVersion;
    private String jreVersion;
    private String jreVendor;
    private String vmName;
    private String vmVendor;
    private String vmVersion;

    private List<String> inputArguments;
    private Date startedTimestamp;

    private String defaultTimeZone;
    private String defaultCharset;

    private JvmInfo() {
    }

    public static JvmInfo create() {
        JvmInfo jvmInfo = new JvmInfo();

        jvmInfo.specVersion = SystemUtils.JAVA_SPECIFICATION_VERSION;
        jvmInfo.classVersion = SystemUtils.JAVA_CLASS_VERSION;
        jvmInfo.jreVersion = SystemUtils.JAVA_VERSION;
        jvmInfo.jreVendor = SystemUtils.JAVA_VENDOR;
        jvmInfo.vmName = SystemUtils.JAVA_VM_NAME;
        jvmInfo.vmVendor = SystemUtils.JAVA_VM_VENDOR;
        jvmInfo.vmVersion = SystemUtils.JAVA_VM_VERSION;

        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        jvmInfo.inputArguments = new ArrayList<>(runtimeMXBean.getInputArguments());
        jvmInfo.startedTimestamp = new Date(runtimeMXBean.getStartTime());

        jvmInfo.defaultTimeZone = TimeZone.getDefault().getID();
        jvmInfo.defaultCharset = Charset.defaultCharset().displayName();

        return jvmInfo;
    }

    /**
     * Retrieves the version of the Java language spec.
     *
     * @see RuntimeMXBean#getSpecVersion()
     * @return the version of the Java language spec.
     */
    public String getSpecVersion() {
        return specVersion;
    }

    /**
     * Retrieves the version number of the Java class format.
     *
     * @return the version number of the Java class format.
     */
    public String getClassVersion() {
        return classVersion;
    }

    /**
     * Retrieves the version of the Java Runtime Environment (JRE).
     *
     * @return the version of the Java Runtime Environment (JRE).
     */
    public String getJreVersion() {
        return jreVersion;
    }

    /**
     * Retrieves the vendor of the Java Runtime Environment (JRE).
     *
     * @return the vendor of the Java Runtime Environment (JRE).
     */
    public String getJreVendor() {
        return jreVendor;
    }

    /**
     * Retrieves the implementation name of this Java Virtual Machine.
     *
     * @see RuntimeMXBean#getVmName()
     * @return the implementation name of this Java Virtual Machine.
     */
    public String getVmName() {
        return vmName;
    }

    /**
     * Retrieves the vendor of this Java Virtual Machine.
     *
     * @see RuntimeMXBean#getVmVendor()
     * @return the vendor of this Java Virtual Machine.
     */
    public String getVmVendor() {
        return vmVendor;
    }

    /**
     * Retrieves the implementation version of this Java Virtual Machine.
     *
     * @see RuntimeMXBean#getVmVersion()
     * @return the implementation version of this Java Virtual Machine.
     */
    public String getVmVersion() {
        return vmVersion;
    }

    /**
     * Retrieves a list of input arguments passed to the JVM from all sources, except for the arguments passed to the
     * main() method.
     *
     * @see RuntimeMXBean#getInputArguments()
     * @return a list of input arguments passed to the JVM.
     */
    public List<String> getInputArguments() {
        return Collections.unmodifiableList(inputArguments);
    }

    /**
     * Retrieves the timestamp denoting when the JVM started execution.
     *
     * @see RuntimeMXBean#getStartTime()
     * @return the timestamp denoting when the JVM started execution.
     */
    public Date getStartedTimestamp() {
        return startedTimestamp;
    }

    /**
     * Retrieves the default time zone in this JVM.
     *
     * @see TimeZone#getDefault()
     * @return the default time zone in this JVM.
     */
    public String getDefaultTimeZone() {
        return defaultTimeZone;
    }

    /**
     * Retrieves the default charset in this JVM.
     *
     * @see Charset#defaultCharset()
     * @return the default charset in this JVM.
     */
    public String getDefaultCharset() {
        return defaultCharset;
    }
}

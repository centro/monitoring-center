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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class holds the information about the operating system that hosts this application.
 *
 * @see OperatingSystemMXBean
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OperatingSystemInfo {
    private static final Logger logger = LoggerFactory.getLogger(OperatingSystemInfo.class);

    private static final List<Pattern> LINUX_DISTRIBUTION_PATTERNS = Collections.unmodifiableList(Arrays.asList(
            Pattern.compile("PRETTY_NAME=\"(.+)\""),
            Pattern.compile("DISTRIB_DESCRIPTION=\"(.+)\""))
    );

    private String name;
    private String version;
    private String distributionName;

    private String architecture;
    private int numberOfLogicalProcessors;
    private long physicalMemorySizeInBytes;
    private long swapSpaceSizeInBytes;
    private Map<String, Long> diskSpaceInBytesByRootPaths;

    private OperatingSystemInfo() {
    }

    public static OperatingSystemInfo create() {
        OperatingSystemInfo operatingSystemInfo = new OperatingSystemInfo();

        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();

        operatingSystemInfo.name = operatingSystemMXBean.getName();
        operatingSystemInfo.version = operatingSystemMXBean.getVersion();

        if (SystemUtils.IS_OS_LINUX) {
            operatingSystemInfo.distributionName = resolveLinuxDistributionName();
        }

        operatingSystemInfo.architecture = operatingSystemMXBean.getArch();
        operatingSystemInfo.numberOfLogicalProcessors = operatingSystemMXBean.getAvailableProcessors();

        if (com.sun.management.OperatingSystemMXBean.class.isAssignableFrom(operatingSystemMXBean.getClass())) {
            com.sun.management.OperatingSystemMXBean sunOsMxBean = com.sun.management.OperatingSystemMXBean.class.cast(operatingSystemMXBean);
            operatingSystemInfo.physicalMemorySizeInBytes = sunOsMxBean.getTotalPhysicalMemorySize();
            operatingSystemInfo.swapSpaceSizeInBytes = sunOsMxBean.getTotalSwapSpaceSize();
        }

        Map<String, Long> diskSpaceInBytesByRootPaths = new HashMap<>();
        for (File rootFile : File.listRoots()) {
            diskSpaceInBytesByRootPaths.put(rootFile.getAbsolutePath(), rootFile.getTotalSpace());
        }
        operatingSystemInfo.diskSpaceInBytesByRootPaths = diskSpaceInBytesByRootPaths;

        return operatingSystemInfo;
    }

    /**
     * Retrieves the name of the operating system. For Linux, this method simply returns "Linux"; method
     * {@link #getDistributionName()} can then be utilized to retrieve the distribution name and version.
     *
     * @see OperatingSystemMXBean#getName()
     * @return the name of the operating system.
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieves the version of the operating system. For Linux, kernel version is returned.
     *
     * @see OperatingSystemMXBean#getVersion()
     * @return the version of the operating system.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Retrieves the name of the operating system distribution. Currently, this method only supports Linux
     * distributions. For instance, for a Debian 8, it may return "Debian GNU/Linux 8 (jessie)".
     *
     * @return the name of the operating system distribution.
     */
    public String getDistributionName() {
        return distributionName;
    }

    /**
     * Retrieves the operating system architecture.
     *
     * @see OperatingSystemMXBean#getArch()
     * @return the operating system architecture.
     */
    public String getArchitecture() {
        return architecture;
    }

    /**
     * Retrieves the number of logical processors. Logical processors are individual cores, summed up for all sockets
     * and, possibly, multiplied by a factor of two whenever Intel's hyper-threading technology is in play.
     *
     * @see OperatingSystemMXBean#getAvailableProcessors()
     * @return the number of logical processors.
     */
    public int getNumberOfLogicalProcessors() {
        return numberOfLogicalProcessors;
    }

    /**
     * Retrieves the physical memory (RAM) size in bytes.
     *
     * @see com.sun.management.OperatingSystemMXBean#getTotalPhysicalMemorySize()
     * @return the physical memory (RAM) size in bytes.
     */
    public long getPhysicalMemorySizeInBytes() {
        return physicalMemorySizeInBytes;
    }

    /**
     * Retrieves the swap space size in bytes.
     *
     * @see com.sun.management.OperatingSystemMXBean#getTotalSwapSpaceSize()
     * @return the swap space size in bytes.
     */
    public long getSwapSpaceSizeInBytes() {
        return swapSpaceSizeInBytes;
    }

    /**
     * Retrieves the total disk space in bytes by root paths.
     *
     * @return a map of total disk space in bytes by root paths.
     */
    public Map<String, Long> getDiskSpaceInBytesByRootPaths() {
        return Collections.unmodifiableMap(diskSpaceInBytesByRootPaths);
    }

    // This is not a bullet-proof method, but it should work nicely for most Linux distributions.
    private static String resolveLinuxDistributionName() {
        File etcDirectory = new File("/etc/");
        if (etcDirectory.exists() && etcDirectory.isDirectory()) {
            File[] releaseFiles = etcDirectory.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    return filename.endsWith("-release");
                }
            });

            if (releaseFiles != null) {
                for (File file : releaseFiles) {
                    try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
                        String line = null;
                        while ((line = bufferedReader.readLine()) != null) {
                            for (Pattern pattern : LINUX_DISTRIBUTION_PATTERNS) {
                                Matcher matcher = pattern.matcher(line);
                                if (matcher.find()) {
                                    return matcher.group(1);
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("Error while reading a Linux release file: {}", file.getAbsolutePath(), e);
                    }
                }
            }
        }
        return null;
    }
}

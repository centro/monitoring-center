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

package net.centro.rtb.monitoringcenter.metrics.system.os;

import com.codahale.metrics.Gauge;
import com.sun.management.UnixOperatingSystemMXBean;

import java.io.File;
import java.lang.management.OperatingSystemMXBean;
import java.util.List;

/**
 * This interface holds the current readings for OS-level data points (such as CPU load, RAM usage, swap space usage,
 * and disk usage). All metrics exposed by this class are immutable by the client.
 *
 * @see OperatingSystemMXBean
 * @see com.sun.management.OperatingSystemMXBean
 * @see UnixOperatingSystemMXBean
 */
public interface OperatingSystemStatus {
    /**
     * Retrieves the number of logical processors as observed by the JVM. Logical processors are
     * individual cores, summed up for all sockets and, possibly, multiplied by a factor of two whenever Intel's
     * hyper-threading technology is in play.
     *
     * @see OperatingSystemMXBean#getAvailableProcessors()
     * @return a gauge holding the number of logical processors as observed by the JVM.
     */
    Gauge<Integer> getAvailableLogicalProcessorsGauge();

    /**
     * Retrieves the UNIX system load average for the past minute.
     *
     * @see OperatingSystemMXBean#getSystemLoadAverage()
     * @return a gauge holding the system load average for the past minute; <tt>null</tt> if not available.
     */
    Gauge<Double> getSystemLoadAverageGauge();

    /**
     * Retrieves the UNIX system load average for the past minute divided by the number of logical processors.
     *
     * @return a gauge holding the system load average for the past minute per logical processor; <tt>null</tt> if not
     * available.
     */
    Gauge<Double> getSystemLoadAveragePerLogicalProcessorGauge();

    /**
     * Retrieves the recent CPU usage for the JVM process. The returned value is in the range from 0 to 100.
     *
     * @see com.sun.management.OperatingSystemMXBean#getProcessCpuLoad()
     * @return a gauge holding the percentage of the time CPU was busy recently running JVM threads; <tt>null</tt> if
     * not available.
     */
    Gauge<Double> getJvmCpuBusyPercentageGauge();

    /**
     * Retrieves the recent CPU usage for the whole system. The returned value is in the range from 0 to 100.
     *
     * @see com.sun.management.OperatingSystemMXBean#getSystemCpuLoad()
     * @return a gauge holding the percentage of the time CPU was busy recently; <tt>null</tt> if not available.
     */
    Gauge<Double> getSystemCpuBusyPercentageGauge();

    /**
     * Retrieves the amount of virtual memory that is guaranteed to be available to the running process in bytes.
     *
     * @see com.sun.management.OperatingSystemMXBean#getCommittedVirtualMemorySize()
     * @return a gauge holding the amount of virtual memory that is guaranteed to be available; <tt>null</tt> if not
     * available.
     */
    Gauge<Long> getCommittedVirtualMemorySizeInBytesGauge();

    /**
     * Retrieves the total amount of physical memory in bytes.
     *
     * @see com.sun.management.OperatingSystemMXBean#getTotalPhysicalMemorySize()
     * @return a gauge holding the total amount of physical memory in bytes; <tt>null</tt> if not available.
     */
    Gauge<Long> getTotalPhysicalMemorySizeInBytesGauge();

    /**
     * Retrieves the amount of free physical memory in bytes.
     *
     * @see com.sun.management.OperatingSystemMXBean#getFreePhysicalMemorySize()
     * @return a gauge holding the amount of free physical memory in bytes; <tt>null</tt> if not available.
     */
    Gauge<Long> getFreePhysicalMemorySizeInBytesGauge();

    /**
     * Retrieves the percentage of used physical memory. The returned value is in the range from 0 to 100.
     *
     * @return a gauge holding the percentage of used physical memory; <tt>null</tt> if not available.
     */
    Gauge<Double> getUsedPhysicalMemoryPercentageGauge();

    /**
     * Retrieves the total amount of swap space in bytes.
     *
     * @see com.sun.management.OperatingSystemMXBean#getTotalSwapSpaceSize()
     * @return a gauge holding the total amount of swap space in bytes; <tt>null</tt> if not available.
     */
    Gauge<Long> getTotalSwapSpaceSizeInBytesGauge();

    /**
     * Retrieves the amount of free swap space in bytes.
     *
     * @see com.sun.management.OperatingSystemMXBean#getFreeSwapSpaceSize()
     * @return a gauge holding the amount of free swap space in bytes; <tt>null</tt> if not available.
     */
    Gauge<Long> getFreeSwapSpaceSizeInBytesGauge();

    /**
     * Retrieves the percentage of used swap space. The returned value is in the range from 0 to 100.
     *
     * @return a gauge holding the percentage of used swap space; <tt>null</tt> if not available.
     */
    Gauge<Double> getUsedSwapSpacePercentageGauge();

    /**
     * Retrieves the maximum number of file descriptors.
     *
     * @see UnixOperatingSystemMXBean#getMaxFileDescriptorCount()
     * @return a gauge holding the maximum number of file descriptors; <tt>null</tt> if not available.
     */
    Gauge<Long> getMaxFileDescriptorsGauge();

    /**
     * Retrieves the number of open file descriptors.
     *
     * @see UnixOperatingSystemMXBean#getOpenFileDescriptorCount()
     * @return a gauge holding the number of open file descriptors; <tt>null</tt> if not available.
     */
    Gauge<Long> getOpenFileDescriptorsGauge();

    /**
     * Retrieves the percentage of used file descriptors. The returned value is in the range from 0 to 100.
     *
     * @return a gauge holding the percentage of used file descriptors; <tt>null</tt> if not available.
     */
    Gauge<Double> getUsedFileDescriptorsPercentageGauge();

    /**
     * Retrieves the total disk space in bytes for the root path ("/").
     *
     * @see File#getTotalSpace()
     * @return a gauge holding the total disk space in bytes for the root path; <tt>null</tt> if not available.
     */
    Gauge<Long> getTotalDiskSpaceInBytesGauge();

    /**
     * Retrieves the free disk space in bytes for the root path ("/").
     *
     * @see File#getFreeSpace() ()
     * @return a gauge holding the free disk space in bytes for the root path; <tt>null</tt> if not available.
     */
    Gauge<Long> getFreeDiskSpaceInBytesGauge();

    /**
     * Retrieves the percentage of used disk space for the root path ("/"). The returned value is in the range
     * from 0 to 100.
     *
     * @return a gauge holding the percentage of used disk space for the root path; <tt>null</tt> if not available.
     */
    Gauge<Double> getUsedDiskSpacePercentageGauge();

    /**
     * Retrieves a recent UNIX iowait (percentage of CPU blocked for IO) value as reported in the output for the iostat
     * command. The returned value is in the range from 0 to 100.
     *
     * @return a gauge holding a recent UNIX iowait value; <tt>null</tt> if not available.
     */
    Gauge<Double> getIoWaitPercentageGauge();

    /**
     * Retrieves a list of network interface statuses. An empty list is returned if no network interface data is
     * available.
     *
     * @return a list of network interface statuses.
     */
    List<NetworkInterfaceStatus> getNetworkInterfaceStatuses();
}

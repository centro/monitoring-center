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
import com.google.common.base.Preconditions;
import net.centro.rtb.monitoringcenter.config.NamingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;

/**
 * This class holds the information about a network node. In this context, the definition of a node is somewhat loose
 * and generally means any physical or virtual server running one or more applications.
 *
 * <p>Node ID, data center name, and node group name are all as configured in the
 * {@link net.centro.rtb.monitoringcenter.MonitoringCenter}.
 * </p>
 *
 * <p>The IP addresses in are auto-detected on a best effort basis via introspecting the available instances of
 * {@link NetworkInterface}. As such, the load balancer IP address is searched for in the loopback network interface.
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NodeInfo {
    private static final Logger logger = LoggerFactory.getLogger(NodeInfo.class);

    private String nodeId;
    private String datacenterName;
    private String nodeGroupName;

    private String publicIpAddress;
    private String loadBalancerIpAddress;

    private NodeInfo() {
    }

    // Useful for testing
    public static NodeInfo create(String datacenterName, String nodeGroupName, String nodeId) {
        Preconditions.checkNotNull(datacenterName);
        Preconditions.checkNotNull(nodeGroupName);
        Preconditions.checkNotNull(nodeId);

        NodeInfo nodeInfo = new NodeInfo();

        nodeInfo.datacenterName = datacenterName;
        nodeInfo.nodeGroupName = nodeGroupName;
        nodeInfo.nodeId = nodeId;

        nodeInfo.publicIpAddress = detectPublicIpAddress();
        nodeInfo.loadBalancerIpAddress = detectLoadBalancerIpAddress();

        return nodeInfo;
    }

    public static NodeInfo create(NamingConfig namingConfig) {
        Preconditions.checkNotNull(namingConfig);
        return create(namingConfig.getDatacenterName(), namingConfig.getNodeGroupName(), namingConfig.getNodeId());
    }

    /**
     * Retrieves the node ID, as specified in the MonitoringCenter configuration.
     *
     * @return the node ID, as specified in the MonitoringCenter configuration.
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * Retrieves the data center name, as specified in the MonitoringCenter configuration.
     *
     * @return the data center name, as specified in the MonitoringCenter configuration.
     */
    public String getDatacenterName() {
        return datacenterName;
    }

    /**
     * Retrieves the node group name, as specified in the MonitoringCenter configuration.
     *
     * @return the node group name, as specified in the MonitoringCenter configuration.
     */
    public String getNodeGroupName() {
        return nodeGroupName;
    }

    /**
     * Retrieves the public IP address of this node.
     *
     * @return the public IP address of this node; <tt>null</tt> if could not be detected.
     */
    public String getPublicIpAddress() {
        return publicIpAddress;
    }

    /**
     * Retrieves the IP address of the load balancer, if applicable.
     *
     * @return the IP address of the load balancer; <tt>null</tt> if not applicable or could not be detected.
     */
    public String getLoadBalancerIpAddress() {
        return loadBalancerIpAddress;
    }

    private static String detectPublicIpAddress() {
        Enumeration<NetworkInterface> networkInterfaces = null;
        try {
            networkInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            logger.debug("Unable to obtain network interfaces!", e);
            return null;
        }

        for (NetworkInterface networkInterface : Collections.list(networkInterfaces)) {
            boolean isLoopback = false;
            try {
                isLoopback = networkInterface.isLoopback();
            } catch (SocketException e) {
                logger.debug("Unable to identify if a network interface is a loopback or not");
            }

            if (!isLoopback) {
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    if (Inet4Address.class.isInstance(inetAddress)) {
                        if (!inetAddress.isLoopbackAddress() && !inetAddress.isAnyLocalAddress() &&
                                !inetAddress.isLinkLocalAddress() && !inetAddress.isSiteLocalAddress()) {
                            return inetAddress.getHostAddress();
                        }
                    }
                }
            }
        }

        return null;
    }

    private static String detectLoadBalancerIpAddress() {
        Enumeration<NetworkInterface> networkInterfaces = null;
        try {
            networkInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            logger.debug("Unable to obtain network interfaces!", e);
            return null;
        }

        for (NetworkInterface networkInterface : Collections.list(networkInterfaces)) {
            boolean isLoopback = false;
            try {
                isLoopback = networkInterface.isLoopback();
            } catch (SocketException e) {
                logger.debug("Unable to identify if a network interface is a loopback or not");
                continue;
            }

            if (isLoopback) {
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    if (!inetAddress.isLoopbackAddress() && Inet4Address.class.isInstance(inetAddress)) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        }

        return null;
    }
}

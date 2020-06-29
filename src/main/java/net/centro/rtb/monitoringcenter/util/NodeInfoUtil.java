package net.centro.rtb.monitoringcenter.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Utilities related to the NodeInfo resolution.
 */
public class NodeInfoUtil {
    private static final Logger logger = LoggerFactory.getLogger(NodeInfoUtil.class);

    public static final String PUBLIC_IP_ADDRESS_ENV_VARIABLE_NAME = "METRICS_PUBLIC_IP_ADDRESS";
    public static final String PUBLIC_IP_ADDRESS_SYSTEM_PROPERTY_NAME = "metricsPublicIpAddress";

    /**
     * Attempts to detect the public IP address of this node.
     *
     * @return public IP address of this node, or null if no such IP address could be detected.
     */
    public static String detectPublicIpAddress() {
        String systemPropertyPublicIpAddress = StringUtils.trimToNull(System.getProperty(PUBLIC_IP_ADDRESS_SYSTEM_PROPERTY_NAME));
        if (systemPropertyPublicIpAddress != null) {
            return systemPropertyPublicIpAddress;
        }

        String envVariablePublicIpAddress = StringUtils.trimToNull(System.getenv(PUBLIC_IP_ADDRESS_ENV_VARIABLE_NAME));
        if (envVariablePublicIpAddress != null) {
            return envVariablePublicIpAddress;
        }

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
}

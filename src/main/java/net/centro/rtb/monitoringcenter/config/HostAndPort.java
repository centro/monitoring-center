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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;

/**
 * A simplistic immutable representation of a host and port.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HostAndPort {
    private String host;
    private int port;

    /**
     * Constructs an immutable instance given a host and a port.
     *
     * @param host hostname or IP address.
     * @param port port number.
     * @throws IllegalArgumentException if <tt>host</tt> is blank.
     * @throws IllegalArgumentException if <tt>port</tt> is outside of the valid range of [0, 65535].
     */
    public HostAndPort(@JsonProperty("host") String host, @JsonProperty("port") int port) {
        if (StringUtils.isBlank(host)) {
            throw new IllegalArgumentException("host cannot be blank");
        }
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("invalid port: " + port);
        }

        this.host = host.trim();
        this.port = port;
    }

    /**
     * Parses a host and port from a string and instantiates an instance of HostAndPort using the obtained values.
     *
     * @param hostAndPortStr a string to parse.
     * @return an instance of HostAndPort containing the host and port values parsed from the passed in string.
     * @throws NullPointerException if the passed in string is <tt>null</tt>.
     * @throws IllegalArgumentException if the passed in string is empty.
     */
    public static HostAndPort fromString(String hostAndPortStr) {
        if (hostAndPortStr == null) {
            throw new NullPointerException("hostAndPortStr cannot be null");
        }
        if (hostAndPortStr.trim().isEmpty()) {
            throw new IllegalArgumentException("hostAndPortStr cannot be blank");
        }

        String[] hostAndPortStrParts = hostAndPortStr.split(":");
        if (hostAndPortStrParts.length == 2) {
            String host = hostAndPortStrParts[0].trim();
            String portStr = hostAndPortStrParts[1].trim();

            Integer port = null;
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Port cannot be parsed from " + portStr, e);
            }

            return new HostAndPort(host, port);
        } else {
            throw new IllegalArgumentException("Invalid format: " + hostAndPortStr + ". Expected host:port.");
        }
    }

    /**
     * Constructs a HostAndPort instance from a host and a port. This is a synonym for
     * {@link #HostAndPort(String, int)}.
     *
     * @param host hostname or IP address.
     * @param port port number.
     * @return an instance of HostAndPort representing the passed in host and port values.
     */
    public static HostAndPort of(String host, int port) {
        return new HostAndPort(host, port);
    }

    /**
     * Retrieves the host.
     *
     * @return the host.
     */
    public String getHost() {
        return host;
    }

    /**
     * Retrieves the port.
     *
     * @return the port.
     */
    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("HostAndPort{");
        sb.append("host='").append(host).append('\'');
        sb.append(", port=").append(port);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HostAndPort that = (HostAndPort) o;

        if (port != that.port) return false;
        return !(host != null ? !host.equals(that.host) : that.host != null);

    }

    @Override
    public int hashCode() {
        int result = host != null ? host.hashCode() : 0;
        result = 31 * result + port;
        return result;
    }
}

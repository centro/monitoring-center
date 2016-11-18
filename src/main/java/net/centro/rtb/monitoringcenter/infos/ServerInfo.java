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

import javax.servlet.ServletContext;

/**
 * This class encapsulates basic information about the HTTP server/servlet container, in which an application is
 * running. The data in this class is populated via {@link javax.servlet.ServletContext}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServerInfo {
    private String nameAndVersion;
    private String servletSpecVersion;

    private ServerInfo() {
    }

    public static ServerInfo create(String nameAndVersion, String servletVersion) {
        Preconditions.checkNotNull(nameAndVersion);
        Preconditions.checkNotNull(servletVersion);

        ServerInfo serverInfo = new ServerInfo();

        serverInfo.nameAndVersion = nameAndVersion;
        serverInfo.servletSpecVersion = servletVersion;

        return serverInfo;
    }

    /**
     * Retrieves the name and version of the HTTP server/servlet container. For instance, "Apache Tomcat/8.0.30".
     *
     * @see ServletContext#getServerInfo()
     * @return a string denoting the name and version of the HTTP server/servlet container.
     */
    public String getNameAndVersion() {
        return nameAndVersion;
    }

    /**
     * Retrieves the version of the servlet specification. For instance, "3.1".
     *
     * @see ServletContext#getMajorVersion()
     * @see ServletContext#getMinorVersion()
     * @return the version of the servlet specification.
     */
    public String getServletSpecVersion() {
        return servletSpecVersion;
    }
}

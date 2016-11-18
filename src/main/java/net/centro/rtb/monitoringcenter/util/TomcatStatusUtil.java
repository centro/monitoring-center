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

import net.centro.rtb.monitoringcenter.metrics.tomcat.TomcatConnectorStatus;
import net.centro.rtb.monitoringcenter.metrics.tomcat.TomcatStatus;

public class TomcatStatusUtil {
    private TomcatStatusUtil() {
    }

    public static TomcatConnectorStatus getInternalHttpsConnectorStatus(TomcatStatus tomcatStatus) {
        return getConnectorStatusImpl(tomcatStatus, true, true);
    }

    public static TomcatConnectorStatus getInternalHttpConnectorStatus(TomcatStatus tomcatStatus) {
        return getConnectorStatusImpl(tomcatStatus, true, false);
    }

    public static TomcatConnectorStatus getHttpsConnectorStatus(TomcatStatus tomcatStatus) {
        return getConnectorStatusImpl(tomcatStatus, false, true);
    }

    public static TomcatConnectorStatus getHttpConnectorStatus(TomcatStatus tomcatStatus) {
        return getConnectorStatusImpl(tomcatStatus, false, false);
    }

    private static TomcatConnectorStatus getConnectorStatusImpl(TomcatStatus tomcatStatus, boolean isInternalPort, boolean isSecure) {
        if (tomcatStatus == null || tomcatStatus.getConnectorStatuses() == null) {
            return null;
        }

        for (TomcatConnectorStatus connectorStatus : tomcatStatus.getConnectorStatuses()) {
            if (connectorStatus.isInternalPort() == isInternalPort && connectorStatus.isSecure() == isSecure) {
                return connectorStatus;
            }
        }

        return null;
    }
}

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

import com.codahale.metrics.Gauge;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

public class JmxUtil {
    private static final Logger logger = LoggerFactory.getLogger(JmxUtil.class);

    private static MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

    private JmxUtil() {
    }

    /**
     * Creates a Gauge representing the value of a JMX attribute. If the attribute is not found or it is of a different
     * type, null will be returned to signify absence of value. Default value will be returned by the Gauge, if an
     * exception occurs within the Gauge's getValue() method.
     *
     * @param targetObjectName JMX ObjectName to query.
     * @param attributeName name of the JMX attribute.
     * @param attributeClass class of the JMX attribute's value.
     * @param gaugeDefaultValue default value to be returned by the gauge.
     * @param <T> type of the JMX attribute's value.
     * @return Gauge representing the value of a JMX attribute or null if the attribute is absent or of different type
     */
    public static <T> Gauge<T> getJmxAttributeAsGauge(final ObjectName targetObjectName, final String attributeName, final Class<T> attributeClass, final T gaugeDefaultValue) {
        Preconditions.checkNotNull(targetObjectName);
        Preconditions.checkNotNull(attributeName);
        Preconditions.checkNotNull(attributeClass);

        Object value = null;
        try {
            value = mBeanServer.getAttribute(targetObjectName, attributeName);
        } catch (Exception e) {
            logger.debug("Error while looking up attribute {} in the {} MxBean", attributeName, targetObjectName.getKeyPropertyListString(), e);

            if (InterruptedException.class.isInstance(e)) {
                Thread.currentThread().interrupt();
            }

            return null;
        }

        if (!attributeClass.isInstance(value)) {
            logger.debug("JMX attribute {} in the {} MxBean is of unexpected type: {}", attributeName, targetObjectName.getKeyPropertyListString(), value != null ? value.getClass().getSimpleName() : null);
            return null;
        }

        return new Gauge<T>() {
            @Override
            public T getValue() {
                try {
                    return attributeClass.cast(mBeanServer.getAttribute(targetObjectName, attributeName));
                } catch (Exception ignore) {
                    return gaugeDefaultValue;
                }
            }
        };
    }

    /**
     * Retrieves the value of a JMX attribute. If the attribute is not found or it is of a different type, defaultValue
     * will be returned if the attribute is absent or of different type.
     *
     * @param targetObjectName JMX ObjectName to query.
     * @param attributeName name of the JMX attribute.
     * @param attributeClass class of the JMX attribute's value.
     * @param defaultValue default value to be returned if the JMX attribute is absent or of different type.
     * @param <T> type of the JMX attribute's value.
     * @return value of a JMX attribute or <tt>defaultValue</tt> if the attribute is absent or of different type.
     */
    public static <T> T getJmxAttribute(ObjectName targetObjectName, String attributeName, Class<T> attributeClass, T defaultValue) {
        Preconditions.checkNotNull(targetObjectName);
        Preconditions.checkNotNull(attributeName);
        Preconditions.checkNotNull(attributeClass);

        Object value = null;
        try {
            value = mBeanServer.getAttribute(targetObjectName, attributeName);
        } catch (Exception e) {
            logger.debug("Error while looking up attribute {} in the {} MxBean", attributeName, targetObjectName.getKeyPropertyListString(), e);

            if (InterruptedException.class.isInstance(e)) {
                Thread.currentThread().interrupt();
            }

            return defaultValue;
        }

        if (attributeClass.isInstance(value)) {
            return attributeClass.cast(value);
        } else {
            return defaultValue;
        }
    }
}

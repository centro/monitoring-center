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

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * This utility class handles the naming concerns for metrics and health checks. In particular, this class encapsulates
 * the separator to be used in composite metric names, input sanitization logic, and a facility for building composite
 * names from supplied parts.
 */
public class MetricNamingUtil {
    private static final Pattern ILLEGAL_CHAR_PATTERN = Pattern.compile("[^\\w-\\.]+");
    private static final Pattern SEPARATOR_PATTERN = Pattern.compile("[\\.]+");

    public static final String REPLACEMENT_CHAR = "-";
    public static final String SEPARATOR = ".";

    private MetricNamingUtil() {
    }

    /**
     * Sanitizes the input string by trimming it and replacing illegal characters with
     * {@link MetricNamingUtil#REPLACEMENT_CHAR}. An illegal character is any character but "\w" and "-". That is,
     * this method does not allow the separator ({@link MetricNamingUtil#SEPARATOR}) character.
     *
     * @param valueStr the string to sanitize.
     * @return a sanitized string or <tt>null</tt> if input was null.
     */
    public static String sanitize(String valueStr) {
        return sanitizeImpl(valueStr, false);
    }

    /**
     * Joins an array of strings into a composite name. This method sanitizes each element of the array by trimming it
     * and replacing illegal characters with {@link MetricNamingUtil#REPLACEMENT_CHAR}. An illegal character is any
     * character but "\w", "-", and {@link MetricNamingUtil#SEPARATOR}.
     *
     * @param nameParts array of strings to combine into a composite name.
     * @return a composite name, comprised of sanitized elements of the passed in array, delimited by the separator.
     * @throws NullPointerException if the passed in array is null.
     * @throws IllegalArgumentException if the passed in array is empty.
     */
    public static String join(String[] nameParts) {
        Preconditions.checkNotNull(nameParts, "nameParts cannot be null");
        Preconditions.checkArgument(nameParts.length > 0, "At least one namePart must be provided!");
        return join(nameParts[0], Arrays.copyOfRange(nameParts, 1, nameParts.length));
    }

    /**
     * Joins the passed in strings into a composite name. This method sanitizes each provided string by trimming it
     * and replacing illegal characters with {@link MetricNamingUtil#REPLACEMENT_CHAR}. An illegal character is any
     * character but "\w", "-", and {@link MetricNamingUtil#SEPARATOR}.
     *
     * @param topLevelName top-level part of the name.
     * @param additionalNames additional parts of the name.
     * @return a composite name, comprised of the sanitized passed in strings, delimited by the separator.
     * @throws IllegalArgumentException if top-level part of the name is blank.
     */
    public static String join(String topLevelName, String... additionalNames) {
        Preconditions.checkArgument(StringUtils.isNotBlank(topLevelName), "At least one name must be provided!");

        if (additionalNames == null || additionalNames.length == 0) {
            return sanitizeImpl(topLevelName, true);
        }

        StringBuilder stringBuilder = new StringBuilder().append(topLevelName).append(SEPARATOR);
        for (int i = 0; i < additionalNames.length; i++) {
            stringBuilder.append(sanitizeImpl(additionalNames[i], true));
            if (i < additionalNames.length - 1) {
                stringBuilder.append(SEPARATOR);
            }
        }
        return stringBuilder.toString();
    }

    private static String sanitizeImpl(String valueStr, boolean allowSeparator) {
        if (valueStr == null) {
            return null;
        }

        valueStr = valueStr.trim();
        valueStr = ILLEGAL_CHAR_PATTERN.matcher(valueStr).replaceAll(REPLACEMENT_CHAR);

        if (!allowSeparator) {
            valueStr = SEPARATOR_PATTERN.matcher(valueStr).replaceAll(REPLACEMENT_CHAR);
        }

        if (valueStr.startsWith(REPLACEMENT_CHAR) && valueStr.length() > 1) {
            valueStr = valueStr.substring(1);
        }

        if (valueStr.endsWith(REPLACEMENT_CHAR) && valueStr.length() > 1) {
            valueStr = valueStr.substring(0, valueStr.length() - 1);
        }

        return valueStr;
    }
}

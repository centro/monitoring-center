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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

/**
 * This class encompasses information about the application. In particular, it exposes the application name and build
 * context.
 *
 * <p>Application name is as configured in the {@link net.centro.rtb.monitoringcenter.MonitoringCenter}.</p>
 *
 * <p>The build/version control data is retrieved from buildInfo.properties, which is expected to be found on the
 * classpath. All dates in this file are to be expressed in UTC and to follow the following format: "yyyyMMdd-HHmm".
 * <br><br>
 * The file should have the following properties (including Maven variables as values):</p>
 * <pre>
 *     build.version = ${project.version}
 *     build.timestamp = ${timestamp}
 *     build.username = ${user.name}
 *     vcs.branch = ${git.branch}
 *     vcs.commit.id = ${git.commit.id}
 *     vcs.commit.message = ${git.commit.message.full}
 *     vcs.commit.timestamp = ${git.commit.time}
 *     vcs.commit.author = ${git.commit.user.name}
 * </pre>
 * <p>
 * The values in buildInfo.properties will be expanded by Maven or another build too via the resources filter feature.
 * For Maven, it is recommended to employ git-commit-id-plugin to expand the git-related variables. Also, please note
 * that in Maven the build timestamp needs to be captured into another variable (see
 * http://stackoverflow.com/questions/13228472/how-to-acces-maven-build-timestamp-for-resource-filtering).
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppInfo {
    private static final Logger logger = LoggerFactory.getLogger(AppInfo.class);

    private static final String BUILD_INFO_PROPERTIES_FILE = "/buildInfo.properties";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd-HHmm") {{
        setTimeZone(TimeZone.getTimeZone("UTC"));
    }};

    private String applicationName;

    private String applicationVersion;
    private Date buildTimestamp;
    private String buildUsername;
    private String branchName;
    private String commitId;
    private String commitMessage;
    private Date commitTimestamp;
    private String commitAuthor;

    private AppInfo() {
    }

    public static AppInfo create(String applicationName) {
        Preconditions.checkNotNull(applicationName);

        AppInfo appInfo = new AppInfo();

        appInfo.applicationName = applicationName;

        Properties properties = new Properties();
        try {
            properties.load(AppInfo.class.getResourceAsStream(BUILD_INFO_PROPERTIES_FILE));
        } catch (Exception e) {
            logger.debug("Error loading the buildInfo file from the classpath: {}", BUILD_INFO_PROPERTIES_FILE, e);

            if (InterruptedException.class.isInstance(e)) {
                Thread.currentThread().interrupt();
            }

            return appInfo;
        }

        appInfo.applicationVersion = StringUtils.trimToNull(properties.getProperty("build.version"));
        appInfo.buildTimestamp = parseDate(properties.getProperty("build.timestamp"));
        appInfo.buildUsername = StringUtils.trimToNull(properties.getProperty("build.username"));

        appInfo.branchName = StringUtils.trimToNull(properties.getProperty("vcs.branch"));
        appInfo.commitId = StringUtils.trimToNull(properties.getProperty("vcs.commit.id"));
        appInfo.commitMessage = StringUtils.trimToNull(properties.getProperty("vcs.commit.message"));
        appInfo.commitTimestamp = parseDate(properties.getProperty("vcs.commit.timestamp"));
        appInfo.commitAuthor = StringUtils.trimToNull(properties.getProperty("vcs.commit.author"));

        return appInfo;
    }

    private static Date parseDate(String dateString) {
        if (dateString != null) {
            try {
                return DATE_FORMAT.parse(dateString);
            } catch (ParseException e) {
                logger.debug("Could not parse date from {}", dateString, e);
            }
        }
        return null;
    }

    /**
     * Retrieves the application name, as specified in the MonitoringCenter configuration.
     *
     * @return the application name.
     */
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * Retrieves the version of the application.
     *
     * @return the version of the application.
     */
    public String getApplicationVersion() {
        return applicationVersion;
    }

    /**
     * Retrieves the timestamp indicating when this application was built.
     *
     * @return the timestamp indicating when this application was built.
     */
    public Date getBuildTimestamp() {
        return buildTimestamp;
    }

    /**
     * Retrieves the username of the user who ran the application build.
     *
     * @return the username of the user who ran the application build.
     */
    public String getBuildUsername() {
        return buildUsername;
    }

    /**
     * Retrieves the name of the branch from which the application was built.
     *
     * @return the name of the branch from which the application was built.
     */
    public String getBranchName() {
        return branchName;
    }

    /**
     * Retrieves the ID of the most recent commit in this application's build.
     *
     * @return the ID of the most recent commit in this application's build.
     */
    public String getCommitId() {
        return commitId;
    }

    /**
     * Retrieves the commit message for the most recent commit in this application's build.
     *
     * @return the commit message for the most recent commit in this application's build.
     */
    public String getCommitMessage() {
        return commitMessage;
    }

    /**
     * Retrieves the timestamp of the most recent commit in this application's build.
     *
     * @return the timestamp of the most recent commit in this application's build.
     */
    public Date getCommitTimestamp() {
        return commitTimestamp;
    }

    /**
     * Retrieves the author of the most recent commit in this application's build.
     *
     * @return the author of the most recent commit in this application's build.
     */
    public String getCommitAuthor() {
        return commitAuthor;
    }
}

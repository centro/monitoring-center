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

package net.centro.rtb.monitoringcenter;

import com.codahale.metrics.Metric;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.json.HealthCheckModule;
import com.codahale.metrics.json.MetricsModule;
import com.codahale.metrics.jvm.ThreadDump;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.BaseEncoding;
import net.centro.rtb.monitoringcenter.infos.ServerInfo;
import net.centro.rtb.monitoringcenter.util.GraphiteMetricFormatter;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * This servlet exposes the various facets of the {@link MonitoringCenter}. The exposed endpoints provide the user with
 * an ability to retrieve metrics, health checks, information about the system and about the application. In addition,
 * this servlet has a ping endpoint and an endpoint that can be used to generate and display a thread dump.
 *
 * <p>In order to enable this servlet, the client application needs to declare and map it in web.xml. The mapping
 * is up to the client, but for consistency it is recommended to map this servlet to /monitoringCenter.</p>
 *
 * <p>
 *     The servlet documents itself at the root path--that is, sending a GET request to this servlet's root path (e.g.,
 *     GET /monitoringCenter) will output a JSON array containing descriptions for all exposed endpoints. Also, one can
 *     access these same descriptions programmatically via the {@link #ENDPOINT_DESCRIPTIONS} constant.
 * </p>
 */
public class MonitoringCenterServlet extends HttpServlet {
    public static class EndpointDescription {
        @JsonProperty
        private final String path;
        @JsonProperty
        private final String description;
        @JsonProperty
        private Map<String, String> queryParams;

        private EndpointDescription(String path, String description) {
            this.path = path;
            this.description = description;
        }

        private EndpointDescription queryParam(String queryParamName, String description) {
            if (queryParams == null) {
                queryParams = new HashMap<>();
            }
            queryParams.put(queryParamName, description);
            return this;
        }

        public String getPath() {
            return path;
        }

        public String getDescription() {
            return description;
        }

        public Map<String, String> getQueryParams() {
            if (queryParams != null) {
                return Collections.unmodifiableMap(queryParams);
            }
            return null;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("EndpointDescription{");
            sb.append("path='").append(path).append('\'');
            sb.append(", description='").append(description).append('\'');
            sb.append(", queryParams=").append(queryParams);
            sb.append('}');
            return sb.toString();
        }
    }

    private static final String DEFAULT_CREDENTIALS = "monitoringCenter:d3faUlt~P4Ssw0rD";

    private static final String DISABLE_AUTHORIZATION_INIT_PARAM = "disableAuthorization";
    private static final String USERNAME_INIT_PARAM = "username";
    private static final String PASSWORD_INIT_PARAM = "password";

    private static final String PATH_METRICS = "/metrics";
    private static final String PATH_HEALTHCHECKS = "/healthChecks";
    private static final String PATH_PING = "/ping";
    private static final String PATH_THREADDUMP = "/threadDump";

    private static final String PATH_SYSTEM_INFO = "/systemInfo";
    private static final String PATH_NODE_INFO = "/nodeInfo";
    private static final String PATH_SERVER_INFO = "/serverInfo";
    private static final String PATH_APP_INFO = "/appInfo";

    public static final List<EndpointDescription> ENDPOINT_DESCRIPTIONS = Collections.unmodifiableList(new ArrayList<EndpointDescription>() {{
        add(new EndpointDescription(PATH_METRICS, "Retrieves current readings from registered metrics.")
                .queryParam("format", "Serialization format. Supported formats are JSON (\"json\") and Graphite-ready string (\"graphite\"). " +
                        "By default, JSON will be served.")
                .queryParam("startsWithFilter", "Filters to be applied to metric names. A filter can include multiple wildcards, expressed as \"*\". " +
                        "Multiple filters can be specified; at least one filter must match for a metric to be included in the response. The filters " +
                        "are applied to the actual metric name, with no regard to the node-specific prefix. By default, all registered metrics will be returned.")
                .queryParam("prettyPrint", "Indicates whether to nicely format the output or not. Only applies to JSON. Boolean values must be specified as " +
                        "\"true\" or \"false\". By default, the output will not be pretty printed.")
                .queryParam("appendPrefix", "Indicates whether to append the node-specific prefix to metric names or not. Boolean values must be specified as " +
                        "\"true\" or \"false\". By default, the prefix will not be appended."));
        add(new EndpointDescription(PATH_HEALTHCHECKS, "Runs all registered health checks and returns their results as JSON.")
                .queryParam("prettyPrint", "Indicates whether to nicely format the output or not. Boolean values must be specified as " +
                        "\"true\" or \"false\". By default, the output will not be pretty printed."));
        add(new EndpointDescription(PATH_HEALTHCHECKS + "/{healthCheckName}", "Runs a health check represented by {healthCheckName} and returns its result as JSON.")
                .queryParam("prettyPrint", "Indicates whether to nicely format the output or not. Boolean values must be specified as " +
                        "\"true\" or \"false\". By default, the output will not be pretty printed."));
        add(new EndpointDescription(PATH_PING, "\"Pings\" the node. The expected output is \"pong\"."));
        add(new EndpointDescription(PATH_THREADDUMP, "Generates a Java thread dump and returns its output as text."));
        add(new EndpointDescription(PATH_SYSTEM_INFO, "Retrieves system information, which consists of operating system and JVM data points.")
                .queryParam("prettyPrint", "Indicates whether to nicely format the output or not. Boolean values must be specified as " +
                        "\"true\" or \"false\". By default, the output will not be pretty printed."));
        add(new EndpointDescription(PATH_NODE_INFO, "Retrieves node information, including node nomenclature and networking set-up.")
                .queryParam("prettyPrint", "Indicates whether to nicely format the output or not. Boolean values must be specified as " +
                        "\"true\" or \"false\". By default, the output will not be pretty printed."));
        add(new EndpointDescription(PATH_SERVER_INFO, "Retrieves HTTP server information.")
                .queryParam("prettyPrint", "Indicates whether to nicely format the output or not. Boolean values must be specified as " +
                        "\"true\" or \"false\". By default, the output will not be pretty printed."));
        add(new EndpointDescription(PATH_APP_INFO, "Retrieves app information, including build-related data points.")
                .queryParam("prettyPrint", "Indicates whether to nicely format the output or not. Boolean values must be specified as " +
                        "\"true\" or \"false\". By default, the output will not be pretty printed."));
    }});

    private static final String FORMAT_GRAPHITE = "graphite";

    private static final String CONTENT_TYPE_APPLICATION_JSON = "application/json";
    private static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain";

    private static final String PING_RESPONSE = "pong";

    private ObjectMapper objectMapper;
    private GraphiteMetricFormatter graphiteMetricFormatter;

    private ThreadDump threadDumpGenerator;

    private String encodedCredentials;

    private ServerInfo serverInfo;

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);

        boolean disableAuthorization = Boolean.TRUE.toString().equalsIgnoreCase(servletConfig.getInitParameter(DISABLE_AUTHORIZATION_INIT_PARAM));
        if (!disableAuthorization) {
            String credentials = null;

            String username = servletConfig.getInitParameter(USERNAME_INIT_PARAM);
            String password = servletConfig.getInitParameter(PASSWORD_INIT_PARAM);
            if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
                credentials = username.trim() + ":" + password.trim();
            } else {
                credentials = DEFAULT_CREDENTIALS;
            }

            this.encodedCredentials = BaseEncoding.base64().encode(credentials.getBytes());
        }

        this.objectMapper = new ObjectMapper()
                .registerModule(new MetricsModule(TimeUnit.SECONDS, TimeUnit.MICROSECONDS, false))
                .registerModule(new HealthCheckModule())
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .setTimeZone(TimeZone.getDefault())
                .setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z"));

        this.graphiteMetricFormatter = new GraphiteMetricFormatter(TimeUnit.SECONDS, TimeUnit.MICROSECONDS);

        try {
            this.threadDumpGenerator = new ThreadDump(ManagementFactory.getThreadMXBean());
        } catch (NoClassDefFoundError ignore) {
        }

        ServletContext servletContext = servletConfig.getServletContext();
        String servletSpecVersion = servletContext.getMajorVersion() + "." + servletContext.getMinorVersion();
        this.serverInfo = ServerInfo.create(servletContext.getServerInfo(), servletSpecVersion);
    }

    @Override
    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        httpServletResponse.setHeader("Cache-Control", "must-revalidate,no-cache,no-store");
        httpServletResponse.setHeader("Pragma", "no-cache");
        httpServletResponse.setHeader("Expires", "Tue, 11 Oct 1977 12:34:56 GMT");

        if (!checkAuthorization(httpServletRequest)) {
            httpServletResponse.setHeader("WWW-Authenticate", "Basic realm=\"Centro Monitoring Center\"");
            httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        final String path = httpServletRequest.getPathInfo();

        if (path == null || path.equals("/")) {
            handlePathDescriptions(httpServletRequest, httpServletResponse);
        } else if (path.startsWith(PATH_METRICS)) {
            handleMetrics(httpServletRequest, httpServletResponse);
        } else if (path.startsWith(PATH_HEALTHCHECKS)) {
            handleHealthChecks(httpServletRequest, httpServletResponse);
        } else if (path.startsWith(PATH_PING)) {
            handlePing(httpServletRequest, httpServletResponse);
        } else if (path.startsWith(PATH_THREADDUMP)) {
            handleThreadDump(httpServletRequest, httpServletResponse);
        } else if (path.startsWith(PATH_SYSTEM_INFO)) {
            handleSystemInfo(httpServletRequest, httpServletResponse);
        } else if (path.startsWith(PATH_NODE_INFO)) {
            handleNodeInfo(httpServletRequest, httpServletResponse);
        } else if (path.startsWith(PATH_SERVER_INFO)) {
            handleServerInfo(httpServletRequest, httpServletResponse);
        } else if (path.startsWith(PATH_APP_INFO)) {
            handleAppInfo(httpServletRequest, httpServletResponse);
        } else {
            httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void handlePathDescriptions(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        writeAsJson(httpServletRequest, httpServletResponse, ENDPOINT_DESCRIPTIONS);
    }

    private void handleMetrics(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        String format = StringUtils.trimToNull(httpServletRequest.getParameter("format"));
        String[] startsWithFilters = httpServletRequest.getParameterValues("startsWithFilter");

        if (FORMAT_GRAPHITE.equalsIgnoreCase(format)) {
            httpServletResponse.setContentType(CONTENT_TYPE_TEXT_PLAIN);

            try (PrintWriter printWriter = httpServletResponse.getWriter()) {
                SortedMap<String, Metric> metricsByNames = MonitoringCenter.getMetricsByNames(true, startsWithFilters);
                printWriter.write(graphiteMetricFormatter.format(metricsByNames));
            }
        } else {
            boolean appendPrefix = Boolean.TRUE.toString().equalsIgnoreCase(StringUtils.trimToNull(httpServletRequest.getParameter("appendPrefix")));

            Map<String, SortedMap<String, ? extends Metric>> responseMap = new LinkedHashMap<>();
            responseMap.put("gauges", MonitoringCenter.getGaugesByNames(appendPrefix, startsWithFilters));
            responseMap.put("counters", MonitoringCenter.getCountersByNames(appendPrefix, startsWithFilters));
            responseMap.put("histograms", MonitoringCenter.getHistogramsByNames(appendPrefix, startsWithFilters));
            responseMap.put("meters", MonitoringCenter.getMetersByNames(appendPrefix, startsWithFilters));
            responseMap.put("timers", MonitoringCenter.getTimersByNames(appendPrefix, startsWithFilters));

            writeAsJson(httpServletRequest, httpServletResponse, responseMap);
        }
    }

    private void handleHealthChecks(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        String healthCheckName = null;
        String path = httpServletRequest.getPathInfo();
        if (StringUtils.isNotBlank(path)) {
            String noLeadingSlash = path.substring(1);
            int indexOfSlash = noLeadingSlash.indexOf("/");
            if (indexOfSlash != -1) {
                healthCheckName = noLeadingSlash.substring(indexOfSlash + 1);
            }
        }

        if (StringUtils.isNotBlank(healthCheckName)) {
            try {
                HealthCheck.Result result = MonitoringCenter.runHealthCheck(healthCheckName);
                writeAsJson(httpServletRequest, httpServletResponse, result);
            } catch (NoSuchElementException e) {
                httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        } else {
            SortedMap<String, HealthCheck.Result> healthCheckResultsByNames = MonitoringCenter.runHealthChecks();
            writeAsJson(httpServletRequest, httpServletResponse, healthCheckResultsByNames);
        }
    }

    private void handlePing(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        httpServletResponse.setContentType(CONTENT_TYPE_TEXT_PLAIN);

        try (PrintWriter writer = httpServletResponse.getWriter()) {
            writer.println(PING_RESPONSE);
        }
    }

    private void handleThreadDump(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        httpServletResponse.setContentType(CONTENT_TYPE_TEXT_PLAIN);

        if (threadDumpGenerator == null) {
            try (PrintWriter writer = httpServletResponse.getWriter()) {
                writer.println("Thread dump generation is not supported on this node.");
            }
        } else {
            try (OutputStream output = httpServletResponse.getOutputStream()) {
                threadDumpGenerator.dump(output);
            }
        }
    }

    private void handleSystemInfo(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        writeAsJson(httpServletRequest, httpServletResponse, MonitoringCenter.getSystemInfo());
    }

    private void handleNodeInfo(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        writeAsJson(httpServletRequest, httpServletResponse, MonitoringCenter.getNodeInfo());
    }

    private void handleAppInfo(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        writeAsJson(httpServletRequest, httpServletResponse, MonitoringCenter.getAppInfo());
    }

    private void handleServerInfo(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        writeAsJson(httpServletRequest, httpServletResponse, serverInfo);
    }

    private void writeAsJson(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object object) throws IOException {
        boolean prettyPrint = Boolean.TRUE.toString().equalsIgnoreCase(StringUtils.trimToNull(httpServletRequest.getParameter("prettyPrint")));

        httpServletResponse.setContentType(CONTENT_TYPE_APPLICATION_JSON);

        try (OutputStream output = httpServletResponse.getOutputStream()) {
            if (prettyPrint) {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(output, object);
            } else {
                objectMapper.writeValue(output, object);
            }
        }
    }

    private boolean checkAuthorization(HttpServletRequest httpServletRequest) {
        if (encodedCredentials == null) {
            return true;
        }

        String basicAuthHeader = httpServletRequest.getHeader("Authorization");
        if (basicAuthHeader != null && basicAuthHeader.toUpperCase().startsWith("BASIC")) {
            int indexOfSpace = basicAuthHeader.indexOf(' ');
            if (indexOfSpace > 0) {
                String credentials = basicAuthHeader.substring(indexOfSpace).trim();
                return encodedCredentials.equals(credentials);
            }
        }
        return false;
    }
}

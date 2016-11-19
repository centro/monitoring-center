## MonitoringCenter
MonitoringCenter is an easy-to-use, comprehensive framework for programmatically monitoring Java applications.
It is built on top of Dropwizard's [Metrics](https://dropwizard.github.io/metrics) library.
What MonitoringCenter gives you is:
* Ability to centrally collect and report on metrics.
* Ability to centrally register, execute, and report on health checks.
* Out of the box metric collection for OS, JVM, and Tomcat statistics, including programmatic access to the collected metrics.
* Out of the box support for instrumentation of collections, maps, caches, executors, and C3P0 data sources.
* Logger-like paradigm for metric collection from anywhere in your Java application.
* Unified, distributed system-ready, naming schema for metrics and health checks.
* Programmatic, file-based, or hybrid framework configuration with support for reloadable parameters.
* Insight into the OS, JVM, network node, application, and servlet container configuration.

##### Why not simply use Dropwizard's Metrics?
While Metrics is a great library, it merely provides building blocks for metrics and health checks
without a clear path to enterprise integration and roll out. MonitoringCenter aims to provide idioms
and ready-to-use facilities that can right away be employed throughout your Java applications.

As we worked on the Centro's Real-Time Bidding systems, we gradually realized that 1) gaining insight into the live
application is absolutely crucial for decision-making and, later on, that 2) our monitoring code is all
over the place. Bidder's monitoring code consisted of Dropwizard's Metrics registered via a singleton,
legacy metrics (AtomicLong bundles) owned by multiple services, JSPs, logging statements, a myriad of servlets
for reporting health checks, system status, build info, and node info. Naming was up to an engineer's mood.
Altogether, a chaos, out of which MonitoringCenter was born.

MonitoringCenter's ultimate goal is to encapsulate, standardize, and facilitate all monitoring concerns within a
Java application.

##### Is it safe to use? How is performance?
MonitoringCenter has been fully integrated into all of the Centro's Real-Time Bidding components. These components are 
currently running in production with the library invoked on the critical path millions of times a second.

Generally speaking, noticeable overhead is only incurred on calling the reporting methods--be it programmatically, via
`MonitoringCenterServlet`, or via Graphite reporter.

##### Compatibility
Java 7+

##### Dependencies
* [Dropwizard's Metrics](https://dropwizard.github.io/metrics) - engine of the MonitoringCenter.
* [Jackson JSON](https://github.com/FasterXML/jackson) - config file parsing; reporting via the MonitoringCenterServlet.
* [Google Guava](https://github.com/google/guava) - utilities and cache instrumentation.
* Apache [Commons IO](https://commons.apache.org/proper/commons-io/) and [Commons Lang3](https://commons.apache.org/proper/commons-lang/) - utilities.
* [C3P0](http://www.mchange.com/projects/c3p0/) - instrumentation of C3P0 data sources, under consideration for removal and migration to JMX.
* [SLF4J](http://www.slf4j.org/) - logging.
* Servlet API - `MonitoringCenterServlet`.


## Usage
#### Prerequisites
Familiarity with Dropwizard's [Metrics](https://dropwizard.github.io/metrics) library.

#### Basic Architecture
* `MonitoringCenter` - is the heart of the MonitoringCenter library; it is a singleton class, which owns MetricCollector instances,
the *Info and *Status objects. It encapsulates Dropwizard's MetricRegistry and HealthCheckRegistry.
* `MetricCollector` - logger-like construct retrieved from and owned by `MonitoringCenter`; it is an interface providing
methods for collecting metrics and instrumenting various entities.
* `MonitoringCenterServlet` - a servlet exposing most of MonitoringCenter's capabilities (read-only).
* `SystemInfo`, `NodeInfo`, `AppInfo`, `ServerInfo` - classes representing static information about the environment in which the app is running.
* `SystemStatus`, `TomcatStatus` - classes representing live OS, JVM, and Tomcat statistics.
* `Configurator`, `MonitoringCenterConfig` - configuration classes.

Please see Javadoc comments for these classes to get a better grasp of them.

#### Installation
Maven
```xml
<dependency>
  <groupId>net.centro.rtb</groupId>
  <artifactId>monitoring-center</artifactId>
  <version>${monitoring-center-version}</version>
</dependency>
```

Gradle
```
dependencies {
  compile "net.centro.rtb:monitoring-center:${monitoring-center-version}"
}
```
#### Javadoc
The library contains a substantial amount of Javadoc comments. These should be available in your IDE, once you declare
a dependency on MonitoringCenter via Maven or Gradle.

#### Naming
Naming is of paramount importance for metrics as they are collected and aggregated in a distributed manner, undocumented,
heterogeneous in type, and hierarchical in nature. The naming schema employed by the MonitoringCenter allows for
metrics to be self-contained. Giving a complete metric name, it can be known where this metric comes from and
of what type it is. Metrics are prefixed with a number of namespaces separated by the dot (".") symbol. Namespaces enable
hierarchical, folder-like classification for metrics.

###### Schema
Below is the schema for metric naming in the MonitoringCenter:
```
[applicationName].[datacenterName].[nodeGroupName].[nodeId].[metricCollectorName].[?additionalNamespaces].[metricName][?metricType]
```
All parts of the schema, except for those marked with a question mark, are required and will always be present in the
metrics reported to central aggregators, such as Graphite.

###### Node-Specific Prefix
The first four parts of the schema--applicationName, datacenterName, nodeGroupName, and nodeId--form the so-called
node-specific prefix. When not applicable or not available, datacenterName, nodeGroupName, and nodeId will be reported as
"none". This node-specific prefix is appended to metric names, when reported to central aggregators. Thus, when accessing
metrics programmatically or via `MonitoringCenterServlet` (unless Graphite format is requested), this
prefix will not be utilized. From the implementation point of view, metrics are internally registered without the prefix.

###### Type Postfix
By default, a string denoting the type of the metric will be appended to composite metric types. That is, "Histogram"
will be appended to histogram names, "Meter" - to meter names, and "Timer" - to timer names. This feature is called
MetricNamePostfixPolicy and can be easily be configured as: append type to all metrics, append type to composite metrics
(default), and do not append the type at all. Of course, if the name of the metric already ends with the type string,
no postfix will be appended.

###### Illegal Characters
Namespaces, metric, and health check names are only allowed to contain lower-case and upper-case letters, digits, dashes,
and underscores. All other characters will automatically be replaced by a dash ("-").

###### Conventions
We recommend naming metrics similarly to Java fields: use camel case. Since counters and gauges are simple metrics,
exhibiting a single value, there is normally no need to add the metric type (i.e., "Counter" or "Gauge") to their names.

When instrumenting collections, maps, caches, or other entities, it is a good idea to append the type of entity to the
name of the entity (e.g., "pendingWinsQueue" vs "pendingWins").

###### Metric Collector Naming
There are two reserved namespaces in the MonitoringCenter: "system" and "tomcat". No MetricCollector can be registered
under these reserved namespaces.

For database-related metrics, MonitoringCenter offers a MetricCollector namespaced at "dbs".

#### Configuration
MonitoringCenter can be configured programmatically, using a config file, or both--in a hybrid approach. When a config
file is used, MonitoringCenter supports dynamic reloading of non-naming configuration parameters. For instance, one could
enable Graphite push reporting on a running application. In a hybrid approach, the config file is used to pre-fill the
`MonitoringCenterConfig.Builder` with initial values, while programmatic configuration allows to supplement or override
configuration parameters.

`MonitoringCenter` must be configured before any application code using it is executed. Thus, it is recommended to run
`MonitoringCenter.configure()` as the first statement in the ContextListener or the main() method
(perhaps, right after configuring your logging framework). Please note that, if not configured prior to usage, 
MonitoringCenter will return a no-op implementation of `MetricCollector` interface upon a call to `getMetricCollector()` 
and will silently ignore the call to `registerHealthCheck(String, HealthCheck)`.

The minimal configuration consists of the application name; in other words, it is the only required parameter.

A typical idiom for configuring MonitoringCenter is (using `Configurator`):

Programmatic only (no config file and no reloading)
```java
MonitoringCenter.configure(Configurator.noConfigFile()
                .applicationName("myToyApplication")
                .appendTypeToHealthCheckNames(true)
                .enableSystemMetrics(true)
                .enableTomcatMetrics(false).build());
```

Default config file (looks up monitoringCenter.yaml on the classpath)
```java
MonitoringCenter.configure(Configurator.defaultConfigFile()
                .applicationName("myStandAloneJob")
                .metricNamePostfixPolicy(MetricNamePostfixPolicy.ADD_ALL_TYPES)
                .appendTypeToHealthCheckNames(false)
                .enableSystemMetrics(false)
                .enableTomcatMetrics(false).build());
```

Custom config file
```java
MonitoringCenter.configure(Configurator.configFile(new File("/myFavoriteDir/monitoringCenter.yaml"))
                .applicationName("bidder")
                .metricNamePostfixPolicy(MetricNamePostfixPolicy.ADD_COMPOSITE_TYPES)
                .appendTypeToHealthCheckNames(true)
                .enableSystemMetrics(true)
                .enableTomcatMetrics(true).build());
```

##### Configuration File
MonitoringCenter uses [YAML](http://yaml.org/) as the format for configuration files.

Sample config file with comments
```yaml
naming:
  applicationName: "bidder" #The only required field. It has to be set in the file or programmatically.
  datacenterName: "east"    #If not specified, env. variables will be checked. If neither is set, "none" is used.
  nodeGroupName: "thc"  #If not specified, env. variables will be checked. If neither is set, "none" is used.
  nodeId: "ltest3"  #If not specified, env. variables will be checked. If neither is set, "none" is used.
  metricNamePostfixPolicy: "ADD_COMPOSITE_TYPES"    #Supported values: ADD_ALL_TYPES, ADD_COMPOSITE_TYPES (default), OFF.
  appendTypeToHealthCheckNames: true    #Default: false.
metricCollection:
  enableSystemMetrics: true #Default: false.
  enableTomcatMetrics: true #Default: false.
metricReporting:
  graphite: #Config for Graphite push reporter.
    address:    #The only required field for the Graphite reporter config.
      host: "graphiteHost.com"
      port: 8080
    reportingIntervalInSeconds: 5   #Default is 60 seconds.
    startsWithFilters:  #A list of whitelist filters. A metric satisfies a filter, if its name starts with the filter.
      - "tomcat.connectors.*."  #A filter may contain multiple wildcards, expressed as *.
    blockedStartsWithFilters:  #A list of blacklist filters. A metric satisfies a filter, if its name starts with the filter.
      - "tomcat.connectors.http-apr-443."  #Blocked (i.e., blacklist) filters trump the whitelist filters.  
```

###### Reloadability
In order to support hybrid configuration (config file plus programmatic), MonitoringCenter implements reloadable
configuration parameters by means of an additional configuration file created at runtime in the same directory as the
original configuration file. This runtime configuration file contains the effective config derived from the values from
the original configuration file collated with the values provided programmatically. The name of this runtime configuration
file will follow the template: "monitoringCenter-%applicationName%-current.yaml".

The runtime configuration file is reloaded every minute.

##### Environment Variables
It may be desirable or required to define node-global configuration parameters at system level using environment variables.
The following environment variables are supported by the MonitoringCenter:
* METRICS_DATACENTER
* METRICS_NODE_GROUP
* METRICS_NODE_ID


#### Metric Collection
Metric collection concerns are encapsulated in the `MetricCollector` interface, instances of which can be obtained from
`MonitoringCenter`. This interface is intended to conceptually mimic a logger. As such, the following usage idiom is
typical:

```java
private static final MetricCollector metricCollector = MonitoringCenter.getMetricCollector(AuctionSamplingService.class);
```

To encourage standard naming, `MonitoringCenter` has a special method for retrieval of a metric collector for database-related
metrics: `MonitoringCenter.getDatabaseMetricCollector()`.

In addition to the ability to instantiate or retrieve Dropwizard's `Metric` implementations (e.g., `Counter`), the
`MetricCollector` interface exposes high-level methods for registering and instrumenting collections, maps, caches,
executors, and data sources. The instrumentation methods return a proxy instance that has to be employed in lieu
of the original instance.

All metrics registered within a `MetricCollector` are prefixed with its namespace (defined when retrieving a
`MetricCollector` instance from `MonitoringCenter`). This interface also enforces a metric naming policy specified
in the MonitoringCenter's configuration. All in all, `MetricCollector` abstracts away all the naming intricacies,
allowing the client to simply reference a metric by its short name ("coins" vs "SampleService.coinsMeter").

##### System and Tomcat Metrics
MonitoringCenter has out of the box support for operating system, JVM, and Tomcat monitoring. When enabled in the config,
system and Tomcat metric sets are registered and available for reporting and programmatic access. In order to access
the system or Tomcat metrics programmatically, from your Java application, you can use the following two methods:

```java
SystemStatus systemStatus = MonitoringCenter.getSystemStatus();
TomcatStatus tomcatStatus = MonitoringCenter.getTomcatStatus();
```

##### Instrumentation
In order to simplify and unify metric collection, the `MetricCollector` interface offers methods for instrumenting
collections, maps, Guava Cache instances, Java executors, and C3P0 and generic data sources. When such methods return
a proxied instance of the original entity, it is crucial for the client to use the proxied instance instead of the original
one in order for the metrics to be recorded.

#### Metric Reporting
Metrics can be reported in a number of ways. Metrics can be accessed programmatically from the `MonitoringCenter`, they
can be retrieved in JSON or Graphite-ready format via the `MonitoringCenterServlet`, or they can be pushed directly to
a Graphite instance. For all the aforementioned reporting scenarios, the client is at liberty to define one or more
filters to constrain the returned metrics. These filters support multiple wildcards expressed as `*`.

When reporting metrics to Graphite--be it pull or push--the node-specific prefix will be appended to metric names. In all
other cases, metrics names will not contain the node-specific prefix, unless explicitly requested. Please note that filters
are applied to metric names with no regard to the node-specific prefix.

#### Health Checks
A health check can be registered by calling:

```java
MonitoringCenter.registerHealthCheck("name", healthCheck);
```

Once registered, a health check can be executed programmatically (`MonitoringCenter.runHealthCheck("name")`) or via the
`MonitoringCenterServlet`. Please note that health checks are executed on the client thread.

#### Infos
MonitoringCenter provides facilities for gaining insight into the operating system, JVM, network node, application, and
servlet container configurations. These are cumulatively known as "infos". There are three "infos" available for programmatic
access: `SystemInfo` (via `MonitoringCenter.getSystemInfo()`), `AppInfo` (via `MonitoringCenter.getAppInfo()`), and
`NodeInfo` (via `MonitoringCenter.getNodeInfo()`).

* `SystemInfo` encapsulates information about the operating system and the JVM.
* `AppInfo` encompasses information about the application. In particular, it exposes the application name and build context.
* `NodeInfo` holds the information about a network node. In this context, the definition of a node is somewhat loose and
generally means any physical or virtual server running one or more applications.

`MonitoringCenterServlet` exposes these three "infos" as well as `ServerInfo`, containing basic information about the
servlet container.

### MonitoringCenterServlet
`MonitoringCenterServlet` is a servlet shipped with the MonitoringCenter. It can be enabled in `web.xml` like any other
servlet; its mapping is completely up to the engineer, but for consistency it is recommend to map it at "/monitoringCenter/*".
For instance,

```xml
    <servlet>
        <servlet-name>MonitoringCenterServlet</servlet-name>
        <servlet-class>net.centro.rtb.monitoringcenter.MonitoringCenterServlet</servlet-class>
        <load-on-startup>4</load-on-startup>
    </servlet>

    <servlet-mapping>
        <servlet-name>MonitoringCenterServlet</servlet-name>
        <url-pattern>/monitoringCenter/*</url-pattern>
    </servlet-mapping>
```

This servlet is self-documenting--one can access the endpoint descriptions by navigating to the servlet's root path
(e.g., /monitoringCenter). As a brief overview, `MonitoringCenterServlet` provides endpoints for retrieval of metrics,
results of health checks, system info, app info, node info, and server info, as well as basic ping and thread dump generation
facilities.

For security reasons, MonitoringCenterServlet requires an authorization header to be present on all requests. The default
credentials are presented in the servlet's source code. It is possible to disable authorization or change credentials by
means of servlet init params: `disableAuthorization`, `username`, and `password`. For instance,

```xml
    <servlet>
        <servlet-name>MonitoringCenterServlet</servlet-name>
        <servlet-class>net.centro.rtb.monitoringcenter.MonitoringCenterServlet</servlet-class>
        <load-on-startup>4</load-on-startup>
        <init-param>
            <param-name>username</param-name>
            <param-value>admin</param-value>
        </init-param>
        <init-param>
            <param-name>password</param-name>
            <param-value>god</param-value>
        </init-param>
    </servlet>

    <servlet-mapping>
        <servlet-name>MonitoringCenterServlet</servlet-name>
        <url-pattern>/monitoringCenter/*</url-pattern>
    </servlet-mapping>
```

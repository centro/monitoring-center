package net.centro.rtb.monitoringcenter.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import net.centro.rtb.monitoringcenter.SeparateClassloaderTestRunner;
import net.centro.rtb.monitoringcenter.config.dto.MetricNamePostfixPolicyDto;
import net.centro.rtb.monitoringcenter.config.dto.MonitoringCenterConfigDto;
import net.centro.rtb.monitoringcenter.util.ConfigException;
import net.centro.rtb.monitoringcenter.util.ConfigFileUtil;
import org.junit.*;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

@RunWith(SeparateClassloaderTestRunner.class)
public class ConfiguratorTest {

    private static ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory())
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private static File tempFile;

    @Before
    public void setUp() throws Exception {
        tempFile = File.createTempFile("monitoringCenter", ".yaml");

        FileWriter fileWriter = new FileWriter(new File(tempFile.getAbsolutePath()));
        fileWriter.write("naming:\n" +
                "  applicationName: \"test\"\n" +
                "  datacenterName: \"test\"\n" +
                "  nodeGroupName: \"test\"\n" +
                "  nodeId: \"test\"\n" +
                "  metricNamePostfixPolicy: \"ADD_COMPOSITE_TYPES\"\n" +
                "  appendTypeToHealthCheckNames: true\n" +
                "metricReporting:\n" +
                "  graphite:\n" +
                "    enableReporter: true\n" +
                "    enableBatching: false\n" +
                "    address:\n" +
                "      host: \"www.sitescout.com\"\n" +
                "      port: 2003\n" +
                "    reportingIntervalInSeconds: 3000");
        fileWriter.close();
    }

    @After
    public void cleanUp() throws Exception {
        Assert.assertTrue(tempFile.delete());
        new File(tempFile.getParent() + "/monitoringCenter-applicationName-current.yaml").delete();
    }

    @Test
    public void configFile() throws Exception {
        MonitoringCenterConfig monitoringCenterConfig = Configurator.configFile(new File(tempFile.getAbsolutePath()))
                .applicationName("applicationName")
                .datacenterName("datacenterName")
                .nodeGroupName("nodeGroupName")
                .nodeId("nodeId")
                .enableSystemMetrics(true)
                .enableTomcatMetrics(false)
                .metricNamePostfixPolicy(MetricNamePostfixPolicy.ADD_COMPOSITE_TYPES)
                .appendTypeToHealthCheckNames(true)
                .graphiteReporterConfig(GraphiteReporterConfig.builder()
                        .enableReporter(true)
                        .enableBatching(false)
                        .startsWithFilters(new HashSet<String>() {{
                            add("test.*");
                            add("system.");
                        }})
                        .blockedStartsWithFilters(new HashSet<String>() {{
                            add("jvm.*");
                        }})
                        .reportingInterval((long) 30000, TimeUnit.SECONDS)
                        .address("0.0.0.0", 80)
                        .build())
                .build();

        Assert.assertNotNull(monitoringCenterConfig.getConfigFile());

        ConfigFileUtil.createEffectiveConfigFile(monitoringCenterConfig);

        MonitoringCenterConfigDto configFromFile = objectMapper.readValue(new File(tempFile.getParent() + "/monitoringCenter-applicationName-current.yaml"), MonitoringCenterConfigDto.class);
        Assert.assertEquals(configFromFile.getNamingConfig().getAppendTypeToHealthCheckNames(), true);
        Assert.assertEquals(configFromFile.getNamingConfig().getApplicationName(), "applicationName");
        Assert.assertEquals(configFromFile.getNamingConfig().getDatacenterName(), "datacenterName");
        Assert.assertEquals(configFromFile.getNamingConfig().getNodeGroupName(), "nodeGroupName");
        Assert.assertEquals(configFromFile.getNamingConfig().getNodeId(), "nodeId");
        Assert.assertEquals(configFromFile.getNamingConfig().getMetricNamePostfixPolicy(), MetricNamePostfixPolicyDto.ADD_COMPOSITE_TYPES);
        Assert.assertEquals(configFromFile.getMetricCollectionConfig().getEnableSystemMetrics(), true);
        Assert.assertEquals(configFromFile.getMetricCollectionConfig().getEnableTomcatMetrics(), false);
        Assert.assertEquals(configFromFile.getMetricReportingConfig().getGraphiteReporterConfig().getEnableBatching(), false);
        Assert.assertEquals(configFromFile.getMetricReportingConfig().getGraphiteReporterConfig().getEnableReporter(), true);
        Assert.assertEquals(configFromFile.getMetricReportingConfig().getGraphiteReporterConfig().getAddress(), new HostAndPort("0.0.0.0", 80));
        Assert.assertTrue(configFromFile.getMetricReportingConfig().getGraphiteReporterConfig().getStartsWithFilters().contains("system."));
        Assert.assertTrue(configFromFile.getMetricReportingConfig().getGraphiteReporterConfig().getBlockedStartsWithFilters().contains("jvm.*"));
        Assert.assertTrue(configFromFile.getMetricReportingConfig().getGraphiteReporterConfig().getReportingIntervalInSeconds() == 30000);
    }

    @Test
    public void noConfigFile() throws Exception {
        MonitoringCenterConfig monitoringCenterConfig = Configurator.noConfigFile()
                .applicationName("applicationName")
                .datacenterName("datacenterName")
                .nodeGroupName("nodeGroupName")
                .nodeId("nodeId")
                .enableSystemMetrics(true)
                .enableTomcatMetrics(false)
                .metricNamePostfixPolicy(MetricNamePostfixPolicy.ADD_COMPOSITE_TYPES)
                .appendTypeToHealthCheckNames(true)
                .graphiteReporterConfig(GraphiteReporterConfig.builder()
                        .enableReporter(true)
                        .enableBatching(false)
                        .startsWithFilters(new HashSet<String>() {{
                            add("test.*");
                            add("system.");
                        }})
                        .blockedStartsWithFilters(new HashSet<String>() {{
                            add("jvm.*");
                        }})
                        .reportingInterval((long) 30000, TimeUnit.SECONDS)
                        .address("0.0.0.0", 80)
                        .build())
                .build();

        Assert.assertNull(monitoringCenterConfig.getConfigFile());
        Assert.assertEquals(monitoringCenterConfig.getNamingConfig().isAppendTypeToHealthCheckNames(), true);
        Assert.assertEquals(monitoringCenterConfig.getNamingConfig().getApplicationName(), "applicationName");
        Assert.assertEquals(monitoringCenterConfig.getNamingConfig().getDatacenterName(), "datacenterName");
        Assert.assertEquals(monitoringCenterConfig.getNamingConfig().getNodeGroupName(), "nodeGroupName");
        Assert.assertEquals(monitoringCenterConfig.getNamingConfig().getNodeId(), "nodeId");
        Assert.assertEquals(monitoringCenterConfig.getNamingConfig().getMetricNamePostfixPolicy(), MetricNamePostfixPolicy.ADD_COMPOSITE_TYPES);
        Assert.assertEquals(monitoringCenterConfig.getMetricCollectionConfig().isEnableSystemMetrics(), true);
        Assert.assertEquals(monitoringCenterConfig.getMetricCollectionConfig().isEnableTomcatMetrics(), false);
        Assert.assertEquals(monitoringCenterConfig.getMetricReportingConfig().getGraphiteReporterConfig().isEnableBatching(), false);
        Assert.assertEquals(monitoringCenterConfig.getMetricReportingConfig().getGraphiteReporterConfig().isEnableReporter(), true);
        Assert.assertEquals(monitoringCenterConfig.getMetricReportingConfig().getGraphiteReporterConfig().getAddress(), new HostAndPort("0.0.0.0", 80));
        Assert.assertTrue(monitoringCenterConfig.getMetricReportingConfig().getGraphiteReporterConfig().getStartsWithFilters().contains("system."));
        Assert.assertTrue(monitoringCenterConfig.getMetricReportingConfig().getGraphiteReporterConfig().getBlockedStartsWithFilters().contains("jvm.*"));
        Assert.assertTrue(monitoringCenterConfig.getMetricReportingConfig().getGraphiteReporterConfig().getReportingIntervalInSeconds() == 30000);
    }

    @Test
    public void defaultConfigFileFailed() throws Exception {
        try {
            Configurator.defaultConfigFile().build();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof ConfigException);
        }
    }
}

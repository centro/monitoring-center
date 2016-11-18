package net.centro.rtb.monitoringcenter;

import net.centro.rtb.monitoringcenter.config.Configurator;
import net.centro.rtb.monitoringcenter.config.GraphiteReporterConfig;
import net.centro.rtb.monitoringcenter.config.MetricNamePostfixPolicy;
import net.centro.rtb.monitoringcenter.config.MonitoringCenterConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

@RunWith(SeparateClassloaderTestRunner.class)
public class ReloadConfigTest {
    @Test
    public void reloadConfigure() throws Exception {
        File tempFile = File.createTempFile("monitoringCenter", ".yaml");

        FileWriter fileWriter = new FileWriter(new File(tempFile.getAbsolutePath()));
        fileWriter.write("{}");
        fileWriter.close();

        MonitoringCenterConfig monitoringCenterConfig = Configurator.configFile(new File(tempFile.getAbsolutePath()))
                .applicationName("applicationName")
                .metricNamePostfixPolicy(MetricNamePostfixPolicy.ADD_COMPOSITE_TYPES)
                .enableSystemMetrics(true)
                .enableTomcatMetrics(true)
                .appendTypeToHealthCheckNames(true)
                .graphiteReporterConfig(GraphiteReporterConfig.builder()
                        .enableReporter(true)
                        .enableBatching(false)
                        .reportingInterval((long) 30000, TimeUnit.SECONDS)
                        .address("0.0.0.0", 80)
                        .build())
                .build();

        MonitoringCenter.configure(monitoringCenterConfig);

        Assert.assertTrue(MonitoringCenter.getMetricsByNames().size() > 0);

        PrintWriter writer = new PrintWriter(new File(tempFile.getParent() + "/monitoringCenter-applicationName-current.yaml"));
        writer.print("naming:\n" +
                "  applicationName: \"test\"\n" +
                "  datacenterName: \"test\"\n" +
                "  nodeGroupName: \"test\"\n" +
                "  nodeId: \"test\"\n" +
                "  metricNamePostfixPolicy: \"ADD_COMPOSITE_TYPES\"\n" +
                "  appendTypeToHealthCheckNames: true");
        writer.close();

        MonitoringCenter.reloadConfig();

        Assert.assertEquals(0, MonitoringCenter.getMetricsByNames().size());

        try {
            MonitoringCenter.configure(monitoringCenterConfig);
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalStateException);
        }

        Assert.assertTrue(tempFile.delete());
        Assert.assertTrue(new File(tempFile.getParent() + "/monitoringCenter-applicationName-current.yaml").delete());
    }
}

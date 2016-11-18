package net.centro.rtb.monitoringcenter.util;

import net.centro.rtb.monitoringcenter.config.Configurator;
import net.centro.rtb.monitoringcenter.config.GraphiteReporterConfig;
import net.centro.rtb.monitoringcenter.config.MetricNamePostfixPolicy;
import net.centro.rtb.monitoringcenter.config.MonitoringCenterConfig;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.util.concurrent.TimeUnit;

public class ConfigFileUtilTest {
    @Test
    public void createEffectiveConfigFile() throws Exception {

        File tempFile = File.createTempFile("monitoringCenter", ".yaml");
        FileWriter fileWriter = new FileWriter(new File(tempFile.getAbsolutePath()));
        fileWriter.write("{}");
        fileWriter.close();

        MonitoringCenterConfig monitoringCenterConfig = Configurator.configFile(new File(tempFile.getAbsolutePath()))
                .applicationName("applicationName")
                .enableSystemMetrics(true)
                .metricNamePostfixPolicy(MetricNamePostfixPolicy.ADD_COMPOSITE_TYPES)
                .appendTypeToHealthCheckNames(true)
                .graphiteReporterConfig(GraphiteReporterConfig.builder()
                        .enableReporter(true)
                        .reportingInterval((long) 30000, TimeUnit.SECONDS)
                        .address("0.0.0.0", 80)
                        .build())
                .build();

        ConfigFileUtil.createEffectiveConfigFile(monitoringCenterConfig);

        MonitoringCenterConfig monitoringCenterConfigModified = Configurator.configFile(new File(tempFile.getParent() + "/monitoringCenter-applicationName-current.yaml")).build();
        Assert.assertEquals(monitoringCenterConfig.toString(), monitoringCenterConfigModified.toString());

        Assert.assertTrue(tempFile.delete());
        Assert.assertTrue(new File(tempFile.getParent() + "/monitoringCenter-applicationName-current.yaml").delete());
    }
}
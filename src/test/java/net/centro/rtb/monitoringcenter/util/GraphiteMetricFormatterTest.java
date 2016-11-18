package net.centro.rtb.monitoringcenter.util;

import com.codahale.metrics.*;
import net.centro.rtb.monitoringcenter.MetricCollector;
import net.centro.rtb.monitoringcenter.MonitoringCenter;
import net.centro.rtb.monitoringcenter.MonitoringCenterTest;
import net.centro.rtb.monitoringcenter.SeparateClassloaderTestRunner;
import net.centro.rtb.monitoringcenter.config.Configurator;
import net.centro.rtb.monitoringcenter.config.GraphiteReporterConfig;
import net.centro.rtb.monitoringcenter.config.MetricNamePostfixPolicy;
import net.centro.rtb.monitoringcenter.config.MonitoringCenterConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(SeparateClassloaderTestRunner.class)
public class GraphiteMetricFormatterTest {
    @Test
    public void format() throws Exception {
        MonitoringCenterConfig monitoringCenterConfig = Configurator.noConfigFile()
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

        MetricCollector metricCollector = MonitoringCenter.getMetricCollector(MonitoringCenterTest.class);

        Meter testMeter = new Meter();
        metricCollector.registerMetric(testMeter, "testMeter");
        testMeter.mark();

        Histogram testHistogram = new Histogram(new ExponentiallyDecayingReservoir());
        metricCollector.registerMetric(testHistogram, "testHistogram");
        testHistogram.update(3000);

        Timer testTimer = new Timer();
        metricCollector.registerMetric(testTimer, "testTimer");
        testTimer.update(3000, TimeUnit.SECONDS);

        Counter testCounter = new Counter();
        metricCollector.registerMetric(testCounter, "testCounter");
        testCounter.inc();

        final AtomicInteger counter = new AtomicInteger();

        metricCollector.registerGauge(new Gauge() {
            @Override
            public Object getValue() {
                return counter;
            }
        }, "testGauge");
        counter.getAndIncrement();

        GraphiteMetricFormatter graphiteMetricFormatter = new GraphiteMetricFormatter(TimeUnit.SECONDS, TimeUnit.MICROSECONDS);
        SortedMap<String, Metric> metricSortedMap = MonitoringCenter.getMetricsByNames();
        Assert.assertNotNull(graphiteMetricFormatter.format(metricSortedMap));
        String[] result = graphiteMetricFormatter.format(metricSortedMap).split("\\n");
        Set<String> metricSet = new HashSet<>();
        for (String s : result) {
            metricSet.add(s.split(" ")[0]);
            Assert.assertEquals(3, s.split(" ").length);
        }
        Assert.assertEquals(result.length, metricSet.size());
    }
}
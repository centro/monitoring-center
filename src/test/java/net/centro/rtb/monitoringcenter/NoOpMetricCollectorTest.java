package net.centro.rtb.monitoringcenter;

import com.codahale.metrics.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(SeparateClassloaderTestRunner.class)
public class NoOpMetricCollectorTest {
    @Test
    public void getMetricCollector() throws Exception {
        MetricCollector metricCollector = MonitoringCenter.getMetricCollector(MonitoringCenterTest.class);
        Assert.assertTrue(metricCollector instanceof NoOpMetricCollector);
    }

    @Test
    public void getCounter() throws Exception {
        MetricCollector metricCollector = MonitoringCenter.getMetricCollector(MonitoringCenterTest.class);

        Counter testCounter = new Counter();
        metricCollector.registerMetric(testCounter, "testCounter");
        testCounter.inc();

        Assert.assertEquals(0, metricCollector.getCounter("testCounter").getCount());
    }

    @Test
    public void getTimer() throws Exception {
        MetricCollector metricCollector = MonitoringCenter.getMetricCollector(MonitoringCenterTest.class);

        Timer testTimer = new Timer();
        metricCollector.registerMetric(testTimer, "testTimer");
        testTimer.update(3000, TimeUnit.SECONDS);

        Assert.assertEquals(0, metricCollector.getTimer("testTimer").getCount());
    }

    @Test
    public void getMeter() throws Exception {
        MetricCollector metricCollector = MonitoringCenter.getMetricCollector(MonitoringCenterTest.class);

        Meter testMeter = new Meter();
        metricCollector.registerMetric(testMeter, "testMeter");

        Assert.assertEquals(0, metricCollector.getMeter("testMeter").getCount());
    }

    @Test
    public void getHistogram() throws Exception {
        MetricCollector metricCollector = MonitoringCenter.getMetricCollector(MonitoringCenterTest.class);

        Histogram testHistogram = new Histogram(new ExponentiallyDecayingReservoir());
        metricCollector.registerMetric(testHistogram, "testHistogram");
        testHistogram.update(3000);

        Assert.assertEquals(0, metricCollector.getHistogram("testHistogram").getCount());
    }

    @Test
    public void registerGauge() throws Exception {
        MetricCollector metricCollector = MonitoringCenter.getMetricCollector(MonitoringCenterTest.class);

        final AtomicInteger counter = new AtomicInteger();

        metricCollector.registerGauge(new Gauge() {
            @Override
            public Object getValue() {
                return counter;
            }
        }, "test");

        metricCollector.registerGauge(new Gauge() {
            @Override
            public Object getValue() {
                return counter;
            }
        }, "test");
    }

    @Test
    public void registerMetric() throws Exception {
        MetricCollector metricCollector = MonitoringCenter.getMetricCollector(MonitoringCenterTest.class);

        metricCollector.registerMetric(new Timer(), "testTimer");

        metricCollector.registerMetric(new Timer(), "testTimer");
    }

    @Test
    public void registerMetricSet() throws Exception {
        MetricCollector metricCollector = MonitoringCenter.getMetricCollector(MonitoringCenterTest.class);

        MetricSet metricSet = new MetricSet() {
            @Override
            public Map<String, Metric> getMetrics() {
                Map<String, Metric> metricMap = new HashMap<>();
                metricMap.put("test", new Timer());
                return metricMap;
            }
        };
        metricCollector.registerMetricSet(metricSet, "testTimerSet");
        metricCollector.registerMetricSet(metricSet, "testTimerSet");
    }
}
package net.centro.rtb.monitoringcenter;

import com.codahale.metrics.*;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import net.centro.rtb.monitoringcenter.config.Configurator;
import net.centro.rtb.monitoringcenter.config.GraphiteReporterConfig;
import net.centro.rtb.monitoringcenter.config.MetricNamePostfixPolicy;
import net.centro.rtb.monitoringcenter.config.MonitoringCenterConfig;
import net.centro.rtb.monitoringcenter.metrics.C3P0PooledDataSourceMetricSet;
import net.centro.rtb.monitoringcenter.metrics.GuavaCacheMetricSet;
import org.junit.*;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(SeparateClassloaderTestRunner.class)
public class MetricCollectorImplTest {
    @BeforeClass
    public static void setUp() {
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
    }

    @AfterClass
    public static void cleanUp() {
        MonitoringCenter.shutdown();
    }

    @Test
    public void getCounter() throws Exception {
        MetricCollector metricCollector = MonitoringCenter.getMetricCollector(MonitoringCenterTest.class);

        Counter testCounter = new Counter();
        metricCollector.registerMetric(testCounter, "testCounter");
        testCounter.inc();

        Assert.assertEquals(1, metricCollector.getCounter("testCounter").getCount());
        metricCollector.removeAll();
    }

    @Test
    public void getTimer() throws Exception {
        MetricCollector metricCollector = MonitoringCenter.getMetricCollector(MonitoringCenterTest.class);

        Timer testTimer = new Timer();
        metricCollector.registerMetric(testTimer, "testTimer");
        testTimer.update(3000, TimeUnit.SECONDS);

        Assert.assertEquals(1, metricCollector.getTimer("testTimer").getCount());
        metricCollector.removeAll();
    }

    @Test
    public void getMeter() throws Exception {
        MetricCollector metricCollector = MonitoringCenter.getMetricCollector(MonitoringCenterTest.class);

        Meter testMeter = new Meter();
        metricCollector.registerMetric(testMeter, "testMeter");
        testMeter.mark();

        Assert.assertEquals(1, metricCollector.getMeter("testMeter").getCount());
        metricCollector.removeAll();
    }

    @Test
    public void getHistogram() throws Exception {
        MetricCollector metricCollector = MonitoringCenter.getMetricCollector(MonitoringCenterTest.class);

        Histogram testHistogram = new Histogram(new ExponentiallyDecayingReservoir());
        metricCollector.registerMetric(testHistogram, "testHistogram");
        testHistogram.update(3000);

        Assert.assertEquals(1, metricCollector.getHistogram("testHistogram").getCount());
        metricCollector.removeAll();
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

        try {
            metricCollector.registerGauge(new Gauge() {
                @Override
                public Object getValue() {
                    return counter;
                }
            }, "test");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
        metricCollector.removeAll();
    }

    @Test
    public void registerMetric() throws Exception {
        MetricCollector metricCollector = MonitoringCenter.getMetricCollector(MonitoringCenterTest.class);

        metricCollector.registerMetric(new Timer(), "testTimer");

        try {
            metricCollector.registerMetric(new Timer(), "testTimer");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
        metricCollector.removeAll();
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

        try {
            metricCollector.registerMetricSet(metricSet, "testTimerSet");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
        metricCollector.removeAll();
    }

    @Test
    public void removeAll() throws Exception {
        MetricCollector metricCollector = MonitoringCenter.getMetricCollector(MonitoringCenterTest.class);

        Timer testTimer = new Timer();
        metricCollector.registerMetric(testTimer, "testTimer");
        Counter testCounter = new Counter();
        metricCollector.registerMetric(testCounter, "testCounter");
        testCounter.inc();
        testTimer.update(2000, TimeUnit.MILLISECONDS);

        Assert.assertEquals(2, metricCollector.getTimer("testTimer").getCount() + metricCollector.getCounter("testCounter").getCount());

        metricCollector.removeAll();

        Assert.assertEquals(0, metricCollector.getTimer("testTimer").getCount() + metricCollector.getCounter("testCounter").getCount());
    }

    @Test
    public void removeMetric() throws Exception {
        MetricCollector metricCollector = MonitoringCenter.getMetricCollector(MonitoringCenterTest.class);

        Timer testTimer = new Timer();
        metricCollector.registerMetric(testTimer, "testTimer");
        Counter testCounter = new Counter();
        metricCollector.registerMetric(testCounter, "testCounter");
        testCounter.inc();
        testTimer.update(2000, TimeUnit.MILLISECONDS);

        Assert.assertEquals(2, metricCollector.getTimer("testTimer").getCount() + metricCollector.getCounter("testCounter").getCount());

        metricCollector.removeMetric(testCounter, "testCounter");

        Assert.assertEquals(1, metricCollector.getTimer("testTimer").getCount());
        Assert.assertEquals(0, metricCollector.getCounter("testCounter").getCount());
        metricCollector.removeAll();
    }

    @Test
    public void removeMetricSet() throws Exception {
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

        metricCollector.removeMetricSet(metricSet, "testTimerSet");

        metricCollector.registerMetricSet(metricSet, "testTimerSet");

        metricCollector.removeAll();
    }

    @Test
    public void replaceMetric() throws Exception {
        MetricCollector metricCollector = MonitoringCenter.getMetricCollector(MonitoringCenterTest.class);

        Timer testTimer = new Timer();
        metricCollector.registerMetric(testTimer, "test");
        testTimer.update(2000, TimeUnit.MILLISECONDS);

        Assert.assertEquals(1, metricCollector.getTimer("test").getCount());
        Assert.assertEquals(0, metricCollector.getCounter("test").getCount());

        Timer testTimer2 = new Timer();
        metricCollector.replaceMetric(testTimer2, "test");
        testTimer2.update(2000, TimeUnit.MILLISECONDS);
        testTimer2.update(1000, TimeUnit.MILLISECONDS);
        testTimer2.update(1000, TimeUnit.MILLISECONDS);

        Counter testCounter = new Counter();
        metricCollector.replaceMetric(testCounter, "test");
        testCounter.inc();

        Assert.assertEquals(3, metricCollector.getTimer("test").getCount());
        Assert.assertEquals(1, metricCollector.getCounter("test").getCount());

        metricCollector.removeAll();
    }

    @Test
    public void registerGuavaCache() throws Exception {
        MetricCollector metricCollector = MonitoringCenter.getMetricCollector(MonitoringCenterTest.class);

        Cache<String, String> cached = CacheBuilder.newBuilder()
                .expireAfterAccess(1, TimeUnit.MINUTES)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .recordStats()
                .build();

        metricCollector.registerGuavaCache(cached, "testCache");

        cached.put("test1", "test2");

        try {
            metricCollector.registerGuavaCache(CacheBuilder.newBuilder()
                    .expireAfterAccess(1, TimeUnit.MINUTES)
                    .expireAfterWrite(1, TimeUnit.HOURS)
                    .recordStats()
                    .build(), "testCache");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }

        String[] startsWithFilters = {"MonitoringCenterTest."};

        GuavaCacheMetricSet guavaCacheMetricSet = new GuavaCacheMetricSet(cached);

        Assert.assertEquals(guavaCacheMetricSet.getMetrics().size(), MonitoringCenter.getMetricsByNames(false, startsWithFilters).size());
    }

    @Test
    public void registerC3P0DataSource() throws Exception {
        MetricCollector metricCollector = MonitoringCenter.getMetricCollector(MonitoringCenterTest.class);

        ComboPooledDataSource cpds = new ComboPooledDataSource();
        cpds.setDriverClass("com.mysql.jdbc.Driver");

        cpds.setMinPoolSize(2);
        cpds.setAcquireIncrement(5);
        cpds.setMaxPoolSize(20);
        cpds.setMaxStatements(180);
        cpds.setBreakAfterAcquireFailure(true);

        metricCollector.registerC3P0DataSource(cpds, "testDB");

        C3P0PooledDataSourceMetricSet c3P0PooledDataSourceMetricSet = new C3P0PooledDataSourceMetricSet(cpds);

        String[] startsWithFilters = {"MonitoringCenterTest."};

        Assert.assertEquals(c3P0PooledDataSourceMetricSet.getMetrics().size(), MonitoringCenter.getMetricsByNames(false, startsWithFilters).size());
    }
}
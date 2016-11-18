package net.centro.rtb.monitoringcenter;

import com.codahale.metrics.*;
import com.codahale.metrics.health.HealthCheck;
import net.centro.rtb.monitoringcenter.config.Configurator;
import net.centro.rtb.monitoringcenter.config.GraphiteReporterConfig;
import net.centro.rtb.monitoringcenter.config.MetricNamePostfixPolicy;
import net.centro.rtb.monitoringcenter.config.MonitoringCenterConfig;
import org.junit.*;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(SeparateClassloaderTestRunner.class)
public class MonitoringCenterTest {

    private static MonitoringCenterConfig monitoringCenterConfig;

    @BeforeClass
    public static void setUp() {
        monitoringCenterConfig = Configurator.noConfigFile()
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
    public void configure() throws Exception {
        try {
            MonitoringCenter.configure(monitoringCenterConfig);
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalStateException);
        }
    }

    @Test
    public void getMetricCollector() throws Exception {
        MetricCollector metricCollector = MonitoringCenter.getMetricCollector(MonitoringCenterTest.class);
        Assert.assertNotNull(metricCollector);

        metricCollector = MonitoringCenter.getMetricCollector("Test");
        Assert.assertNotNull(metricCollector);

        metricCollector = MonitoringCenter.getDatabaseMetricCollector("Test");
        Assert.assertNotNull(metricCollector);

        try {
            MonitoringCenter.getMetricCollector("");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }

        try {
            MonitoringCenter.getMetricCollector("system");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void getCountersByNames() throws Exception {
        MetricCollector metricCollector = MonitoringCenter.getMetricCollector(MonitoringCenterTest.class);

        Counter testCounter = new Counter();
        metricCollector.registerMetric(testCounter, "test");
        testCounter.inc();

        Assert.assertEquals(1, MonitoringCenter.getCountersByNames().get("MonitoringCenterTest.test").getCount());
        Assert.assertEquals(1, MonitoringCenter.getCountersByNames(true).get("applicationName.none.none.none.MonitoringCenterTest.test").getCount());

        String[] startsWithFilters = {"MonitoringCenterTest."};
        Assert.assertEquals(1, MonitoringCenter.getCountersByNames(false, startsWithFilters).size());
        Assert.assertEquals(1, MonitoringCenter.getCountersByNames(false, startsWithFilters).get("MonitoringCenterTest.test").getCount());
        MonitoringCenter.removeAllMetrics();
    }

    @Test
    public void getMetersByNames() throws Exception {
        MetricCollector metricCollector = MonitoringCenter.getMetricCollector(MonitoringCenterTest.class);

        Meter testMeter = new Meter();
        metricCollector.registerMetric(testMeter, "test");
        testMeter.mark();

        Assert.assertEquals(1, MonitoringCenter.getMetersByNames().get("MonitoringCenterTest.testMeter").getCount());
        Assert.assertEquals(1, MonitoringCenter.getMetersByNames(true).get("applicationName.none.none.none.MonitoringCenterTest.testMeter").getCount());

        String[] startsWithFilters = {"MonitoringCenterTest."};
        Assert.assertEquals(1, MonitoringCenter.getMetersByNames(false, startsWithFilters).size());
        Assert.assertEquals(1, MonitoringCenter.getMetersByNames(false, startsWithFilters).get("MonitoringCenterTest.testMeter").getCount());
        MonitoringCenter.removeAllMetrics();
    }

    @Test
    public void getHistogramsByNames() throws Exception {
        MetricCollector metricCollector = MonitoringCenter.getMetricCollector(MonitoringCenterTest.class);

        Histogram testHistogram = new Histogram(new ExponentiallyDecayingReservoir());
        metricCollector.registerMetric(testHistogram, "test");
        testHistogram.update(3000);

        Assert.assertEquals(1, MonitoringCenter.getHistogramsByNames().get("MonitoringCenterTest.testHistogram").getCount());
        Assert.assertEquals(1, MonitoringCenter.getHistogramsByNames(true).get("applicationName.none.none.none.MonitoringCenterTest.testHistogram").getCount());

        String[] startsWithFilters = {"MonitoringCenterTest."};
        Assert.assertEquals(1, MonitoringCenter.getHistogramsByNames(false, startsWithFilters).size());
        Assert.assertEquals(1, MonitoringCenter.getHistogramsByNames(false, startsWithFilters).get("MonitoringCenterTest.testHistogram").getCount());
        MonitoringCenter.removeAllMetrics();
    }

    @Test
    public void getTimersByNames() throws Exception {
        MetricCollector metricCollector = MonitoringCenter.getMetricCollector(MonitoringCenterTest.class);

        Timer testTimer = new Timer();
        metricCollector.registerMetric(testTimer, "test");
        testTimer.update(3000, TimeUnit.SECONDS);

        Assert.assertEquals(1, MonitoringCenter.getTimersByNames().get("MonitoringCenterTest.testTimer").getCount());
        Assert.assertEquals(1, MonitoringCenter.getTimersByNames(true).get("applicationName.none.none.none.MonitoringCenterTest.testTimer").getCount());

        String[] startsWithFilters = {"MonitoringCenterTest."};
        Assert.assertEquals(1, MonitoringCenter.getTimersByNames(false, startsWithFilters).size());
        Assert.assertEquals(1, MonitoringCenter.getTimersByNames(false, startsWithFilters).get("MonitoringCenterTest.testTimer").getCount());
        MonitoringCenter.removeAllMetrics();
    }

    @Test
    public void getGaugesByNames() throws Exception {
        MetricCollector metricCollector = MonitoringCenter.getMetricCollector(MonitoringCenterTest.class);

        final AtomicInteger counter = new AtomicInteger();

        metricCollector.registerGauge(new Gauge() {
            @Override
            public Object getValue() {
                return counter;
            }
        }, "test");

        counter.getAndIncrement();

        Assert.assertTrue(MonitoringCenter.getGaugesByNames().get("MonitoringCenterTest.test").getValue().toString().equals("1"));
        Assert.assertTrue(MonitoringCenter.getGaugesByNames(true).get("applicationName.none.none.none.MonitoringCenterTest.test").getValue().toString().equals("1"));

        String[] startsWithFilters = {"MonitoringCenterTest."};
        Assert.assertEquals(1, MonitoringCenter.getGaugesByNames(false, startsWithFilters).size());
        Assert.assertTrue(MonitoringCenter.getGaugesByNames(false, startsWithFilters).get("MonitoringCenterTest.test").getValue().toString().equals("1"));
        MonitoringCenter.removeAllMetrics();
    }

    @Test
    public void getMetricsByNames() throws Exception {
        MetricCollector metricCollector = MonitoringCenter.getMetricCollector(MonitoringCenterTest.class);

        Timer testTimer = new Timer();
        metricCollector.registerMetric(testTimer, "test");
        testTimer.update(3000, TimeUnit.SECONDS);

        Assert.assertEquals(1, ((Timer) MonitoringCenter.getMetricsByNames().get("MonitoringCenterTest.testTimer")).getCount());
        Assert.assertEquals(1, ((Timer) MonitoringCenter.getMetricsByNames(true).get("applicationName.none.none.none.MonitoringCenterTest.testTimer")).getCount());

        String[] startsWithFilters = {"MonitoringCenterTest."};
        Assert.assertEquals(1, MonitoringCenter.getMetricsByNames(false, startsWithFilters).size());
        Assert.assertEquals(1, ((Timer) MonitoringCenter.getMetricsByNames(false, startsWithFilters).get("MonitoringCenterTest.testTimer")).getCount());
        MonitoringCenter.removeAllMetrics();
    }

    @Test
    public void removeAllMetrics() throws Exception {
        MonitoringCenter.removeAllMetrics();
        Assert.assertTrue(MonitoringCenter.getMetricsByNames().size() == 0);
    }

    @Test
    public void registerHealthCheck() throws Exception {
        final boolean isOk = new Date().getTime() % 2 == 0;

        MonitoringCenter.registerHealthCheck("HealthCheckTest", new HealthCheck() {
            @Override
            protected Result check() throws Exception {
                if (!isOk) {
                    return Result.unhealthy("This second is not even!");
                }

                return Result.healthy("Even!");
            }
        });

        MonitoringCenter.registerHealthCheck("HealthCheckTest", new HealthCheck() {
            @Override
            protected Result check() throws Exception {
                if (isOk) {
                    return Result.unhealthy("This second is not odd!");
                }

                return Result.healthy("Odd!");
            }
        });

        Assert.assertEquals(1, MonitoringCenter.runHealthChecks().size());
        HealthCheck.Result healthCheck = MonitoringCenter.runHealthChecks().get("HealthCheckTestHealthCheck");
        if (healthCheck.isHealthy()) {
            Assert.assertEquals("Even!", healthCheck.getMessage());
        } else {
            Assert.assertEquals("This second is not even!", healthCheck.getMessage());
        }
        MonitoringCenter.removeAllHealthChecks();
    }

    @Test
    public void runHealthChecks() throws Exception {
        final boolean isOk = new Date().getTime() % 2 == 0;

        MonitoringCenter.registerHealthCheck("HealthCheckTest1", new HealthCheck() {
            @Override
            protected Result check() throws Exception {
                if (!isOk) {
                    return Result.unhealthy("This second is not even!");
                }

                return Result.healthy("Even!");
            }
        });

        MonitoringCenter.registerHealthCheck("HealthCheckTest2", new HealthCheck() {
            @Override
            protected Result check() throws Exception {
                if (isOk) {
                    return Result.unhealthy("This second is not odd!");
                }

                return Result.healthy("Odd!");
            }
        });

        Assert.assertEquals(2, MonitoringCenter.runHealthChecks().size());
        HealthCheck.Result healthCheck1 = MonitoringCenter.runHealthChecks().get("HealthCheckTest1HealthCheck");
        if (healthCheck1.isHealthy()) {
            Assert.assertEquals("Even!", healthCheck1.getMessage());
        } else {
            Assert.assertEquals("This second is not even!", healthCheck1.getMessage());
        }
        HealthCheck.Result healthCheck2 = MonitoringCenter.runHealthChecks().get("HealthCheckTest2HealthCheck");
        if (healthCheck2.isHealthy()) {
            Assert.assertEquals("Odd!", healthCheck2.getMessage());
        } else {
            Assert.assertEquals("This second is not odd!", healthCheck2.getMessage());
        }
        MonitoringCenter.removeAllHealthChecks();
    }

    @Test
    public void runHealthCheck() throws Exception {
        final boolean isOk = new Date().getTime() % 2 == 0;

        MonitoringCenter.registerHealthCheck("HealthCheckTest1", new HealthCheck() {
            @Override
            protected Result check() throws Exception {
                if (!isOk) {
                    return Result.unhealthy("This second is not even!");
                }

                return Result.healthy("Even!");
            }
        });

        HealthCheck.Result healthCheck = MonitoringCenter.runHealthCheck("HealthCheckTest1HealthCheck");
        if (healthCheck.isHealthy()) {
            Assert.assertEquals("Even!", healthCheck.getMessage());
        } else {
            Assert.assertEquals("This second is not even!", healthCheck.getMessage());
        }
        MonitoringCenter.removeAllHealthChecks();
    }

    @Test
    public void removeAllHealthChecks() throws Exception {
        final boolean isOk = new Date().getTime() % 2 == 0;

        MonitoringCenter.registerHealthCheck("HealthCheckTest1", new HealthCheck() {
            @Override
            protected Result check() throws Exception {
                if (!isOk) {
                    return Result.unhealthy("This second is not even!");
                }

                return Result.healthy("Even!");
            }
        });

        MonitoringCenter.registerHealthCheck("HealthCheckTest2", new HealthCheck() {
            @Override
            protected Result check() throws Exception {
                if (isOk) {
                    return Result.unhealthy("This second is not odd!");
                }

                return Result.healthy("Odd!");
            }
        });

        MonitoringCenter.removeAllHealthChecks();

        Assert.assertEquals(0, MonitoringCenter.runHealthChecks().size());
        Assert.assertNull(MonitoringCenter.runHealthChecks().get("HealthCheckTest1HealthCheck"));
        try {
            MonitoringCenter.runHealthCheck("HealthCheckTest1HealthCheck");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof NoSuchElementException);
        }
    }

    @Test
    public void removeHealthCheck() throws Exception {
        final boolean isOk = new Date().getTime() % 2 == 0;

        MonitoringCenter.registerHealthCheck("HealthCheckTest1", new HealthCheck() {
            @Override
            protected Result check() throws Exception {
                if (!isOk) {
                    return Result.unhealthy("This second is not even!");
                }

                return Result.healthy("Even!");
            }
        });

        MonitoringCenter.registerHealthCheck("HealthCheckTest2", new HealthCheck() {
            @Override
            protected Result check() throws Exception {
                if (isOk) {
                    return Result.unhealthy("This second is not odd!");
                }

                return Result.healthy("Odd!");
            }
        });

        MonitoringCenter.removeHealthCheck("HealthCheckTest1HealthCheck");

        Assert.assertEquals(1, MonitoringCenter.runHealthChecks().size());
        Assert.assertNull(MonitoringCenter.runHealthChecks().get("HealthCheckTest1HealthCheck"));
        try {
            MonitoringCenter.runHealthCheck("HealthCheckTest1HealthCheck");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof NoSuchElementException);
        }
        Assert.assertNotNull(MonitoringCenter.runHealthCheck("HealthCheckTest2HealthCheck"));
        MonitoringCenter.removeAllHealthChecks();
    }

    @Test
    public void getSystemStatus() throws Exception {
        Assert.assertNotNull(MonitoringCenter.getSystemStatus());
        Assert.assertNotNull(MonitoringCenter.getSystemStatus().getJvmStatus());
        Assert.assertNotNull(MonitoringCenter.getSystemStatus().getOperatingSystemStatus());
    }

    @Test
    public void getTomcatStatus() throws Exception {
        Assert.assertNotNull(MonitoringCenter.getTomcatStatus());
        Assert.assertNotNull(MonitoringCenter.getTomcatStatus().getConnectorStatuses());
        Assert.assertNotNull(MonitoringCenter.getTomcatStatus().getExecutorStatuses());
    }

    @Test
    public void getSystemInfo() throws Exception {
        Assert.assertNotNull(MonitoringCenter.getSystemInfo());
        Assert.assertNotNull(MonitoringCenter.getSystemInfo().getJvmInfo());
        Assert.assertNotNull(MonitoringCenter.getSystemInfo().getOperatingSystemInfo());
    }

    @Test
    public void getNodeInfo() throws Exception {
        Assert.assertNotNull(MonitoringCenter.getNodeInfo());
        Assert.assertTrue(MonitoringCenter.getNodeInfo().getDatacenterName().equals("none"));
        Assert.assertTrue(MonitoringCenter.getNodeInfo().getNodeGroupName().equals("none"));
        Assert.assertTrue(MonitoringCenter.getNodeInfo().getNodeId().equals("none"));
    }

    @Test
    public void getAppInfo() throws Exception {
        Assert.assertNotNull(MonitoringCenter.getNodeInfo());
        Assert.assertTrue(MonitoringCenter.getAppInfo().getApplicationName().equals("applicationName"));
    }

    @Test
    public void shutdown() throws Exception {
        MonitoringCenter.shutdown();
    }
}
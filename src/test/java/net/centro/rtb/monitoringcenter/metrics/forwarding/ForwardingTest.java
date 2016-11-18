package net.centro.rtb.monitoringcenter.metrics.forwarding;

import com.codahale.metrics.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ForwardingTest {
    @Test
    public void CompositeForwardingCounter() throws Exception {
        final Map<String, Counter> metricsByNames = new HashMap<>();

        Counter testCounter1 = new Counter();
        Counter testCounter2 = new Counter();
        Counter testCounter3 = new Counter();

        metricsByNames.put("testCounter1", testCounter1);
        metricsByNames.put("testCounter2", testCounter2);
        metricsByNames.put("testCounter3", testCounter3);

        testCounter1.inc();
        testCounter2.inc(2);

        Assert.assertEquals(1, metricsByNames.get("testCounter1").getCount());
        Assert.assertEquals(2, metricsByNames.get("testCounter2").getCount());
        Assert.assertEquals(0, metricsByNames.get("testCounter3").getCount());

        CompositeForwardingCounter compositeForwardingCounter = new CompositeForwardingCounter(testCounter3, new MetricProvider<Counter>() {
            @Override
            public Counter get() {
                return metricsByNames.get("testCounter1");
            }
        });

        compositeForwardingCounter.inc(4);
        compositeForwardingCounter.inc();

        Assert.assertEquals(6, metricsByNames.get("testCounter1").getCount());
        Assert.assertEquals(2, metricsByNames.get("testCounter2").getCount());
        Assert.assertEquals(5, metricsByNames.get("testCounter3").getCount());
        Assert.assertEquals(5, compositeForwardingCounter.getCount());

        testCounter1.inc();
        testCounter3.inc();

        Assert.assertEquals(7, metricsByNames.get("testCounter1").getCount());
        Assert.assertEquals(2, metricsByNames.get("testCounter2").getCount());
        Assert.assertEquals(6, metricsByNames.get("testCounter3").getCount());
        Assert.assertEquals(6, compositeForwardingCounter.getCount());

        testCounter1.dec(4);
        testCounter2.dec();

        Assert.assertEquals(3, metricsByNames.get("testCounter1").getCount());
        Assert.assertEquals(1, metricsByNames.get("testCounter2").getCount());
        Assert.assertEquals(6, metricsByNames.get("testCounter3").getCount());
        Assert.assertEquals(6, compositeForwardingCounter.getCount());

        compositeForwardingCounter.dec(2);
        compositeForwardingCounter.dec();

        Assert.assertEquals(0, metricsByNames.get("testCounter1").getCount());
        Assert.assertEquals(1, metricsByNames.get("testCounter2").getCount());
        Assert.assertEquals(3, metricsByNames.get("testCounter3").getCount());
        Assert.assertEquals(3, compositeForwardingCounter.getCount());
    }

    @Test
    public void CompositeForwardingHistogram() throws Exception {
        final Map<String, Histogram> metricsByNames = new HashMap<>();

        Histogram testHistogram1 = new Histogram(new ExponentiallyDecayingReservoir());
        Histogram testHistogram2 = new Histogram(new ExponentiallyDecayingReservoir());
        Histogram testHistogram3 = new Histogram(new ExponentiallyDecayingReservoir());

        metricsByNames.put("testHistogram1", testHistogram1);
        metricsByNames.put("testHistogram2", testHistogram2);
        metricsByNames.put("testHistogram3", testHistogram3);

        testHistogram1.update(1);
        testHistogram2.update(2);

        Assert.assertEquals(1, metricsByNames.get("testHistogram1").getCount());
        Assert.assertEquals(1, metricsByNames.get("testHistogram2").getCount());
        Assert.assertEquals(0, metricsByNames.get("testHistogram3").getCount());

        CompositeForwardingHistogram compositeForwardingHistogram = new CompositeForwardingHistogram(testHistogram3, new MetricProvider<Histogram>() {
            @Override
            public Histogram get() {
                return metricsByNames.get("testHistogram1");
            }
        });

        compositeForwardingHistogram.update(4);
        compositeForwardingHistogram.update(1);

        Assert.assertEquals(3, metricsByNames.get("testHistogram1").getCount());
        Assert.assertEquals(1, metricsByNames.get("testHistogram2").getCount());
        Assert.assertEquals(2, metricsByNames.get("testHistogram3").getCount());
        Assert.assertEquals(2, compositeForwardingHistogram.getCount());

        testHistogram1.update(1);
        testHistogram1.update(2);
        testHistogram3.update((long) 29000);

        Assert.assertEquals(5, metricsByNames.get("testHistogram1").getCount());
        Assert.assertEquals(1, metricsByNames.get("testHistogram2").getCount());
        Assert.assertEquals(3, metricsByNames.get("testHistogram3").getCount());
        Assert.assertEquals(3, compositeForwardingHistogram.getCount());
    }

    @Test
    public void CompositeForwardingTimer() throws Exception {
        final Map<String, Timer> metricsByNames = new HashMap<>();

        Timer testTimer1 = new Timer();
        Timer testTimer2 = new Timer();
        Timer testTimer3 = new Timer();

        metricsByNames.put("testTimer1", testTimer1);
        metricsByNames.put("testTimer2", testTimer2);
        metricsByNames.put("testTimer3", testTimer3);

        testTimer1.update(1, TimeUnit.SECONDS);
        testTimer2.update(2, TimeUnit.SECONDS);

        Assert.assertEquals(1, metricsByNames.get("testTimer1").getCount());
        Assert.assertEquals(1, metricsByNames.get("testTimer2").getCount());
        Assert.assertEquals(0, metricsByNames.get("testTimer3").getCount());

        CompositeForwardingTimer compositeForwardingTimer = new CompositeForwardingTimer(testTimer3, new MetricProvider<Timer>() {
            @Override
            public Timer get() {
                return metricsByNames.get("testTimer1");
            }
        });

        compositeForwardingTimer.update(4, TimeUnit.SECONDS);
        compositeForwardingTimer.update(1, TimeUnit.SECONDS);

        Assert.assertEquals(3, metricsByNames.get("testTimer1").getCount());
        Assert.assertEquals(1, metricsByNames.get("testTimer2").getCount());
        Assert.assertEquals(2, metricsByNames.get("testTimer3").getCount());
        Assert.assertEquals(2, compositeForwardingTimer.getCount());

        testTimer1.update(1, TimeUnit.SECONDS);
        testTimer1.update(2, TimeUnit.SECONDS);
        testTimer3.update((long) 29000, TimeUnit.MILLISECONDS);

        Assert.assertEquals(5, metricsByNames.get("testTimer1").getCount());
        Assert.assertEquals(1, metricsByNames.get("testTimer2").getCount());
        Assert.assertEquals(3, metricsByNames.get("testTimer3").getCount());
        Assert.assertEquals(3, compositeForwardingTimer.getCount());
    }

    @Test
    public void CompositeForwardingMeter() throws Exception {
        final Map<String, Meter> metricsByNames = new HashMap<>();

        Meter testMeter1 = new Meter();
        Meter testMeter2 = new Meter();
        Meter testMeter3 = new Meter();

        metricsByNames.put("testMeter1", testMeter1);
        metricsByNames.put("testMeter2", testMeter2);
        metricsByNames.put("testMeter3", testMeter3);

        testMeter1.mark(1);
        testMeter2.mark(2);

        Assert.assertEquals(1, metricsByNames.get("testMeter1").getCount());
        Assert.assertEquals(2, metricsByNames.get("testMeter2").getCount());
        Assert.assertEquals(0, metricsByNames.get("testMeter3").getCount());

        CompositeForwardingMeter compositeForwardingMeter = new CompositeForwardingMeter(testMeter3, new MetricProvider<Meter>() {
            @Override
            public Meter get() {
                return metricsByNames.get("testMeter1");
            }
        });

        compositeForwardingMeter.mark(2);

        Assert.assertEquals(3, metricsByNames.get("testMeter1").getCount());
        Assert.assertEquals(2, metricsByNames.get("testMeter2").getCount());
        Assert.assertEquals(2, metricsByNames.get("testMeter3").getCount());
        Assert.assertEquals(2, compositeForwardingMeter.getCount());

        testMeter1.mark(2);
        testMeter3.mark();

        Assert.assertEquals(5, metricsByNames.get("testMeter1").getCount());
        Assert.assertEquals(2, metricsByNames.get("testMeter2").getCount());
        Assert.assertEquals(3, metricsByNames.get("testMeter3").getCount());
        Assert.assertEquals(3, compositeForwardingMeter.getCount());
    }

    @Test
    public void ForwardingReadOnlyMeter() throws Exception {
        final Map<String, Meter> metricsByNames = new HashMap<>();

        Meter testMeter1 = new Meter();
        Meter testMeter2 = new Meter();
        Meter testMeter3 = new Meter();

        metricsByNames.put("testMeter1", testMeter1);
        metricsByNames.put("testMeter2", testMeter2);
        metricsByNames.put("testMeter3", testMeter3);

        testMeter1.mark(1);
        testMeter2.mark(2);

        Assert.assertEquals(1, metricsByNames.get("testMeter1").getCount());
        Assert.assertEquals(2, metricsByNames.get("testMeter2").getCount());
        Assert.assertEquals(0, metricsByNames.get("testMeter3").getCount());

        ForwardingReadOnlyMeter forwardingReadOnlyMeter1 = new ForwardingReadOnlyMeter(new MetricProvider<Meter>() {
            @Override
            public Meter get() {
                return metricsByNames.get("testMeter1");
            }
        });

        ForwardingReadOnlyMeter forwardingReadOnlyMeter2 = new ForwardingReadOnlyMeter(testMeter3);

        try {
            forwardingReadOnlyMeter1.mark(2);
            forwardingReadOnlyMeter2.mark();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof UnsupportedOperationException);
        }

        Assert.assertEquals(1, metricsByNames.get("testMeter1").getCount());
        Assert.assertEquals(2, metricsByNames.get("testMeter2").getCount());
        Assert.assertEquals(0, metricsByNames.get("testMeter3").getCount());
        Assert.assertEquals(1, forwardingReadOnlyMeter1.getCount());
        Assert.assertEquals(0, forwardingReadOnlyMeter2.getCount());

        testMeter1.mark(2);
        testMeter3.mark();

        Assert.assertEquals(3, metricsByNames.get("testMeter1").getCount());
        Assert.assertEquals(2, metricsByNames.get("testMeter2").getCount());
        Assert.assertEquals(1, metricsByNames.get("testMeter3").getCount());
        Assert.assertEquals(3, forwardingReadOnlyMeter1.getCount());
        Assert.assertEquals(1, forwardingReadOnlyMeter2.getCount());
    }

    @Test
    public void ForwardingReadOnlyTimer() throws Exception {
        final Map<String, Timer> metricsByNames = new HashMap<>();

        Timer testTimer1 = new Timer();
        Timer testTimer2 = new Timer();
        Timer testTimer3 = new Timer();

        metricsByNames.put("testTimer1", testTimer1);
        metricsByNames.put("testTimer2", testTimer2);
        metricsByNames.put("testTimer3", testTimer3);

        testTimer1.update(1, TimeUnit.SECONDS);
        testTimer2.update(1, TimeUnit.SECONDS);

        Assert.assertEquals(1, metricsByNames.get("testTimer1").getCount());
        Assert.assertEquals(1, metricsByNames.get("testTimer2").getCount());
        Assert.assertEquals(0, metricsByNames.get("testTimer3").getCount());

        ForwardingReadOnlyTimer forwardingReadOnlyTimer1 = new ForwardingReadOnlyTimer(new MetricProvider<Timer>() {
            @Override
            public Timer get() {
                return metricsByNames.get("testTimer1");
            }
        });

        ForwardingReadOnlyTimer forwardingReadOnlyTimer2 = new ForwardingReadOnlyTimer(testTimer3);

        try {
            forwardingReadOnlyTimer1.update(1, TimeUnit.SECONDS);
            forwardingReadOnlyTimer2.update(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            Assert.assertTrue(e instanceof UnsupportedOperationException);
        }

        Assert.assertEquals(1, metricsByNames.get("testTimer1").getCount());
        Assert.assertEquals(1, metricsByNames.get("testTimer2").getCount());
        Assert.assertEquals(0, metricsByNames.get("testTimer3").getCount());
        Assert.assertEquals(1, forwardingReadOnlyTimer1.getCount());
        Assert.assertEquals(0, forwardingReadOnlyTimer2.getCount());

        testTimer1.update(1, TimeUnit.SECONDS);
        testTimer3.update(1, TimeUnit.SECONDS);

        Assert.assertEquals(2, metricsByNames.get("testTimer1").getCount());
        Assert.assertEquals(1, metricsByNames.get("testTimer2").getCount());
        Assert.assertEquals(1, metricsByNames.get("testTimer3").getCount());
        Assert.assertEquals(2, forwardingReadOnlyTimer1.getCount());
        Assert.assertEquals(1, forwardingReadOnlyTimer2.getCount());
    }

    @Test
    public void ForwardingReadOnlyHistogram() throws Exception {
        final Map<String, Histogram> metricsByNames = new HashMap<>();

        Histogram testHistogram1 = new Histogram(new ExponentiallyDecayingReservoir());
        Histogram testHistogram2 = new Histogram(new ExponentiallyDecayingReservoir());
        Histogram testHistogram3 = new Histogram(new ExponentiallyDecayingReservoir());

        metricsByNames.put("testHistogram1", testHistogram1);
        metricsByNames.put("testHistogram2", testHistogram2);
        metricsByNames.put("testHistogram3", testHistogram3);

        testHistogram1.update(1);
        testHistogram2.update((long) 2);

        Assert.assertEquals(1, metricsByNames.get("testHistogram1").getCount());
        Assert.assertEquals(1, metricsByNames.get("testHistogram2").getCount());
        Assert.assertEquals(0, metricsByNames.get("testHistogram3").getCount());

        ForwardingReadOnlyHistogram forwardingReadOnlyHistogram1 = new ForwardingReadOnlyHistogram(new MetricProvider<Histogram>() {
            @Override
            public Histogram get() {
                return metricsByNames.get("testHistogram1");
            }
        });

        ForwardingReadOnlyHistogram forwardingReadOnlyHistogram2 = new ForwardingReadOnlyHistogram(testHistogram3);

        try {
            forwardingReadOnlyHistogram1.update(2);
            forwardingReadOnlyHistogram2.update(22112);
        } catch (Exception e) {
            Assert.assertTrue(e instanceof UnsupportedOperationException);
        }

        Assert.assertEquals(1, metricsByNames.get("testHistogram1").getCount());
        Assert.assertEquals(1, metricsByNames.get("testHistogram2").getCount());
        Assert.assertEquals(0, metricsByNames.get("testHistogram3").getCount());
        Assert.assertEquals(1, forwardingReadOnlyHistogram1.getCount());
        Assert.assertEquals(0, forwardingReadOnlyHistogram2.getCount());

        testHistogram1.update(2);
        testHistogram3.update(2);

        Assert.assertEquals(2, metricsByNames.get("testHistogram1").getCount());
        Assert.assertEquals(1, metricsByNames.get("testHistogram2").getCount());
        Assert.assertEquals(1, metricsByNames.get("testHistogram3").getCount());
        Assert.assertEquals(2, forwardingReadOnlyHistogram1.getCount());
        Assert.assertEquals(1, forwardingReadOnlyHistogram2.getCount());
    }

    @Test
    public void ForwardingReadOnlyCounter() throws Exception {
        final Map<String, Counter> metricsByNames = new HashMap<>();

        Counter testCounter1 = new Counter();
        Counter testCounter2 = new Counter();
        Counter testCounter3 = new Counter();

        metricsByNames.put("testCounter1", testCounter1);
        metricsByNames.put("testCounter2", testCounter2);
        metricsByNames.put("testCounter3", testCounter3);

        testCounter1.inc(1);
        testCounter2.inc(2);

        Assert.assertEquals(1, metricsByNames.get("testCounter1").getCount());
        Assert.assertEquals(2, metricsByNames.get("testCounter2").getCount());
        Assert.assertEquals(0, metricsByNames.get("testCounter3").getCount());

        ForwardingReadOnlyCounter forwardingReadOnlyCounter1 = new ForwardingReadOnlyCounter(new MetricProvider<Counter>() {
            @Override
            public Counter get() {
                return metricsByNames.get("testCounter1");
            }
        });

        ForwardingReadOnlyCounter forwardingReadOnlyCounter2 = new ForwardingReadOnlyCounter(testCounter3);

        try {
            forwardingReadOnlyCounter1.inc(2);
            forwardingReadOnlyCounter2.inc();
            forwardingReadOnlyCounter1.dec();
            forwardingReadOnlyCounter2.dec(22);
        } catch (Exception e) {
            Assert.assertTrue(e instanceof UnsupportedOperationException);
        }

        Assert.assertEquals(1, metricsByNames.get("testCounter1").getCount());
        Assert.assertEquals(2, metricsByNames.get("testCounter2").getCount());
        Assert.assertEquals(0, metricsByNames.get("testCounter3").getCount());
        Assert.assertEquals(1, forwardingReadOnlyCounter1.getCount());
        Assert.assertEquals(0, forwardingReadOnlyCounter2.getCount());

        testCounter1.dec(2);
        testCounter3.dec();

        Assert.assertEquals(-1, metricsByNames.get("testCounter1").getCount());
        Assert.assertEquals(2, metricsByNames.get("testCounter2").getCount());
        Assert.assertEquals(-1, metricsByNames.get("testCounter3").getCount());
        Assert.assertEquals(-1, forwardingReadOnlyCounter1.getCount());
        Assert.assertEquals(-1, forwardingReadOnlyCounter2.getCount());
    }
}
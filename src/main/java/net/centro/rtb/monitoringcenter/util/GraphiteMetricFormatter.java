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

package net.centro.rtb.monitoringcenter.util;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metered;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.google.common.base.Preconditions;

import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

public class GraphiteMetricFormatter {
    private double rateFactor;
    private double durationFactor;

    public GraphiteMetricFormatter(TimeUnit rateUnit, TimeUnit durationUnit) {
        Preconditions.checkNotNull(rateUnit);
        Preconditions.checkNotNull(durationUnit);

        this.rateFactor = rateUnit.toSeconds(1);
        this.durationFactor = 1.0 / durationUnit.toNanos(1);
    }

    public String format(SortedMap<String, Metric> metricsByNames) {
        Preconditions.checkNotNull(metricsByNames);

        final long timestamp = System.nanoTime() / 1000;

        StringBuilder outputBuilder = new StringBuilder();
        for (Map.Entry<String, Metric> entry : metricsByNames.entrySet()) {
            String metricOutput = null;
            if (Counter.class.isInstance(entry.getValue())) {
                metricOutput = formatCounter(entry.getKey(), Counter.class.cast(entry.getValue()), timestamp);
            } else if (Gauge.class.isInstance(entry.getValue())) {
                metricOutput = formatGauge(entry.getKey(), Gauge.class.cast(entry.getValue()), timestamp);
            } else if (Timer.class.isInstance(entry.getValue())) {
                metricOutput = formatTimer(entry.getKey(), Timer.class.cast(entry.getValue()), timestamp);
            } else if (Meter.class.isInstance(entry.getValue())) {
                metricOutput = formatMetered(entry.getKey(), Meter.class.cast(entry.getValue()), timestamp);
            } else if (Histogram.class.isInstance(entry.getValue())) {
                metricOutput = formatHistogram(entry.getKey(), Histogram.class.cast(entry.getValue()), timestamp);
            }

            if (metricOutput != null) {
                outputBuilder.append(metricOutput);
            }
        }
        return outputBuilder.toString();
    }

    private String formatCounter(String name, Counter counter, long timestamp) {
        return formatLine(MetricNamingUtil.join(name, "count"), counter.getCount(), timestamp);
    }

    private String formatGauge(String name, Gauge gauge, long timestamp) {
        final String value = format(gauge.getValue());
        if (value != null) {
            return formatLine(name, value, timestamp);
        }
        return null;
    }

    private String formatTimer(String name, Timer timer, long timestamp) {
        final Snapshot snapshot = timer.getSnapshot();

        StringBuilder outputBuilder = new StringBuilder();

        outputBuilder.append(formatMetered(name, timer, timestamp));
        outputBuilder.append(formatSamplingSnapshot(name, snapshot, timestamp, true));

        return outputBuilder.toString();
    }

    private String formatMetered(String name, Metered meter, long timestamp) {
        StringBuilder outputBuilder = new StringBuilder();

        outputBuilder.append(formatLine(MetricNamingUtil.join(name, "count"), meter.getCount(), timestamp));
        outputBuilder.append(formatLine(MetricNamingUtil.join(name, "m1_rate"), convertRate(meter.getOneMinuteRate()), timestamp));
        outputBuilder.append(formatLine(MetricNamingUtil.join(name, "m5_rate"), convertRate(meter.getFiveMinuteRate()), timestamp));
        outputBuilder.append(formatLine(MetricNamingUtil.join(name, "m15_rate"), convertRate(meter.getFifteenMinuteRate()), timestamp));
        outputBuilder.append(formatLine(MetricNamingUtil.join(name, "mean_rate"), convertRate(meter.getMeanRate()), timestamp));

        return outputBuilder.toString();
    }

    private String formatHistogram(String name, Histogram histogram, long timestamp) {
        final Snapshot snapshot = histogram.getSnapshot();

        StringBuilder outputBuilder = new StringBuilder();

        outputBuilder.append(formatLine(MetricNamingUtil.join(name, "count"), histogram.getCount(), timestamp));
        outputBuilder.append(formatSamplingSnapshot(name, snapshot, timestamp, false));

        return outputBuilder.toString();
    }

    private String formatSamplingSnapshot(String name, Snapshot snapshot, long timestamp, boolean convertValuesToDurations) {
        StringBuilder outputBuilder = new StringBuilder();

        outputBuilder.append(formatLine(MetricNamingUtil.join(name, "max"), convertValuesToDurations ? convertDuration(snapshot.getMax()) : snapshot.getMax(), timestamp));
        outputBuilder.append(formatLine(MetricNamingUtil.join(name, "mean"), convertValuesToDurations ? convertDuration(snapshot.getMean()) : snapshot.getMean(), timestamp));
        outputBuilder.append(formatLine(MetricNamingUtil.join(name, "min"), convertValuesToDurations ? convertDuration(snapshot.getMin()) : snapshot.getMin(), timestamp));
        outputBuilder.append(formatLine(MetricNamingUtil.join(name, "stddev"), convertValuesToDurations ? convertDuration(snapshot.getStdDev()) : snapshot.getStdDev(), timestamp));
        outputBuilder.append(formatLine(MetricNamingUtil.join(name, "p50"), convertValuesToDurations ? convertDuration(snapshot.getMedian()) : snapshot.getMedian(), timestamp));
        outputBuilder.append(formatLine(MetricNamingUtil.join(name, "p75"), convertValuesToDurations ? convertDuration(snapshot.get75thPercentile()) : snapshot.get75thPercentile(), timestamp));
        outputBuilder.append(formatLine(MetricNamingUtil.join(name, "p95"), convertValuesToDurations ? convertDuration(snapshot.get95thPercentile()) : snapshot.get95thPercentile(), timestamp));
        outputBuilder.append(formatLine(MetricNamingUtil.join(name, "p98"), convertValuesToDurations ? convertDuration(snapshot.get98thPercentile()) : snapshot.get98thPercentile(), timestamp));
        outputBuilder.append(formatLine(MetricNamingUtil.join(name, "p99"), convertValuesToDurations ? convertDuration(snapshot.get99thPercentile()) : snapshot.get99thPercentile(), timestamp));
        outputBuilder.append(formatLine(MetricNamingUtil.join(name, "p999"), convertValuesToDurations ? convertDuration(snapshot.get999thPercentile()) : snapshot.get999thPercentile(), timestamp));

        return outputBuilder.toString();
    }

    private String formatLine(String name, Object value, long timestamp) {
        return new StringBuilder()
                .append(name)
                .append(" ")
                .append(value instanceof String ? value : format(value))
                .append(" ")
                .append(Long.valueOf(timestamp))
                .append("\n")
                .toString();
    }

    private String format(Object o) {
        if (o instanceof Float) {
            return format(((Float) o).doubleValue());
        } else if (o instanceof Double) {
            return format(((Double) o).doubleValue());
        } else if (o instanceof Byte) {
            return format(((Byte) o).longValue());
        } else if (o instanceof Short) {
            return format(((Short) o).longValue());
        } else if (o instanceof Integer) {
            return format(((Integer) o).longValue());
        } else if (o instanceof Long) {
            return format(((Long) o).longValue());
        }
        return null;
    }

    private String format(long n) {
        return Long.toString(n);
    }

    private String format(double v) {
        return String.format(Locale.US, "%2.2f", v);
    }

    private double convertDuration(double duration) {
        return duration * durationFactor;
    }

    private double convertRate(double rate) {
        return rate * rateFactor;
    }
}

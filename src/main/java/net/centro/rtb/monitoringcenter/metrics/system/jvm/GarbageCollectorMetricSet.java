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

package net.centro.rtb.monitoringcenter.metrics.system.jvm;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.Timer;
import com.sun.management.GarbageCollectionNotificationInfo;
import net.centro.rtb.monitoringcenter.metrics.forwarding.ForwardingReadOnlyTimer;
import net.centro.rtb.monitoringcenter.util.MetricNamingUtil;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;

import javax.management.ListenerNotFoundException;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.io.File;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

class GarbageCollectorMetricSet implements MetricSet {
    private static final String LOG_GC_JVM_PARAM = "-Xloggc:";
    private static final long GC_LOG_FILE_TAIL_DELAY_IN_MILLIS = 5000L;
    private static final String FULL_GC_LOG_STRING = "Full GC";

    private static final String GC_NOTIFICATION_MINOR_GC_ACTION_STRING = "end of minor GC";
    private static final String GC_NOTIFICATION_MAJOR_GC_ACTION_STRING = "end of major GC";

    private List<GarbageCollectorMXBean> garbageCollectorMXBeans;

    private NotificationListener gcEventListener;
    private Tailer gcLogTailer;

    private AtomicLong fullCollectionsCounter;

    private Gauge<Long> fullCollectionsGauge;
    private Timer minorGcTimer;
    private Timer majorGcTimer;

    private List<GarbageCollectorStatus> garbageCollectorStatuses;

    private Map<String, Metric> metricsByNames;

    GarbageCollectorMetricSet() {
        this.garbageCollectorMXBeans = ManagementFactory.getGarbageCollectorMXBeans();

        this.minorGcTimer = new Timer();
        this.majorGcTimer = new Timer();

        // Determine the location of the gc log file (note that there's not support for rolling gc logs)
        String gcLogFilePath = null;
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        List<String> inputArguments = runtimeMXBean.getInputArguments();
        for (String argument : inputArguments) {
            if (argument.startsWith(LOG_GC_JVM_PARAM)) {
                gcLogFilePath = argument.substring(LOG_GC_JVM_PARAM.length());
                break;
            }
        }

        if (gcLogFilePath != null && !gcLogFilePath.trim().isEmpty()) {
            final File gcLogFile = new File(gcLogFilePath);
            if (gcLogFile.exists()) {
                this.fullCollectionsCounter = new AtomicLong();

                this.gcLogTailer = Tailer.create(gcLogFile, new TailerListenerAdapter() {
                    @Override
                    public void handle(String line) {
                        if (line != null && line.contains(FULL_GC_LOG_STRING)) {
                            fullCollectionsCounter.incrementAndGet();
                        }
                    }
                }, GC_LOG_FILE_TAIL_DELAY_IN_MILLIS);
            }
        }

        // Attach a listener to the GarbageCollectorMXBeans
        this.gcEventListener = new NotificationListener() {
            @Override
            public void handleNotification(Notification notification, Object handback) {
                String notificationType = notification.getType();
                if (notificationType.equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
                    CompositeData compositeData = CompositeData.class.cast(notification.getUserData());
                    GarbageCollectionNotificationInfo gcNotificationInfo = GarbageCollectionNotificationInfo.from(compositeData);

                    if (GC_NOTIFICATION_MINOR_GC_ACTION_STRING.equals(gcNotificationInfo.getGcAction())) {
                        minorGcTimer.update(gcNotificationInfo.getGcInfo().getDuration(), TimeUnit.MILLISECONDS);
                    } else if (GC_NOTIFICATION_MAJOR_GC_ACTION_STRING.equals(gcNotificationInfo.getGcAction())) {
                        majorGcTimer.update(gcNotificationInfo.getGcInfo().getDuration(), TimeUnit.MILLISECONDS);
                    }
                }
            }
        };

        for (final GarbageCollectorMXBean garbageCollectorMXBean : garbageCollectorMXBeans) {
            if (NotificationEmitter.class.isInstance(garbageCollectorMXBean)) {
                NotificationEmitter emitter = NotificationEmitter.class.cast(garbageCollectorMXBean);
                emitter.addNotificationListener(gcEventListener, null, null);
            }
        }

        // Set up metrics
        Map<String, Metric> metricsByNames = new HashMap<>();

        if (fullCollectionsCounter != null) {
            this.fullCollectionsGauge = new Gauge<Long>() {
                @Override
                public Long getValue() {
                    return fullCollectionsCounter.get();
                }
            };
            metricsByNames.put("fullCollections", fullCollectionsGauge);
        }

        metricsByNames.put("majorGcTimer", majorGcTimer);
        metricsByNames.put("minorGcTimer", minorGcTimer);

        List<GarbageCollectorStatus> garbageCollectorStatuses = new ArrayList<>();
        for (final GarbageCollectorMXBean garbageCollectorMXBean : garbageCollectorMXBeans) {
            final String garbageCollectorName = garbageCollectorMXBean.getName();
            final String garbageCollectorNamespace = MetricNamingUtil.join("collectors", MetricNamingUtil.sanitize(garbageCollectorName));

            final Gauge<Long> collectionsGauge;
            if (garbageCollectorMXBean.getCollectionCount() >= 0) {
                collectionsGauge = new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return garbageCollectorMXBean.getCollectionCount();
                    }
                };
                metricsByNames.put(MetricNamingUtil.join(garbageCollectorNamespace, "collections"), collectionsGauge);
            } else {
                collectionsGauge = null;
            }

            final Gauge<Long> totalCollectionDurationInMillisGauge;
            if (garbageCollectorMXBean.getCollectionTime() >= 0) {
                totalCollectionDurationInMillisGauge = new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                        return garbageCollectorMXBean.getCollectionTime();
                    }
                };
                metricsByNames.put(MetricNamingUtil.join(garbageCollectorNamespace, "totalCollectionDurationInMillis"), totalCollectionDurationInMillisGauge);
            } else {
                totalCollectionDurationInMillisGauge = null;
            }

            garbageCollectorStatuses.add(new GarbageCollectorStatus() {
                @Override
                public String getName() {
                    return garbageCollectorName;
                }

                @Override
                public Gauge<Long> getCollectionsGauge() {
                    return collectionsGauge;
                }

                @Override
                public Gauge<Long> getTotalCollectionDurationInMillisGauge() {
                    return totalCollectionDurationInMillisGauge;
                }
            });
        }
        this.garbageCollectorStatuses = garbageCollectorStatuses;

        this.metricsByNames = metricsByNames;
    }

    @Override
    public Map<String, Metric> getMetrics() {
        return Collections.unmodifiableMap(metricsByNames);
    }

    Gauge<Long> getFullCollectionsGauge() {
        return fullCollectionsGauge;
    }

    Timer getMinorGcTimer() {
        return new ForwardingReadOnlyTimer(minorGcTimer);
    }

    Timer getMajorGcTimer() {
        return new ForwardingReadOnlyTimer(majorGcTimer);
    }

    List<GarbageCollectorStatus> getGarbageCollectorStatuses() {
        return Collections.unmodifiableList(garbageCollectorStatuses);
    }

    public void shutdown() {
        if (gcLogTailer != null) {
            gcLogTailer.stop();
        }

        for (GarbageCollectorMXBean garbageCollectorMXBean : garbageCollectorMXBeans) {
            if (NotificationEmitter.class.isInstance(garbageCollectorMXBean)) {
                NotificationEmitter emitter = NotificationEmitter.class.cast(garbageCollectorMXBean);
                try {
                    emitter.removeNotificationListener(gcEventListener);
                } catch (ListenerNotFoundException ignore) {
                }
            }
        }
    }
}

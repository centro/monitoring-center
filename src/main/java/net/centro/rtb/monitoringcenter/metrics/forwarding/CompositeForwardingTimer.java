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

package net.centro.rtb.monitoringcenter.metrics.forwarding;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.google.common.base.Preconditions;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class CompositeForwardingTimer extends Timer {
    private Timer mainDelegate;
    private MetricProvider<Timer> supplementaryMetricProvider;

    private Clock clock;

    public CompositeForwardingTimer(Timer mainDelegate, MetricProvider<Timer> supplementaryMetricProvider) {
        Preconditions.checkNotNull(mainDelegate);
        Preconditions.checkNotNull(supplementaryMetricProvider);

        this.mainDelegate = mainDelegate;
        this.supplementaryMetricProvider = supplementaryMetricProvider;
        this.clock = Clock.defaultClock();
    }

    @Override
    public void update(long duration, TimeUnit unit) {
        mainDelegate.update(duration, unit);
        supplementaryMetricProvider.get().update(duration, unit);
    }

    @Override
    public <T> T time(Callable<T> event) throws Exception {
        final long startTime = clock.getTick();
        try {
            return event.call();
        } finally {
            update(clock.getTick() - startTime, TimeUnit.NANOSECONDS);
        }
    }

    @Override
    public Context time() {
        return super.time();
    }

    @Override
    public long getCount() {
        return mainDelegate.getCount();
    }

    @Override
    public double getFifteenMinuteRate() {
        return mainDelegate.getFifteenMinuteRate();
    }

    @Override
    public double getFiveMinuteRate() {
        return mainDelegate.getFiveMinuteRate();
    }

    @Override
    public double getMeanRate() {
        return mainDelegate.getMeanRate();
    }

    @Override
    public double getOneMinuteRate() {
        return mainDelegate.getOneMinuteRate();
    }

    @Override
    public Snapshot getSnapshot() {
        return mainDelegate.getSnapshot();
    }
}

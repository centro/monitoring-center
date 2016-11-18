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

import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.google.common.base.Preconditions;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class ForwardingReadOnlyTimer extends Timer {
    private MetricProvider<Timer> metricProvider;

    public ForwardingReadOnlyTimer(final Timer delegate) {
        Preconditions.checkNotNull(delegate);
        this.metricProvider = new MetricProvider<Timer>() {
            @Override
            public Timer get() {
                return delegate;
            }
        };
    }

    public ForwardingReadOnlyTimer(MetricProvider<Timer> metricProvider) {
        Preconditions.checkNotNull(metricProvider);
        this.metricProvider = metricProvider;
    }

    @Override
    public void update(long duration, TimeUnit unit) {
        throw new UnsupportedOperationException("Operation is not allowed for a read-only timer");
    }

    @Override
    public <T> T time(Callable<T> event) throws Exception {
        throw new UnsupportedOperationException("Operation is not allowed for a read-only timer");
    }

    @Override
    public Context time() {
        throw new UnsupportedOperationException("Operation is not allowed for a read-only timer");
    }

    @Override
    public long getCount() {
        return metricProvider.get().getCount();
    }

    @Override
    public double getFifteenMinuteRate() {
        return metricProvider.get().getFifteenMinuteRate();
    }

    @Override
    public double getFiveMinuteRate() {
        return metricProvider.get().getFiveMinuteRate();
    }

    @Override
    public double getMeanRate() {
        return metricProvider.get().getMeanRate();
    }

    @Override
    public double getOneMinuteRate() {
        return metricProvider.get().getOneMinuteRate();
    }

    @Override
    public Snapshot getSnapshot() {
        return metricProvider.get().getSnapshot();
    }
}

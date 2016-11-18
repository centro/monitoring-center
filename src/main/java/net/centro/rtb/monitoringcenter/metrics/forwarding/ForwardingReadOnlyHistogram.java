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

import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Snapshot;
import com.google.common.base.Preconditions;

public class ForwardingReadOnlyHistogram extends Histogram {
    private MetricProvider<Histogram> metricProvider;

    public ForwardingReadOnlyHistogram(final Histogram delegate) {
        super(new ExponentiallyDecayingReservoir());

        Preconditions.checkNotNull(delegate);
        this.metricProvider = new MetricProvider<Histogram>() {
            @Override
            public Histogram get() {
                return delegate;
            }
        };
    }

    public ForwardingReadOnlyHistogram(MetricProvider<Histogram> metricProvider) {
        super(new ExponentiallyDecayingReservoir());

        Preconditions.checkNotNull(metricProvider);
        this.metricProvider = metricProvider;
    }

    @Override
    public Snapshot getSnapshot() {
        return metricProvider.get().getSnapshot();
    }

    @Override
    public long getCount() {
        return metricProvider.get().getCount();
    }

    @Override
    public void update(long value) {
        throw new UnsupportedOperationException("Operation is not allowed for a read-only histogram");
    }

    @Override
    public void update(int value) {
        throw new UnsupportedOperationException("Operation is not allowed for a read-only histogram");
    }
}

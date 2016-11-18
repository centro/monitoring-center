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

public class CompositeForwardingHistogram extends Histogram {
    private Histogram mainDelegate;
    private MetricProvider<Histogram> supplementaryMetricProvider;

    public CompositeForwardingHistogram(Histogram mainDelegate, MetricProvider<Histogram> supplementaryMetricProvider) {
        super(new ExponentiallyDecayingReservoir());

        Preconditions.checkNotNull(mainDelegate);
        Preconditions.checkNotNull(supplementaryMetricProvider);

        this.mainDelegate = mainDelegate;
        this.supplementaryMetricProvider = supplementaryMetricProvider;
    }

    @Override
    public Snapshot getSnapshot() {
        return mainDelegate.getSnapshot();
    }

    @Override
    public long getCount() {
        return mainDelegate.getCount();
    }

    @Override
    public void update(long value) {
        mainDelegate.update(value);
        supplementaryMetricProvider.get().update(value);
    }

    @Override
    public void update(int value) {
        mainDelegate.update(value);
        supplementaryMetricProvider.get().update(value);
    }
}

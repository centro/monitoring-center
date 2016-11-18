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

import com.codahale.metrics.Meter;
import com.google.common.base.Preconditions;

public class CompositeForwardingMeter extends Meter {
    private Meter mainDelegate;
    private MetricProvider<Meter> supplementaryMetricProvider;

    public CompositeForwardingMeter(Meter mainDelegate, MetricProvider<Meter> supplementaryMetricProvider) {
        Preconditions.checkNotNull(mainDelegate);
        Preconditions.checkNotNull(supplementaryMetricProvider);

        this.mainDelegate = mainDelegate;
        this.supplementaryMetricProvider = supplementaryMetricProvider;
    }

    @Override
    public double getOneMinuteRate() {
        return mainDelegate.getOneMinuteRate();
    }

    @Override
    public double getMeanRate() {
        return mainDelegate.getMeanRate();
    }

    @Override
    public double getFiveMinuteRate() {
        return mainDelegate.getFiveMinuteRate();
    }

    @Override
    public double getFifteenMinuteRate() {
        return mainDelegate.getFifteenMinuteRate();
    }

    @Override
    public long getCount() {
        return mainDelegate.getCount();
    }

    @Override
    public void mark(long n) {
        mainDelegate.mark(n);
        supplementaryMetricProvider.get().mark(n);
    }

    @Override
    public void mark() {
        mainDelegate.mark();
        supplementaryMetricProvider.get().mark();
    }
}

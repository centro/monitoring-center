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

import com.codahale.metrics.Counter;
import com.google.common.base.Preconditions;

public class CompositeForwardingCounter extends Counter {
    private Counter mainDelegate;
    private MetricProvider<Counter> supplementaryMetricProvider;

    public CompositeForwardingCounter(Counter mainDelegate, MetricProvider<Counter> supplementaryMetricProvider) {
        Preconditions.checkNotNull(mainDelegate);
        Preconditions.checkNotNull(supplementaryMetricProvider);

        this.mainDelegate = mainDelegate;
        this.supplementaryMetricProvider = supplementaryMetricProvider;
    }

    @Override
    public void inc() {
        mainDelegate.inc();
        supplementaryMetricProvider.get().inc();
    }

    @Override
    public void inc(long n) {
        mainDelegate.inc(n);
        supplementaryMetricProvider.get().inc(n);
    }

    @Override
    public void dec() {
        mainDelegate.dec();
        supplementaryMetricProvider.get().dec();
    }

    @Override
    public void dec(long n) {
        mainDelegate.dec(n);
        supplementaryMetricProvider.get().dec(n);
    }

    @Override
    public long getCount() {
        return mainDelegate.getCount();
    }
}

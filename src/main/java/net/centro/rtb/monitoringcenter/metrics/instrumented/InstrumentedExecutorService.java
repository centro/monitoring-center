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

package net.centro.rtb.monitoringcenter.metrics.instrumented;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import net.centro.rtb.monitoringcenter.MetricCollector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// Based on io/dropwizard/metrics/InstrumentedExecutorService.java
public class InstrumentedExecutorService implements ExecutorService {
    private final ExecutorService delegate;

    private final Meter submittedMeter;
    private final Counter runningCounter;
    private final Meter completedMeter;
    private final Timer durationTimer;
    private final Meter rejectedMeter;

    public InstrumentedExecutorService(ExecutorService delegate, MetricCollector metricCollector, String name) {
        this.delegate = delegate;
        this.submittedMeter = metricCollector.getMeter(name, "submittedMeter");
        this.runningCounter = metricCollector.getCounter(name, "running");
        this.completedMeter = metricCollector.getMeter(name, "completedMeter");
        this.durationTimer = metricCollector.getTimer(name, "durationTimer");
        this.rejectedMeter = metricCollector.getMeter(name, "rejectedMeter");
    }

    @Override
    public void execute(Runnable runnable) {
        submittedMeter.mark();
        try {
            delegate.execute(new InstrumentedRunnable(runnable));
        } catch (RejectedExecutionException e) {
            rejectedMeter.mark();
            throw e;
        }
    }

    @Override
    public Future<?> submit(Runnable runnable) {
        submittedMeter.mark();
        try {
            return delegate.submit(new InstrumentedRunnable(runnable));
        } catch (RejectedExecutionException e) {
            rejectedMeter.mark();
            throw e;
        }
    }

    @Override
    public <T> Future<T> submit(Runnable runnable, T result) {
        submittedMeter.mark();
        try {
            return delegate.submit(new InstrumentedRunnable(runnable), result);
        } catch (RejectedExecutionException e) {
            rejectedMeter.mark();
            throw e;
        }
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        submittedMeter.mark();
        try {
            return delegate.submit(new InstrumentedCallable<T>(task));
        } catch (RejectedExecutionException e) {
            rejectedMeter.mark();
            throw e;
        }
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        submittedMeter.mark(tasks.size());
        Collection<? extends Callable<T>> instrumented = instrument(tasks);
        try {
            return delegate.invokeAll(instrumented);
        } catch (RejectedExecutionException e) {
            rejectedMeter.mark();
            throw e;
        }
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        submittedMeter.mark(tasks.size());
        Collection<? extends Callable<T>> instrumented = instrument(tasks);
        try {
            return delegate.invokeAll(instrumented, timeout, unit);
        } catch (RejectedExecutionException e) {
            rejectedMeter.mark();
            throw e;
        }
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws ExecutionException, InterruptedException {
        submittedMeter.mark(tasks.size());
        Collection<? extends Callable<T>> instrumented = instrument(tasks);
        try {
            return delegate.invokeAny(instrumented);
        } catch (RejectedExecutionException e) {
            rejectedMeter.mark();
            throw e;
        }
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        submittedMeter.mark(tasks.size());
        Collection<? extends Callable<T>> instrumented = instrument(tasks);
        try {
            return delegate.invokeAny(instrumented, timeout, unit);
        } catch (RejectedExecutionException e) {
            rejectedMeter.mark();
            throw e;
        }
    }

    private <T> Collection<? extends Callable<T>> instrument(Collection<? extends Callable<T>> tasks) {
        final List<InstrumentedCallable<T>> instrumented = new ArrayList<InstrumentedCallable<T>>(tasks.size());
        for (Callable<T> task : tasks) {
            instrumented.add(new InstrumentedCallable<T>(task));
        }
        return instrumented;
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long l, TimeUnit timeUnit) throws InterruptedException {
        return delegate.awaitTermination(l, timeUnit);
    }

    private class InstrumentedRunnable implements Runnable {
        private final Runnable task;

        InstrumentedRunnable(Runnable task) {
            this.task = task;
        }

        @Override
        public void run() {
            runningCounter.inc();
            final Timer.Context context = durationTimer.time();
            try {
                task.run();
            } finally {
                context.stop();
                runningCounter.dec();
                completedMeter.mark();
            }
        }
    }

    private class InstrumentedCallable<T> implements Callable<T> {
        private final Callable<T> callable;

        InstrumentedCallable(Callable<T> callable) {
            this.callable = callable;
        }

        @Override
        public T call() throws Exception {
            runningCounter.inc();
            final Timer.Context context = durationTimer.time();
            try {
                return callable.call();
            } finally {
                context.stop();
                runningCounter.dec();
                completedMeter.mark();
            }
        }
    }
}

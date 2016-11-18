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
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import net.centro.rtb.monitoringcenter.MetricCollector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// Based on io/dropwizard/metrics/InstrumentedScheduledExecutorService.java
@SuppressWarnings("NullableProblems")
public class InstrumentedScheduledExecutorService implements ScheduledExecutorService {
    private final ScheduledExecutorService delegate;

    private final Meter submittedMeter;
    private final Counter runningCounter;
    private final Meter completedMeter;
    private final Timer durationTimer;

    private final Meter scheduledOneOffTaskMeter;
    private final Meter scheduledRepetitiveTaskMeter;
    private final Counter scheduledOverrunCounter;
    private final Histogram durationAsPercentOfPeriodHistogram;

    public InstrumentedScheduledExecutorService(ScheduledExecutorService delegate, MetricCollector metricCollector, String name) {
        this.delegate = delegate;

        this.submittedMeter = metricCollector.getMeter(name, "submittedMeter");

        this.runningCounter = metricCollector.getCounter(name, "running");
        this.completedMeter = metricCollector.getMeter(name, "completedMeter");
        this.durationTimer = metricCollector.getTimer(name, "durationTimer");

        this.scheduledOneOffTaskMeter = metricCollector.getMeter(name, "scheduled.oneOffTaskMeter");
        this.scheduledRepetitiveTaskMeter = metricCollector.getMeter(name, "scheduled.repetitiveTaskMeter");
        this.scheduledOverrunCounter = metricCollector.getCounter(name, "scheduled.overrun");
        this.durationAsPercentOfPeriodHistogram = metricCollector.getHistogram(name, "scheduled.durationAsPercentOfPeriodHistogram");
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        scheduledOneOffTaskMeter.mark();
        return delegate.schedule(new InstrumentedRunnable(command), delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        scheduledOneOffTaskMeter.mark();
        return delegate.schedule(new InstrumentedCallable<>(callable), delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        scheduledRepetitiveTaskMeter.mark();
        return delegate.scheduleAtFixedRate(new InstrumentedPeriodicRunnable(command, period, unit), initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        scheduledRepetitiveTaskMeter.mark();
        return delegate.scheduleAtFixedRate(new InstrumentedRunnable(command), initialDelay, delay, unit);
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
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        submittedMeter.mark();
        return delegate.submit(new InstrumentedCallable<>(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        submittedMeter.mark();
        return delegate.submit(new InstrumentedRunnable(task), result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        submittedMeter.mark();
        return delegate.submit(new InstrumentedRunnable(task));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        submittedMeter.mark(tasks.size());
        Collection<? extends Callable<T>> instrumented = instrument(tasks);
        return delegate.invokeAll(instrumented);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        submittedMeter.mark(tasks.size());
        Collection<? extends Callable<T>> instrumented = instrument(tasks);
        return delegate.invokeAll(instrumented, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        submittedMeter.mark(tasks.size());
        Collection<? extends Callable<T>> instrumented = instrument(tasks);
        return delegate.invokeAny(instrumented);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        submittedMeter.mark(tasks.size());
        Collection<? extends Callable<T>> instrumented = instrument(tasks);
        return delegate.invokeAny(instrumented, timeout, unit);
    }

    private <T> Collection<? extends Callable<T>> instrument(Collection<? extends Callable<T>> tasks) {
        final List<InstrumentedCallable<T>> instrumented = new ArrayList<>(tasks.size());
        for (Callable<T> task : tasks) {
            instrumented.add(new InstrumentedCallable<>(task));
        }
        return instrumented;
    }

    @Override
    public void execute(Runnable command) {
        submittedMeter.mark();
        delegate.execute(new InstrumentedRunnable(command));
    }

    private class InstrumentedRunnable implements Runnable {
        private final Runnable command;

        InstrumentedRunnable(Runnable command) {
            this.command = command;
        }

        @Override
        public void run() {
            runningCounter.inc();
            final Timer.Context context = durationTimer.time();
            try {
                command.run();
            } finally {
                context.stop();
                runningCounter.dec();
                completedMeter.mark();
            }
        }
    }

    private class InstrumentedPeriodicRunnable implements Runnable {
        private final Runnable command;
        private final long periodInNanos;

        InstrumentedPeriodicRunnable(Runnable command, long period, TimeUnit unit) {
            this.command = command;
            this.periodInNanos = unit.toNanos(period);
        }

        @Override
        public void run() {
            runningCounter.inc();
            final Timer.Context context = durationTimer.time();
            try {
                command.run();
            } finally {
                final long elapsed = context.stop();
                runningCounter.dec();
                completedMeter.mark();
                if (elapsed > periodInNanos) {
                    scheduledOverrunCounter.inc();
                }
                durationAsPercentOfPeriodHistogram.update((100L * elapsed) / periodInNanos);
            }
        }
    }

    private class InstrumentedCallable<T> implements Callable<T> {
        private final Callable<T> task;

        InstrumentedCallable(Callable<T> task) {
            this.task = task;
        }

        @Override
        public T call() throws Exception {
            runningCounter.inc();
            final Timer.Context context = durationTimer.time();
            try {
                return task.call();
            } finally {
                context.stop();
                runningCounter.dec();
                completedMeter.mark();
            }
        }
    }
}

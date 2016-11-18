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

import com.codahale.metrics.Timer;
import net.centro.rtb.monitoringcenter.MetricCollector;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

public class InstrumentedDataSource implements DataSource {
    private DataSource delegate;

    private Timer connectionAcquisitionTimer;

    public InstrumentedDataSource(DataSource delegate, MetricCollector metricCollector, String name) {
        this.delegate = delegate;
        this.connectionAcquisitionTimer = metricCollector.getTimer(name, "connectionAcquisitionTimer");
    }

    @Override
    public Connection getConnection() throws SQLException {
        final Timer.Context context = connectionAcquisitionTimer.time();
        try {
            return delegate.getConnection();
        } finally {
            context.stop();
        }
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        final Timer.Context context = connectionAcquisitionTimer.time();
        try {
            return delegate.getConnection(username, password);
        } finally {
            context.stop();
        }
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return delegate.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        delegate.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        delegate.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return delegate.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return delegate.getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return delegate.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return delegate.isWrapperFor(iface);
    }
}

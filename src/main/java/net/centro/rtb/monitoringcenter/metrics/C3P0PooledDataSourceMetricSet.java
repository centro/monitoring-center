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

package net.centro.rtb.monitoringcenter.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.google.common.base.Preconditions;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.PooledDataSource;
import net.centro.rtb.monitoringcenter.util.MetricNamingUtil;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class C3P0PooledDataSourceMetricSet implements MetricSet {
    private PooledDataSource pooledDataSource;
    private ComboPooledDataSource comboPooledDataSource;

    public C3P0PooledDataSourceMetricSet(PooledDataSource pooledDataSource) {
        Preconditions.checkNotNull(pooledDataSource);
        this.pooledDataSource = pooledDataSource;

        if (ComboPooledDataSource.class.isInstance(pooledDataSource)) {
            this.comboPooledDataSource = ComboPooledDataSource.class.cast(pooledDataSource);
        }
    }

    @Override
    public Map<String, Metric> getMetrics() {
        Map<String, Metric> metricsByNames = new HashMap<>();

        // ----- Connections -----
        String connectionsNamespace = "connections";

        metricsByNames.put(MetricNamingUtil.join(connectionsNamespace, "currentCount"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                try {
                    return pooledDataSource.getNumConnectionsAllUsers();
                } catch (SQLException ignore) {
                    return -1;
                }
            }
        });

        metricsByNames.put(MetricNamingUtil.join(connectionsNamespace, "busy"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                try {
                    return pooledDataSource.getNumBusyConnectionsAllUsers();
                } catch (SQLException ignore) {
                    return -1;
                }
            }
        });

        metricsByNames.put(MetricNamingUtil.join(connectionsNamespace, "idle"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                try {
                    return pooledDataSource.getNumIdleConnectionsAllUsers();
                } catch (SQLException ignore) {
                    return -1;
                }
            }
        });

        metricsByNames.put(MetricNamingUtil.join(connectionsNamespace, "unclosedOrphaned"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                try {
                    return pooledDataSource.getNumUnclosedOrphanedConnectionsAllUsers();
                } catch (SQLException ignore) {
                    return -1;
                }
            }
        });

        metricsByNames.put(MetricNamingUtil.join(connectionsNamespace, "failedCheckouts"), new Gauge<Long>() {
            @Override
            public Long getValue() {
                try {
                    return pooledDataSource.getNumFailedCheckoutsDefaultUser();
                } catch (SQLException ignore) {
                    return -1L;
                }
            }
        });

        metricsByNames.put(MetricNamingUtil.join(connectionsNamespace, "failedCheckins"), new Gauge<Long>() {
            @Override
            public Long getValue() {
                try {
                    return pooledDataSource.getNumFailedCheckinsDefaultUser();
                } catch (SQLException ignore) {
                    return -1L;
                }
            }
        });

        metricsByNames.put(MetricNamingUtil.join(connectionsNamespace, "failedIdleTests"), new Gauge<Long>() {
            @Override
            public Long getValue() {
                try {
                    return pooledDataSource.getNumFailedIdleTestsDefaultUser();
                } catch (SQLException ignore) {
                    return -1L;
                }
            }
        });

        metricsByNames.put(MetricNamingUtil.join(connectionsNamespace, "threadsAwaitingCheckout"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                try {
                    return pooledDataSource.getNumThreadsAwaitingCheckoutDefaultUser();
                } catch (SQLException ignore) {
                    return -1;
                }
            }
        });

        if (comboPooledDataSource != null) {
            metricsByNames.put(MetricNamingUtil.join(connectionsNamespace, "max"), new Gauge<Integer>() {
                @Override
                public Integer getValue() {
                    return comboPooledDataSource.getMaxPoolSize();
                }
            });

            metricsByNames.put(MetricNamingUtil.join(connectionsNamespace, "min"), new Gauge<Integer>() {
                @Override
                public Integer getValue() {
                    return comboPooledDataSource.getMinPoolSize();
                }
            });
        }

        // ----- Threads -----
        String helperThreadPoolNamespace = "helperThreadPool";

        metricsByNames.put(MetricNamingUtil.join(helperThreadPoolNamespace, "size"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                try {
                    return pooledDataSource.getThreadPoolSize();
                } catch (SQLException ignore) {
                    return -1;
                }
            }
        });

        metricsByNames.put(MetricNamingUtil.join(helperThreadPoolNamespace, "busyThreads"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                try {
                    return pooledDataSource.getThreadPoolNumActiveThreads();
                } catch (SQLException ignore) {
                    return -1;
                }
            }
        });

        metricsByNames.put(MetricNamingUtil.join(helperThreadPoolNamespace, "idleThreads"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                try {
                    return pooledDataSource.getThreadPoolNumIdleThreads();
                } catch (SQLException ignore) {
                    return -1;
                }
            }
        });

        metricsByNames.put(MetricNamingUtil.join(helperThreadPoolNamespace, "pendingTasks"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                try {
                    return pooledDataSource.getThreadPoolNumTasksPending();
                } catch (SQLException ignore) {
                    return -1;
                }
            }
        });

        // ----- Statement Cache -----
        String statementCacheNamespace = "statementCache";

        metricsByNames.put(MetricNamingUtil.join(statementCacheNamespace, "size"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                try {
                    return pooledDataSource.getStatementCacheNumStatementsAllUsers();
                } catch (SQLException ignore) {
                    return -1;
                }
            }
        });

        if (comboPooledDataSource != null) {
            metricsByNames.put(MetricNamingUtil.join(statementCacheNamespace, "maxSize"), new Gauge<Integer>() {
                @Override
                public Integer getValue() {
                    return comboPooledDataSource.getMaxStatements();
                }
            });
        }

        return Collections.unmodifiableMap(metricsByNames);
    }
}

package net.centro.rtb.monitoringcenter.metrics.web;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InstrumentedWebAppFilter implements Filter {
    private static final String METRIC_PREFIX = "name-prefix";
    private final String otherMetricName;
    private final Map<Integer, String> meterNamesByStatusCode;
    private ConcurrentMap<Integer, Meter> metersByStatusCode;
    private MetricRegistry metricsRegistry;
    private String metricPrefix;
    private Meter otherMeter;
    private Meter timeoutsMeter;
    private Meter errorsMeter;
    private Counter activeRequests;
    private Timer requestTimer;

    private static Map<Integer, String> createMeterNamesByStatusCode() {
        HashMap<Integer, String> meterNamesByStatusCode = new HashMap<>(6);
        meterNamesByStatusCode.put(200, "responseCodes.ok");
        meterNamesByStatusCode.put(201, "responseCodes.created");
        meterNamesByStatusCode.put(204, "responseCodes.noContent");
        meterNamesByStatusCode.put(400, "responseCodes.badRequest");
        meterNamesByStatusCode.put(404, "responseCodes.notFound");
        meterNamesByStatusCode.put(429, "responseCodes.tooManyRequests");
        meterNamesByStatusCode.put(500, "responseCodes.serverError");
        return meterNamesByStatusCode;
    }

    protected InstrumentedWebAppFilter(String otherMetricName) {
        this.otherMetricName = otherMetricName;
        this.meterNamesByStatusCode = createMeterNamesByStatusCode();
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        metricPrefix = filterConfig.getInitParameter(METRIC_PREFIX);
        if (metricPrefix == null || metricPrefix.isEmpty()) {
            metricPrefix = this.getClass().getName();
        }

        resetMetrics(metricPrefix);
    }

    public void setMetricsRegistry(MetricRegistry metricsRegistry) {
        this.metricsRegistry = metricsRegistry;

        resetMetrics(metricPrefix);
    }

    private void resetMetrics(String metricName) {
        if (metricsRegistry != null) {
            this.metersByStatusCode = new ConcurrentHashMap<>(this.meterNamesByStatusCode.size());

            for (Map.Entry entry : this.meterNamesByStatusCode.entrySet()) {
                this.metersByStatusCode.put((Integer) entry.getKey(), metricsRegistry.meter(MetricRegistry.name(metricName, (String) entry.getValue())));
            }

            this.otherMeter = metricsRegistry.meter(MetricRegistry.name(metricName, this.otherMetricName));
            this.timeoutsMeter = metricsRegistry.meter(MetricRegistry.name(metricName, "timeouts"));
            this.errorsMeter = metricsRegistry.meter(MetricRegistry.name(metricName, "errors"));
            this.activeRequests = metricsRegistry.counter(MetricRegistry.name(metricName, "activeRequests"));
            this.requestTimer = metricsRegistry.timer(MetricRegistry.name(metricName, "requests"));
        }
    }

    public void removeMetrics() {
        if (metricsRegistry != null) {
            for (Map.Entry entry : this.meterNamesByStatusCode.entrySet()) {
                metricsRegistry.remove(MetricRegistry.name(metricPrefix, (String) entry.getValue()));
            }

            metricsRegistry.remove(MetricRegistry.name(metricPrefix, this.otherMetricName));
            metricsRegistry.remove(MetricRegistry.name(metricPrefix, "timeouts"));
            metricsRegistry.remove(MetricRegistry.name(metricPrefix, "errors"));
            metricsRegistry.remove(MetricRegistry.name(metricPrefix, "activeRequests"));
            metricsRegistry.remove(MetricRegistry.name(metricPrefix, "requests"));
        }
    }

    public void destroy() {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (metricsRegistry == null) return;

        InstrumentedWebAppFilter.StatusExposingServletResponse wrappedResponse = new InstrumentedWebAppFilter.StatusExposingServletResponse((HttpServletResponse) response);
        this.activeRequests.inc();
        Timer.Context context = this.requestTimer.time();
        boolean error = false;

        try {
            chain.doFilter(request, wrappedResponse);
        } catch (IOException | ServletException | RuntimeException ex) {
            error = true;
            throw ex;
        } finally {
            if (!error && request.isAsyncStarted()) {
                request.getAsyncContext().addListener(new InstrumentedWebAppFilter.AsyncResultListener(context));
            } else {
                context.stop();
                this.activeRequests.dec();
                if (error) {
                    this.errorsMeter.mark();
                } else {
                    this.markMeterForStatusCode(wrappedResponse.getStatus());
                }
            }

        }

    }

    private void markMeterForStatusCode(int status) {
        Meter metric = this.metersByStatusCode.get(status);
        if (metric != null) {
            metric.mark();
        } else {
            this.otherMeter.mark();
        }

    }

    private class AsyncResultListener implements AsyncListener {
        private Timer.Context context;
        private boolean done = false;

        AsyncResultListener(Timer.Context context) {
            this.context = context;
        }

        public void onComplete(AsyncEvent event) throws IOException {
            if (!this.done) {
                HttpServletResponse suppliedResponse = (HttpServletResponse) event.getSuppliedResponse();
                this.context.stop();
                InstrumentedWebAppFilter.this.activeRequests.dec();
                InstrumentedWebAppFilter.this.markMeterForStatusCode(suppliedResponse.getStatus());
            }

        }

        public void onTimeout(AsyncEvent event) throws IOException {
            this.context.stop();
            InstrumentedWebAppFilter.this.activeRequests.dec();
            InstrumentedWebAppFilter.this.timeoutsMeter.mark();
            this.done = true;
        }

        public void onError(AsyncEvent event) throws IOException {
            this.context.stop();
            InstrumentedWebAppFilter.this.activeRequests.dec();
            InstrumentedWebAppFilter.this.errorsMeter.mark();
            this.done = true;
        }

        public void onStartAsync(AsyncEvent event) throws IOException {
        }
    }

    private static class StatusExposingServletResponse extends HttpServletResponseWrapper {
        private int httpStatus = 200;

        StatusExposingServletResponse(HttpServletResponse response) {
            super(response);
        }

        public void sendError(int sc) throws IOException {
            this.httpStatus = sc;
            super.sendError(sc);
        }

        public void sendError(int sc, String msg) throws IOException {
            this.httpStatus = sc;
            super.sendError(sc, msg);
        }

        public void setStatus(int sc) {
            this.httpStatus = sc;
            super.setStatus(sc);
        }

        public void setStatus(int sc, String sm) {
            this.httpStatus = sc;
            super.setStatus(sc, sm);
        }

        public int getStatus() {
            return this.httpStatus;
        }
    }
}

package com.geekvivek.dropwizard.jmxmetricsutils;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.geekvivek.dropwizard.jmxmetricsutils.config.MetricConfig;
import com.geekvivek.jmx.utils.JmxMetric;
import com.geekvivek.jmx.utils.exceptions.MetricNotAvailableException;
import com.geekvivek.jmx.utils.interfaces.JmxMetricsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import java.util.*;

/**
 * This class is a implementation of JmxMetricsListener. It receives the metrics events from
 * {@link com.geekvivek.jmx.utils.service.JmxMetricProviderService JmxMetricProviderService} and it add new metrics to
 * dropwizard MetricRegistry and also delete removed metrics from JMX MbeansServer from MetricRegistry.
 *
 * It first filter the metric with the given Filter config in MetricConfig, then add the valid metrics to MetricRegistry.
 * Note : By default it do not add anything to MetricRegistry.
 */
public class JmxToDropwizardReporter implements JmxMetricsListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(JmxToDropwizardReporter.class);

    private final MetricRegistry registry;
    private final MetricConfig metricConfig;
    private final Map<JmxMetric, String> metricCache;

    public JmxToDropwizardReporter(MetricRegistry registry, MetricConfig metricConfig) {
        this.registry = registry;
        this.metricConfig = metricConfig;
        this.metricCache = new HashMap<>();
    }

    @Override
    public void metricChange(JmxMetric metric) {
        this.metricConfig.getFilters().forEach(filter -> {
            if (metric.toString().matches(filter.getMetricsRegex())) {
                try {
                    handleNewMetric(metric, filter.getMetricType());
                } catch (InstanceNotFoundException e) {
                    LOGGER.error("Metric does not exist in MBeanServer - {}", metric);
                }
            }
        });
    }

    private void handleNewMetric(JmxMetric metric, String metricType) throws InstanceNotFoundException {
        switch (metricType) {
            case "gauge":
                handleGaugeTypeMetric(metric, metricType);
                break;
            case "histogram":
                metric.addNotificationListener(
                        new ValueChangeListener(metric),
                        registry.histogram(getDropWizardMetricName(metric))
                );
                break;
            case "meter":
                metric.addNotificationListener(
                        new ValueChangeListener(metric),
                        registry.meter(getDropWizardMetricName(metric))
                );
                break;
            default:
                LOGGER.error("Unsupported metricType {} for metric {} ; " +
                        "Only supported meter types are : gauge,histogram,meter", metricType, metric);
        }
    }

    @Override
    public void metricRemoval(JmxMetric metric) {
        String name = metricCache.remove(metric);
        if (name != null) {
            LOGGER.debug("Removing {} from MetricRegistry", name);
            registry.remove(name);
        }
    }

    private String getDropWizardMetricName(JmxMetric metric) {
        List<String> nameParts = new ArrayList<>(3);
        nameParts.add(metric.getType());
        metric.getTags().forEach((key, value) -> nameParts.add(key + "." + value));
        if (metric.getName() != null) {
            nameParts.add(metric.getName());
        }
        if (!Objects.equals(metric.getMeterName(), "Value") && !Objects.equals(metric.getMeterName(), "Number")) {
            nameParts.add(metric.getMeterName());
        }
        StringBuilder builder = new StringBuilder();
        for (String namePart : nameParts) {
            builder.append(namePart);
            builder.append(".");
        }
        builder.setLength(builder.length() - 1);  // Remove the trailing dot.
        String processedName = builder.toString().replace(' ', '_');
        return MetricRegistry.name(metric.getDomain(), processedName);
    }

    @Override
    public void close() {
        for (String name : metricCache.values())
            registry.remove(name);
    }

    private void handleGaugeTypeMetric(JmxMetric metric, String meterType) {
        Gauge<Double> gauge = () -> {
            try {
                return new Double(metric.getValue() + "");
            } catch (MetricNotAvailableException e) {
                return 0.0;
            } catch (RuntimeException e) {
                metricRemoval(metric);
                return 0.0;
            }
        };
        String name = getDropWizardMetricName(metric);
        LOGGER.info("Registering {} to MetricRegistry", name);
        try {
            registry.register(name, gauge);
            metricCache.put(metric, name);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("metricChange called for `{}' which was already registered, ignoring.", name);
        }
    }

    public class ValueChangeListener implements NotificationListener {
        private JmxMetric metric;

        public ValueChangeListener(JmxMetric metric) {
            this.metric = metric;
        }

        @Override
        public void handleNotification(Notification notification, Object context) {
            try {
                if (context == null) {
                    return;
                } else if (context instanceof Histogram) {
                    ((Histogram) context).update(new Long(notification.getUserData() + ""));
                } else if (context instanceof Meter) {
                    ((Meter) context).mark(new Long(notification.getUserData() + ""));
                }
            } catch (RuntimeException e) {
                LOGGER.error("Invalid metric config for {}", notification, e);
                metricRemoval(metric);
                try {
                    metric.removeNotificationListener(this);
                } catch (OperationsException listenerNotFoundException) {
                    //Nothing to log
                }
            }
        }
    }
}

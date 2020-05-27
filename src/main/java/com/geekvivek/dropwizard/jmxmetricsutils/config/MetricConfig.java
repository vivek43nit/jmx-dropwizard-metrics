package com.geekvivek.dropwizard.jmxmetricsutils.config;

import com.geekvivek.dropwizard.jmxmetricsutils.JmxToDropwizardReporter;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Config class to be passed to {@link JmxToDropwizardReporter JmxToDropwizardReporter} class.
 */
@Data
@Builder
public class MetricConfig {

    @Data
    @Builder
    public static class Filter {
        /**
         * metric regex. all matching regex will be added in MetricRegistry with given meterType.
         */
        private String metricsRegex;

        /**
         * meterType for all metrics which is matching metricsRegex.
         * <p>
         * Supported metricTypes are : gauge, histogram, meter.
         */
        private String metricType;
    }

    /**
     * List of filters. Note : First matching filter will be used for a metric.
     */
    private List<Filter> filters;
}

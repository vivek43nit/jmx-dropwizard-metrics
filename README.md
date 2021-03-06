# jmx-dropwizard-metrics [![Clojars Project](https://img.shields.io/clojars/v/com.geek-vivek.dropwizard/jmx-dropwizard-metrics.svg)](https://clojars.org/com.geek-vivek.dropwizard/jmx-dropwizard-metrics)

### When to Use ?
Ans: If you are using dropwizard and publishing the metrics from MetricRegistry
instead of JMX.

Since most of the open source libraries, and many clients, by default publish the metrics to JMX,
so if you will not publish the metrics from JMX then you need to add some other dependencies for
each library.

Now by using this library you will not need to add metrics reporter for each library.

### Maven Dependency
Use the following repository
```xml
<repository>
    <id>clojars</id>
    <name>Clojars repository</name>
    <url>https://clojars.org/repo</url>
</repository>
```

Use the following maven dependency
```xml
<dependency>
  <groupId>com.geek-vivek.dropwizard</groupId>
  <artifactId>jmx-dropwizard-metrics</artifactId>
  <version>1.0.0</version>
</dependency>
```

### How to Use ?
````
    //Get metric registry instance in your application.
    MetricRegistry registry = ...
````

Now check the JMX metrics that you want to publish. One simple way is to use jconsole and connect to your application on local system. 
Get the domain name, type name(optional), name of metric

Lets for example, you want to get kafka consume metrics.

When you will check in jconsole, then you will get domain name=kafka.consumer, and so on.


```java
    // After listing down, Now it is time to create filter
    import com.geekvivek.dropwizard.jmxmetricsutils.config.MetricConfig;

    MetricConfig metricConfig = MetricConfig.builder().filters(
        Collections.singletonList(
           MetricConfig.Filter.builder()
                .metricsRegex("kafka.consumer.*")   //regex to filter the metrics
                .metricType("gauge")                 //type of meter to apply on each metrics; supported metricTypes are gauge, histogram, meter
                .build()
        )
    ).build();  
```

Now Create a instance of JmxToDropwizardReporter and pass that to JMXMetricProviderService
and start the JMXMetricProviderService

```java
    new JmxMetricProviderService(Collections.singletonList(
        new JmxToDropwizardReporter(registry, metricConfig)
    )).start(10, 60, TimeUnit.SECONDS);
```

And you are done. :) 

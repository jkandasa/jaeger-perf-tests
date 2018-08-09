package io.jaegertracing.test;

import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.reporters.RemoteReporter;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.jaegertracing.spi.Sender;
import io.jaegertracing.thrift.internal.senders.HttpSender;
import io.jaegertracing.thrift.internal.senders.UdpSender;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pavol Loffay
 */
public class Main {
  private static final Logger logger = LoggerFactory.getLogger(Main.class);
  private static final Map<String, String> envs = System.getenv();

  private static final String SENDER = envs.getOrDefault("SENDER", "http");
  private static final Integer NUMBER_OF_SPANS = new Integer(envs.getOrDefault("NUMBER_OF_SPANS", "10"));
  private static final Integer NUM_OF_TRACERS = new Integer(envs.getOrDefault("NUM_OF_TRACERS", "10"));
  private static final Integer DELAY = new Integer(envs.getOrDefault("DELAY", "0"));

  private static final String STORAGE = envs.getOrDefault("QUERY_FROM", "jaeger-query");

  private static final String CASSANDRA_CLUSTER_IP = envs.getOrDefault("CASSANDRA_CLUSTER_IP", "localhost");
  private static final String CASSANDRA_KEYSPACE_NAME = envs.getOrDefault("CASSANDRA_KEYSPACE_NAME", "jaeger_v1_test");
  private static final String ELASTIC_HOSTNAME = envs.getOrDefault("ELASTIC_HOSTNAME", "localhost");

  private static final String JAEGER_QUERY_URL = envs.getOrDefault("JAEGER_QUERY_URL", "http://localhost:16686");
  private static final String JAEGER_QUERY_ASYNC = envs.getOrDefault("JAEGER_QUERY_ASYNC", "false");

  private static final String JAEGER_COLLECTOR_HOST = envs.getOrDefault("JAEGER_COLLECTOR_HOST", "localhost");
  private static final String JAEGER_COLLECTOR_PORT = envs.getOrDefault("JAEGER_COLLECTOR_PORT", "14268");
  private static final String JAEGER_AGENT_HOST = envs.getOrDefault("JAEGER_AGENT_HOST", "localhost");
  private static final Integer JAEGER_AGENT_PORT = new Integer(envs.getOrDefault("JAEGER_AGENT_PORT", "6831"));
  private static final Integer JAEGER_FLUSH_INTERVAL = new Integer(envs.getOrDefault("JAEGER_FLUSH_INTERVAL", "100"));
  private static final Integer JAEGER_MAX_PACKET_SIZE = new Integer(envs.getOrDefault("JAEGER_MAX_PACKET_SIZE", "0"));

  private final int expectedSpansCount;

  private final String serviceName = "perf-test";

  Main() {
    expectedSpansCount = NUMBER_OF_SPANS * NUM_OF_TRACERS;
  }

  public static void main(String []args) throws Exception {
    new Main().createSpansTest();
  }

  public void createSpansTest() throws Exception {
    logger.info("Starting with " + NUM_OF_TRACERS + " threads for " + NUMBER_OF_SPANS + " iterations with a delay of " + DELAY);

    long startTime = System.currentTimeMillis();
    ExecutorService executor = Executors.newFixedThreadPool(NUM_OF_TRACERS);
    List<Future<?>> futures = new ArrayList<>(NUM_OF_TRACERS);
    Set<String> serviceNames = new LinkedHashSet<>();
    for (int i = 0; i < NUM_OF_TRACERS; i++) {
      String name = "thread-" + i;
      JaegerTracer tracer = createJaegerTracer(serviceName + "-" + name);
      serviceNames.add(tracer.getServiceName());
      Runnable worker = new CreateSpansRunnable(tracer, name, NUMBER_OF_SPANS, DELAY, true);
      futures.add(executor.submit(worker));
    }
    for (Future<?> future: futures) {
      future.get();
    }
    executor.shutdownNow();
    executor.awaitTermination(1, TimeUnit.SECONDS);

    long endTime = System.currentTimeMillis();
    long duration = endTime - startTime;
    logger.info("Finished all " + NUM_OF_TRACERS + " threads; Created " + expectedSpansCount + " spans" + " in " + duration/1000 + " seconds") ;
    SpanCounter spanCounter = getSpanCounter(serviceNames);
    startTime = System.currentTimeMillis();
    int spansCount = spanCounter.countUntilNoChange(expectedSpansCount);
    duration = System.currentTimeMillis() - startTime;
    logger.info("Expected number of spans {}, actual {} stored in {}s", expectedSpansCount, spansCount,
        TimeUnit.MILLISECONDS.toSeconds(duration));
    ElasticsearchStatsGetter esStatsGetter = new ElasticsearchStatsGetter(
        ELASTIC_HOSTNAME, 9200);
    System.out.println(esStatsGetter.getStats());
    spanCounter.close();
    esStatsGetter.close();
  }

  public JaegerTracer createJaegerTracer(String serviceName) {
    Sender sender;
    if (SENDER.equalsIgnoreCase("udp")) {
      sender = new UdpSender(JAEGER_AGENT_HOST, JAEGER_AGENT_PORT, JAEGER_MAX_PACKET_SIZE);
      logger.info("Using UDP sender, sending to: {}:{}", JAEGER_AGENT_HOST, JAEGER_AGENT_PORT);
    } else {
      // use the collector
      String httpEndpoint = "http://" + JAEGER_COLLECTOR_HOST + ":" + JAEGER_COLLECTOR_PORT + "/api/traces";
      logger.info("Using HTTP sender, sending to endpoint: {}", httpEndpoint);
      sender = new HttpSender.Builder(httpEndpoint).build();
    }

    logger.info("Flush interval {}, queue size {}", JAEGER_FLUSH_INTERVAL, expectedSpansCount);
    RemoteReporter reporter = new RemoteReporter.Builder()
        .withSender(sender)
        .withMaxQueueSize(expectedSpansCount)
        .withFlushInterval(JAEGER_FLUSH_INTERVAL)
        .build();

    return new JaegerTracer.Builder(serviceName)
        .withReporter(reporter)
        .withSampler(new ConstSampler(true))
        .build();
  }

  private static SpanCounter getSpanCounter(Set<String> serviceNames) {
    SpanCounter spanCounter;
    if ("elasticsearch".equals(STORAGE)) {
      spanCounter = new ElasticsearchSpanCounter(ELASTIC_HOSTNAME, 9200);
    } else if ("jaeger-query".equals(STORAGE)) {
      boolean async = "true".equals(JAEGER_QUERY_ASYNC);
      spanCounter = new JaegerQuerySpanCounter(JAEGER_QUERY_URL, NUM_OF_TRACERS * NUMBER_OF_SPANS, serviceNames, async);
    } else {
      spanCounter = new CassandraSpanCounter(CASSANDRA_CLUSTER_IP, CASSANDRA_KEYSPACE_NAME);
    }
    return spanCounter;
  }
}

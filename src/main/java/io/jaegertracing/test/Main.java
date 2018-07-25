package io.jaegertracing.test;

import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.reporters.RemoteReporter;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.jaegertracing.spi.Sender;
import io.jaegertracing.thrift.internal.senders.HttpSender;
import io.jaegertracing.thrift.internal.senders.UdpSender;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
  private static final Integer ITERATIONS = new Integer(envs.getOrDefault("ITERATIONS", "3000"));
  private static final Integer THREAD_COUNT = new Integer(envs.getOrDefault("THREAD_COUNT", "100"));
  private static final Integer DELAY = new Integer(envs.getOrDefault("DELAY", "0"));

  private static final String STORAGE = envs.getOrDefault("SPAN_STORAGE_TYPE", "elasticsearch");
  private static final String CASSANDRA_CLUSTER_IP = envs.getOrDefault("CASSANDRA_CLUSTER_IP", "localhost");
  private static final String CASSANDRA_KEYSPACE_NAME = envs.getOrDefault("CASSANDRA_KEYSPACE_NAME", "jaeger_v1_test");
  private static final String ELASTIC_HOSTNAME = envs.getOrDefault("ELASTIC_HOSTNAME", "localhost");

  private static final String JAEGER_QUERY_URL = envs.getOrDefault("JAEGER_QUERY_URL", "http://localhost:16686");

  private static final String JAEGER_COLLECTOR_HOST = envs.getOrDefault("JAEGER_COLLECTOR_HOST", "localhost");
  private static final String JAEGER_COLLECTOR_PORT = envs.getOrDefault("JAEGER_COLLECTOR_PORT", "14268");
  private static final String JAEGER_AGENT_HOST = envs.getOrDefault("JAEGER_AGENT_HOST", "localhost");
  private static final Integer JAEGER_AGENT_PORT = new Integer(envs.getOrDefault("JAEGER_AGENT_PORT", "6831"));
  private static final Integer JAEGER_FLUSH_INTERVAL = new Integer(envs.getOrDefault("JAEGER_FLUSH_INTERVAL", "100"));
  private static final Integer JAEGER_MAX_PACKET_SIZE = new Integer(envs.getOrDefault("JAEGER_MAX_PACKET_SIZE", "0"));

  private final int expectedSpansCount;
  private final SpanCounter spanCounter;

  Main() {
    if ("elasticsearch".equals(STORAGE)) {
      spanCounter = new ElasticsearchUtils(ELASTIC_HOSTNAME, 9200);
    } else if ("jaeger-query".equals(STORAGE)) {
      spanCounter = new JaegerQuerySpanCounter(JAEGER_QUERY_URL, "PerfTest", THREAD_COUNT*ITERATIONS);
    } else {
      spanCounter = new CassandraUtils(CASSANDRA_CLUSTER_IP, CASSANDRA_KEYSPACE_NAME);
    }
    expectedSpansCount = ITERATIONS * THREAD_COUNT;
  }

  public static void main(String []args) throws Exception {
    new Main().createSpansTest();
  }

  public void createSpansTest() throws Exception {
    logger.info("Starting with " + THREAD_COUNT + " threads for " + ITERATIONS + " iterations with a delay of " + DELAY);

    long startTime = System.currentTimeMillis();
    ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
    List<Future<?>> futures = new ArrayList<>(THREAD_COUNT);
    JaegerTracer tracer = createJaegerTracer("PerfTest");
    for (int i = 0; i < THREAD_COUNT; i++) {
      String name = "Thread " + i;
      Runnable worker = new CreateSpansRunnable(tracer, name, ITERATIONS, DELAY, true);
      futures.add(executor.submit(worker));
    }
    for (Future<?> future: futures) {
      future.get();
    }
    executor.shutdownNow();
    executor.awaitTermination(1, TimeUnit.SECONDS);

    long endTime = System.currentTimeMillis();
    long duration = endTime - startTime;
    logger.info("Finished all " + THREAD_COUNT + " threads; Created " + expectedSpansCount + " spans" + " in " + duration/1000 + " seconds") ;
    startTime = System.currentTimeMillis();
    int spansCount = spanCounter.countUntilNoChange(expectedSpansCount);
    duration = System.currentTimeMillis() - startTime;
    logger.info("Expected number of spans {}, actual {} stored in {} ms", expectedSpansCount, spansCount, duration);
    spanCounter.close();
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
}

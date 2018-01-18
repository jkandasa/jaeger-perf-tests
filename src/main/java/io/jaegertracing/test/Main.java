package io.jaegertracing.test;

import static org.junit.Assert.assertEquals;

import com.uber.jaeger.Tracer;
import com.uber.jaeger.metrics.Metrics;
import com.uber.jaeger.metrics.NullStatsReporter;
import com.uber.jaeger.metrics.StatsFactoryImpl;
import com.uber.jaeger.reporters.CompositeReporter;
import com.uber.jaeger.reporters.LoggingReporter;
import com.uber.jaeger.reporters.RemoteReporter;
import com.uber.jaeger.reporters.Reporter;
import com.uber.jaeger.samplers.ConstSampler;
import com.uber.jaeger.senders.HttpSender;
import com.uber.jaeger.senders.Sender;
import com.uber.jaeger.senders.UdpSender;
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

  private static final String USE_AGENT_OR_COLLECTOR = envs.getOrDefault("USE_AGENT_OR_COLLECTOR", "collector");
  private static final Integer ITERATIONS = new Integer(envs.getOrDefault("ITERATIONS", "3000"));
  private static final Integer THREAD_COUNT = new Integer(envs.getOrDefault("THREAD_COUNT", "100"));
  private static final Integer DELAY = new Integer(envs.getOrDefault("DELAY", "0"));

  private static final String CASSANDRA_CLUSTER_IP = envs.getOrDefault("CASSANDRA_CLUSTER_IP", "localhost");
  private static final String CASSANDRA_KEYSPACE_NAME = envs.getOrDefault("CASSANDRA_KEYSPACE_NAME", "jaeger_v1_test");

  private static final String JAEGER_AGENT_HOST = envs.getOrDefault("JAEGER_AGENT_HOST", "localhost");
  private static final String JAEGER_COLLECTOR_HOST = envs.getOrDefault("JAEGER_COLLECTOR_HOST", "localhost");
  private static final String JAEGER_COLLECTOR_PORT = envs.getOrDefault("JAEGER_COLLECTOR_PORT", "14268");
  private static final Integer JAEGER_FLUSH_INTERVAL = new Integer(envs.getOrDefault("JAEGER_FLUSH_INTERVAL", "100"));
  private static final Integer JAEGER_MAX_PACKET_SIZE = new Integer(envs.getOrDefault("JAEGER_MAX_PACKET_SIZE", "0"));
  private static final Integer JAEGER_MAX_QUEUE_SIZE = new Integer(envs.getOrDefault("JAEGER_MAX_QUEUE_SIZE", "300000"));
  private static final Integer JAEGER_UDP_PORT = new Integer(envs.getOrDefault("JAEGER_UDP_PORT", "6831"));
  private static final String TEST_SERVICE_NAME = envs.getOrDefault("TEST_SERVICE_NAME", "standalone");
  private static final String USE_LOGGING_REPORTER = envs.getOrDefault("USE_LOGGING_REPORTER", "false");

  private Tracer tracer;

  public Main(Tracer tracer) {
    this.tracer = tracer;
  }

  public static void main(String []args) throws Exception {
    new Main(createJaegerTracer()).createSpansTest();
  }

  public void createSpansTest() throws Exception {
    logger.info("Starting with " + THREAD_COUNT + " threads for " + ITERATIONS + " iterations with a delay of " + DELAY);

    long startTime = System.currentTimeMillis();
    ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
    List<Future<?>> futures = new ArrayList<>(THREAD_COUNT);
    for (int i = 0; i < THREAD_COUNT; i++) {
      Runnable worker = new CreateSpansRunnable(tracer, "Thread " + i, ITERATIONS, DELAY);
      futures.add(executor.submit(worker));
    }
    for (Future<?> future: futures) {
      future.get();
    }
    executor.shutdown();
    executor.awaitTermination(1, TimeUnit.SECONDS);

    long endTime = System.currentTimeMillis();
    long duration = endTime - startTime;
    logger.info("Finished all " + THREAD_COUNT + " threads; Created " + THREAD_COUNT * ITERATIONS + " spans" + " in " + duration/1000 + " seconds") ;
    startTime = System.currentTimeMillis();
//    int spansCount = new CassandraUtils("localhost", "jaeger_v1_test").countSpansUntilNoChange();
    int spansCount = new ElasticsearchUtils("localhost", 9200).countSpansUntilNoChange();
    duration = System.currentTimeMillis() - startTime;
    final int expectedTraceCount = THREAD_COUNT * ITERATIONS;
    logger.info("Expected number of spans {}, actual {} stored in {} ms", expectedTraceCount, spansCount, duration);
    tracer.close();
  }

  public static Tracer createJaegerTracer() {
    Sender sender;
    if (USE_AGENT_OR_COLLECTOR.equalsIgnoreCase("agent")) {
      sender = new UdpSender(JAEGER_AGENT_HOST, JAEGER_UDP_PORT, JAEGER_MAX_PACKET_SIZE);
      logger.info("Using JAEGER tracer using agent on host [" + JAEGER_AGENT_HOST + "] port [" + JAEGER_UDP_PORT +
          "] Service Name [" + TEST_SERVICE_NAME + "] Max queue size: [" + JAEGER_MAX_QUEUE_SIZE + "]");
    } else {
      // use the collector
      String httpEndpoint = "http://" + JAEGER_COLLECTOR_HOST + ":" + JAEGER_COLLECTOR_PORT + "/api/traces";
      logger.info("HttpSender sends to {}", httpEndpoint);
      sender = new HttpSender(httpEndpoint);
      logger.info("Using JAEGER tracer using collector on host [" + JAEGER_COLLECTOR_HOST + "] port [" + JAEGER_COLLECTOR_PORT +
          "] Service Name [" + TEST_SERVICE_NAME + "] Max queue size: [" + JAEGER_MAX_QUEUE_SIZE + "]");
    }

    Reporter reporter = new CompositeReporter(
        new RemoteReporter(sender, JAEGER_FLUSH_INTERVAL, JAEGER_MAX_QUEUE_SIZE, new Metrics(new StatsFactoryImpl(new NullStatsReporter()))));
    if (USE_LOGGING_REPORTER.equalsIgnoreCase("true")) {
      reporter = new CompositeReporter(reporter, new LoggingReporter(logger));
    }

    return new com.uber.jaeger.Tracer.Builder(TEST_SERVICE_NAME, reporter, new ConstSampler(true))
        .build();
  }
}

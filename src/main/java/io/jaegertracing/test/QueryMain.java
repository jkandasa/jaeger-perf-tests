package io.jaegertracing.test;

import static io.jaegertracing.test.Main.JAEGER_QUERY_LIMIT;
import static io.jaegertracing.test.Main.JAEGER_QUERY_URL;
import static io.jaegertracing.test.Main.SERVICE_NAME;
import static io.jaegertracing.test.Main.getNonseseTags;
import static io.jaegertracing.test.Main.getTags;

import com.codahale.metrics.MetricRegistry;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Pavol Loffay
 */
public class QueryMain {

  /**
   * Before running make sure jaeger contains data reported by {@link Main} class.
   */
  public static void main(String []args) {
    MetricRegistry metricsRegistry = new MetricRegistry();

    JaegerQuery jaegerQuery = new JaegerQuery(JAEGER_QUERY_URL, metricsRegistry,
        SERVICE_NAME + "-thread-0", "thread-0", JAEGER_QUERY_LIMIT,
        Arrays.asList(getNonseseTags(), getTags()));
    jaegerQuery.executeQueries(20);
    jaegerQuery.close();
  }
}

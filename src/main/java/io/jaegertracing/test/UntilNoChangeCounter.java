package io.jaegertracing.test;

import static org.awaitility.Awaitility.await;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pavol Loffay
 */
public abstract class UntilNoChangeCounter implements SpanCounter {

  private static final Logger logger = LoggerFactory.getLogger(UntilNoChangeCounter.class);

  protected MetricRegistry metricRegistry;
  private Timer queryTimer;
  private Timer queryUntilNoChangeTimer;

  public UntilNoChangeCounter(MetricRegistry metricRegistry) {
    this.metricRegistry = metricRegistry;
    this.queryTimer = metricRegistry.timer("query-single");
    this.queryUntilNoChangeTimer = metricRegistry.timer("query-until-no-change");
  }

  @Override
  public int countUntilNoChange(int expected) {
    long startUntilNoChange = System.currentTimeMillis();

    await().atMost(5, TimeUnit.HOURS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
      long start = System.currentTimeMillis();
      int spansCount = count();
      long duration = System.currentTimeMillis() - start;
      queryTimer.update(duration, TimeUnit.MICROSECONDS);
      logger.info("Count took: {}s, {} number of spans returned", TimeUnit.MILLISECONDS.toSeconds(duration), spansCount);
      return expected <= spansCount;
    });
    int count = count();
    queryUntilNoChangeTimer.update(System.currentTimeMillis() - startUntilNoChange, TimeUnit.MILLISECONDS);
    return count;
  }
}

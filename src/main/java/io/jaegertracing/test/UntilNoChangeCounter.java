package io.jaegertracing.test;

import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pavol Loffay
 */
public abstract class UntilNoChangeCounter implements SpanCounter {

  private static final Logger logger = LoggerFactory.getLogger(UntilNoChangeCounter.class);

  @Override
  public int countUntilNoChange(int expected) {
    await().atMost(5, TimeUnit.HOURS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
      long start = System.currentTimeMillis();
      int spansCount = count();
      long end = System.currentTimeMillis() - start;
      logger.info("Count took: {}s, {} number of spans returned", TimeUnit.MILLISECONDS.toSeconds(end), spansCount);
      return expected <= spansCount;
    });
    return count();
  }
}

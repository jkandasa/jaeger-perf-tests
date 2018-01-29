package io.jaegertracing.test;

import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;

/**
 * @author Pavol Loffay
 */
public abstract class UntilNoChangeCounter implements SpanCounter {

  @Override
  public int countUntilNoChange(int expected) {
    await().atMost(5, TimeUnit.HOURS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
      int spansCount = count();
      return expected <= spansCount;
    });
    return count();
  }
}

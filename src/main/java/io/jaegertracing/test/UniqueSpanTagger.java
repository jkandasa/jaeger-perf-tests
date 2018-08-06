package io.jaegertracing.test;

import io.opentracing.Span;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Pavol Loffay
 */
public class UniqueSpanTagger {

  private AtomicInteger counter = new AtomicInteger(0);

  public void setTag(Span span) {
    int id = counter.getAndIncrement();
    span.setTag(getTagKey(id), id);
  }

  public String getTagKey(int id) {
    return String.format("unique-tag-%d", id);
  }
}

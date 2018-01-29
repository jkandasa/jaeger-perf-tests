package io.jaegertracing.test;

import java.io.Closeable;

/**
 * @author Pavol Loffay
 */
public interface SpanCounter extends Closeable {
  int countUntilNoChange(int expected);
  int count();
}

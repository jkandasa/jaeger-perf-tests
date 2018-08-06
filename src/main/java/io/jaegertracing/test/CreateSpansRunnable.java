package io.jaegertracing.test;

import io.jaegertracing.internal.JaegerTracer;
import io.opentracing.Span;
import io.opentracing.tag.Tags;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pavol Loffay
 */
public class CreateSpansRunnable implements Runnable {
  private static final Logger log = LoggerFactory.getLogger(CreateSpansRunnable.class);
  private JaegerTracer tracer;
  private String name;
  private int count;
  private int delay;
  private boolean close;
  private UniqueSpanTagger uniqueSpanTagger = new UniqueSpanTagger();

  public CreateSpansRunnable(JaegerTracer tracer, String name, int count, int delay, boolean close) {
    this.tracer = tracer;
    this.name = name;
    this.count = count;
    this.delay = delay;
    this.close = close;
  }

  @Override
  public void run() {
    log.debug("Starting " + name);
    Map<String, Object> logs = new HashMap<>();
    logs.put("event", Tags.ERROR);
    logs.put("error.object", new RuntimeException());
    logs.put("class", this.getClass().getName());
    for (int i = 0; i < count; i++) {
      // emulate client spans
      Span span = tracer.buildSpan(String.format("thread: %s, iteration: %d", name, i))
          .withTag(Tags.COMPONENT.getKey(), "perf-test")
          .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT)
          .withTag(Tags.HTTP_METHOD.getKey(), "get")
          .withTag(Tags.HTTP_STATUS.getKey(), 200)
          .withTag(Tags.HTTP_URL.getKey(), "http://www.example.com/foo/bar?q=bar")
          .start();
      uniqueSpanTagger.setTag(span);
      span.log(logs);
      span.finish();
      try {
        TimeUnit.MILLISECONDS.sleep(delay);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    if (close) {
      tracer.close();
    }
  }

}

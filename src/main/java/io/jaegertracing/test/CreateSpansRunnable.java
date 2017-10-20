package io.jaegertracing.test;

import com.uber.jaeger.Tracer;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pavol Loffay
 */
public class CreateSpansRunnable implements Runnable {
  private static final Logger log = LoggerFactory.getLogger(CreateSpansRunnable.class);
  private Tracer tracer;
  private String id;
  private int count;
  private int delay;

  public CreateSpansRunnable(Tracer tracer, String id, int count, int delay) {
    this.tracer = tracer;
    this.id = id;
    this.count = count;
    this.delay = delay;
  }

  @Override
  public void run() {
    log.debug("Starting " + id);
    for (int i = 0; i < count; i++) {
      tracer.buildSpan(id).startManual().finish();
      try {
        TimeUnit.MILLISECONDS.sleep(delay);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

}

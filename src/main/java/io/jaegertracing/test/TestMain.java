package io.jaegertracing.test;

import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.JaegerTracer.Builder;
import io.jaegertracing.internal.reporters.RemoteReporter;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.jaegertracing.thrift.internal.senders.HttpSender;
import io.opentracing.Span;
import io.opentracing.tag.Tags;
import java.util.HashMap;

/**
 * @author Pavol Loffay
 */
public class TestMain {

  public static void main(String []args) {
    JaegerTracer tracer = new Builder("test")
        .withSampler(new ConstSampler(true))
        .withReporter(new RemoteReporter.Builder()
            .withSender(new HttpSender.Builder("http://localhost:14268/api/traces").build())
            .build())
        .build();

    Span span = tracer.buildSpan("foo")
        .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT)
        // java client does not support repeated keys!
        .withTag("a", "foo")
        .withTag("a.a", "true")
        .withTag("a.b", true)
        .withTag("a.c", 1)
        .withTag("a.a.a", "foo")
        .start();

    HashMap<String, Object> logs = new HashMap<>();
    logs.put("foo", "bar");
    span.log(logs);
    span.finish();

    tracer.close();
  }
}

package io.jaegertracing.test;

import com.uber.jaeger.Tracer;
import com.uber.jaeger.Tracer.Builder;
import com.uber.jaeger.exceptions.SenderException;
import com.uber.jaeger.metrics.Metrics;
import com.uber.jaeger.metrics.NullStatsReporter;
import com.uber.jaeger.metrics.StatsFactoryImpl;
import com.uber.jaeger.reporters.InMemoryReporter;
import com.uber.jaeger.reporters.LoggingReporter;
import com.uber.jaeger.reporters.RemoteReporter;
import com.uber.jaeger.samplers.ConstSampler;
import com.uber.jaeger.senders.HttpSender;

/**
 * @author Pavol Loffay
 */
public class Delete {

  public static void main(String[] args) throws SenderException {
    LoggingReporter loggingReporter = new LoggingReporter();

    HttpSender httpSender = new HttpSender("http://localhost:14268/api/traces", 6500);
    RemoteReporter remoteReporter = new RemoteReporter(httpSender, 100, 65000,
        new Metrics(new StatsFactoryImpl(new NullStatsReporter())));

    Tracer tracer = new Builder("test", remoteReporter,
        new ConstSampler(true)).build();

    System.out.println("before close");
//    httpSender.close();
//    remoteReporter.close();
    tracer.close();
    System.out.println("after close");
  }
}

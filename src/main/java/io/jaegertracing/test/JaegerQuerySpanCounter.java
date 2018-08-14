package io.jaegertracing.test;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jaegertracing.test.model.Result;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author Pavol Loffay
 */
public class JaegerQuerySpanCounter extends UntilNoChangeCounter {

  private OkHttpClient okClient;
  private ObjectMapper objectMapper;
  private Set<Request> requests = new LinkedHashSet<>();
  private boolean async;

  public JaegerQuerySpanCounter(
      String queryUrl,
      long limit, Set<String> serviceNames,
      boolean async,
      MetricRegistry metricRegistry
  ) {
    super(metricRegistry);
    Timer jaegerQueryTimer = super.metricRegistry.timer("jaeger-query");

    this.okClient = new OkHttpClient.Builder()
        .readTimeout(10, TimeUnit.MINUTES)
        .addInterceptor(chain -> {
          long start = System.currentTimeMillis();
          Response response = chain.proceed(chain.request());
          long duration = System.currentTimeMillis() - start;
          jaegerQueryTimer.update(duration, TimeUnit.MILLISECONDS);
          System.out.printf("%s --> in %ds\n", response, TimeUnit.MILLISECONDS.toSeconds(duration));
          return response;
        })
        .build();
    this.objectMapper = new ObjectMapper();
    this.objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    for (String service: serviceNames) {
      Request request = new Request.Builder()
          .url(String.format("%s/api/traces?service=%s&limit=%d", queryUrl, service, limit)).build();
      this.requests.add(request);
    }
    this.async = async;
  }

  @Override
  public int count() {
    return async ? getAsync() : getSync();
  }

  private int getSync() {
    int totalCount = 0;
    for (Request request: requests) {
      try {
        Response response = okClient.newCall(request)
            .execute();
        String body = response.body().string();
        Result result = objectMapper.readValue(body, Result.class);
        System.out.printf("---> %d spans\n", result.getData().size());
        totalCount += result.getData().size();
        response.close();
      } catch (IOException e) {
        e.printStackTrace();
        return 0;
      }
    }
    return totalCount;
  }

  private int getAsync() {
    AtomicInteger totalCount = new AtomicInteger(0);
    CountDownLatch latch = new CountDownLatch(requests.size());
    for (Request request: requests) {
      okClient.newCall(request).enqueue(new Callback() {
        @Override
        public void onFailure(Call call, IOException e) {
          e.printStackTrace();
          latch.countDown();
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
          Result result = parseResult(response);
          totalCount.addAndGet(result.getData().size());
          System.out.printf("---> %d spans\n", result.getData().size());
          response.close();
          latch.countDown();
        }
      });
    }
    try {
      latch.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return totalCount.get();

  }

  private Result parseResult(Response response) throws IOException {
    String body = response.body().string();
    return objectMapper.readValue(body, Result.class);
  }

  @Override
  public void close() {
     okClient.dispatcher().executorService().shutdown();
     okClient.connectionPool().evictAll();
  }
}

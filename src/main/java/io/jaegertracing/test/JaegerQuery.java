package io.jaegertracing.test;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import jnr.x86asm.HINT;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.Response;

/**
 * @author Pavol Loffay
 */
public class JaegerQuery implements Closeable {
  private static final int DEFAULT_LIMIT = 20;

  private OkHttpClient okClient;
  private ObjectMapper objectMapper;
  private MetricRegistry metricRegistry;
  private Map<String, Timer> timersMap;

  public JaegerQuery(
      String queryUrl,
      MetricRegistry metricRegistry,
      String service,
      String operation,
      int highLimit,
      List<Map<String, String>> tagsList
  ) {
    this.metricRegistry = metricRegistry;
    this.objectMapper = new ObjectMapper();
    this.objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    Timer jaegerQueryTimer = metricRegistry.timer("jaeger-query-");

    this.okClient = new OkHttpClient.Builder()
        .readTimeout(10, TimeUnit.MINUTES)
        .build();

    String urlServiceLimit =          "%s/api/traces?service=%s&limit=%d&lookback=1h";
    String urlServiceLimitTags =      "%s/api/traces?service=%s&limit=%d&lookback=1h&tags=%s";
    String urlServiceLimitOperation = "%s/api/traces?service=%s&limit=%d&lookback=1h&operation=%s";

    List<String> urls = new ArrayList<>();
    urls.add(String.format(urlServiceLimit, queryUrl, service, DEFAULT_LIMIT));
    urls.add(String.format(urlServiceLimit, queryUrl, service, highLimit));
    urls.add(String.format(urlServiceLimitOperation, queryUrl, service, DEFAULT_LIMIT, operation));
    urls.add(String.format(urlServiceLimitOperation, queryUrl, service, highLimit, operation));

    for (Map<String, String> map: tagsList) {
      String tagsQueryString = getTagsQueryString(map);
      urls.add(String.format(urlServiceLimitTags, queryUrl, service, DEFAULT_LIMIT, tagsQueryString));
      urls.add(String.format(urlServiceLimitTags, queryUrl, service, highLimit, tagsQueryString));
    }

    timersMap = createTimers(urls);
  }

  public void executeQueries(int iterations) {
    for (int i = 0; i < iterations; i++) {
      executeQueries();
    }
  }

  public void executeQueries() {
    for (Map.Entry<String, Timer> entry: timersMap.entrySet()) {
      try {
        long start = System.currentTimeMillis();
        Response response = okClient.newCall(new Builder()
            .url(entry.getKey())
            .build())
            .execute();

        if (!response.isSuccessful()) {
          System.out.println("Not successful request!\n\n");
        }

        response.body().string();
        entry.getValue().update(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);
        response.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private Map<String, Timer> createTimers(List<String> names) {
    HashMap<String, Timer> timersMap = new HashMap<>();
    for (String name: names) {
      timersMap.put(name, metricRegistry.timer(name));
    }
    return timersMap;
  }

  private String getTagsQueryString(Map<String, String> tags) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("{");
    for (Map.Entry<String, String> entry: tags.entrySet()) {
      if (stringBuilder.length() != 1) {
        stringBuilder.append(",");
      }
      stringBuilder.append(String.format("\"%s\":\"%s\"", entry.getKey(), entry.getValue()));
    }
    stringBuilder.append("}");
    return stringBuilder.toString();
  }

  @Override
  public void close() {
    okClient.dispatcher().executorService().shutdown();
    okClient.connectionPool().evictAll();
  }
}

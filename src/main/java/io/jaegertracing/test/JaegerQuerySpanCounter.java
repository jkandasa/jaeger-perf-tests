package io.jaegertracing.test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jaegertracing.test.model.Result;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author Pavol Loffay
 */
public class JaegerQuerySpanCounter extends UntilNoChangeCounter {

  private OkHttpClient okClient;
  private Request request;
  private ObjectMapper objectMapper;

  public JaegerQuerySpanCounter(String queryUrl, String service, long limit) {
    this.okClient = new OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .build();
    this.request = new Request.Builder()
        .url(String.format("%s/api/traces?service=%s&limit=%d", queryUrl, service, limit)).build();
    this.objectMapper = new ObjectMapper();
    this.objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }

  @Override
  public int count() {
    Result result = null;
    try {
      Response response = okClient.newCall(request)
          .execute();
      String body = response.body().string();
      System.out.println(response);
//      System.out.println(body);
      result = objectMapper.readValue(body, Result.class);
      response.close();
    } catch (IOException e) {
      e.printStackTrace();
      return 0;
    }
    return result.getData().size();
  }

  @Override
  public void close() {
     okClient.dispatcher().executorService().shutdown();
     okClient.connectionPool().evictAll();
  }
}

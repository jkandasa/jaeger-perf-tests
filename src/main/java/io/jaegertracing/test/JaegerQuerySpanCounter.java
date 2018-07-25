package io.jaegertracing.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jaegertracing.test.model.Result;
import java.io.IOException;
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
    this.okClient = new OkHttpClient();
    this.request = new Request.Builder()
        .url(String.format("%s/api/traces?service=%s&limit=%d", queryUrl, service, limit)).build();
    this.objectMapper = new ObjectMapper();
  }

  @Override
  public int count() {
    Result result = null;
    try {
      Response response = okClient.newCall(request)
          .execute();
      result = objectMapper.readValue(response.body().bytes(), Result.class);
    } catch (IOException e) {
      e.printStackTrace();
      return 0;
    }
    return result.getData().size();
  }

  @Override
  public void close() {
  }
}

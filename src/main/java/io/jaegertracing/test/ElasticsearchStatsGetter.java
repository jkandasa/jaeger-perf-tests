package io.jaegertracing.test;

import static io.jaegertracing.test.ElasticsearchSpanCounter.getSpanIndex;

import java.io.Closeable;
import java.io.IOException;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pavol Loffay
 */
public class ElasticsearchStatsGetter implements Closeable {
  private static final Logger log = LoggerFactory.getLogger(ElasticsearchStatsGetter.class);


  private final RestClient restClient;
  private final String spanIndex;

  public ElasticsearchStatsGetter(String host, int port) {
    this.restClient = ElasticsearchSpanCounter.getESRestClient(host, port);
    this.spanIndex = getSpanIndex();

  }

  public String getStats() {
    try {
      Response response = restClient.performRequest("GET", "/" + spanIndex + "/_stats?pretty");
      return EntityUtils.toString(response.getEntity());
    } catch (IOException e) {
      e.printStackTrace();
    }
    return "Unknown ES stats";
  }

  @Override
  public void close() throws IOException {
    restClient.close();
  }
}

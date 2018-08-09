package io.jaegertracing.test;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.apache.http.HttpHost;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pavol Loffay
 */
public class ElasticsearchSpanCounter extends UntilNoChangeCounter {
  private static final Logger log = LoggerFactory.getLogger(ElasticsearchSpanCounter.class);

  private final String spanIndex;
  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  public ElasticsearchSpanCounter(String host, int port) {
    this.restClient = getESRestClient(host, port);
    this.objectMapper = new ObjectMapper();
    this.spanIndex = getSpanIndex();
  }

  public void refreshSpanIndex() {
    try {
      Response response = restClient.performRequest("GET", "/" + spanIndex + "/_refresh");
      if (response.getStatusLine().getStatusCode() >= 400) {
        throw new RuntimeException("Could not refresh span index");
      }
    } catch (IOException ex) {
      log.error("Could not make request to refresh span index", ex);
//      throw new RuntimeException("Could not make request to refresh span index", ex);
    }
  }

  @Override
  public int count() {
    refreshSpanIndex();
    try {
      Response response = restClient.performRequest("GET", "/" + spanIndex + "/_count");
      String responseBody = EntityUtils.toString(response.getEntity());
      JsonNode jsonPayload = objectMapper.readTree(responseBody);
      JsonNode count = jsonPayload.get("count");
      int spansCount = count.asInt();
      log.info("found {} traces in ES", spansCount);
      return spansCount;
    } catch (IOException ex) {
      log.error("Could not make request to count span index", ex);
      return -1;
    }
  }

  static RestClient getESRestClient(String host, int port) {
    return RestClient.builder(
        new HttpHost(host, port, "http"))
        .build();
  }

  static String getSpanIndex() {
    String formattedDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    String spanIndex = "jaeger-span-" + formattedDate;
    log.info("Using ElasticSearch index : [" + spanIndex + "]" );
    return spanIndex;
  }

  public void close() throws IOException {
    restClient.close();
  }
}

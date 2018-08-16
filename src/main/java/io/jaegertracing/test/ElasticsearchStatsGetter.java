package io.jaegertracing.test;

import static io.jaegertracing.test.ElasticsearchSpanCounter.getSpanIndex;

import java.io.Closeable;
import java.io.IOException;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

/**
 * @author Pavol Loffay
 */
public class ElasticsearchStatsGetter implements Closeable {

  private static final String INDEX_STATS = "/_stats?pretty";
  private static final String NODES = "/_cat/nodes?v&h=hm,hc,rm,rc,iiti,iito,sqti,sqto";
  private static final String THREAD_POOL = "/_cat/thread_pool/generic?v&h=id,name,active,rejected,completed";

  private final RestClient restClient;
  private final String spanIndex;

  public ElasticsearchStatsGetter(String host, int port) {
    this.restClient = ElasticsearchSpanCounter.getESRestClient(host, port);
    this.spanIndex = getSpanIndex();

  }

  public void printStats() {
    try {
      Response indexStats = restClient.performRequest("GET", "/" + spanIndex + INDEX_STATS);
      // https://www.elastic.co/guide/en/elasticsearch/reference/6.3/cat-nodes.html
      Response nodeCat = restClient.performRequest("GET", NODES);
      // https://www.elastic.co/guide/en/elasticsearch/reference/6.3/cat-thread-pool.html
      Response threadPool = restClient.performRequest("GET", THREAD_POOL);
      System.out.printf("%s\n%s\n", nodeCat.getRequestLine().getUri(), EntityUtils.toString(nodeCat.getEntity()));
      System.out.printf("%s\n%s\n", threadPool.getRequestLine().getUri(), EntityUtils.toString(threadPool.getEntity()));
      System.out.printf("%s\n%s\n", indexStats.getRequestLine().getUri(), EntityUtils.toString(indexStats.getEntity()));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void close() throws IOException {
    restClient.close();
  }
}

package io.jaegertracing.test;

import com.codahale.metrics.MetricRegistry;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pavol Loffay
 */
public class CassandraSpanCounter extends UntilNoChangeCounter {
  private static final Logger log = LoggerFactory.getLogger(CassandraSpanCounter.class);

  private final Session session;

  public CassandraSpanCounter(String contactPoint, String keyspace, MetricRegistry metricRegistry) {
    super(metricRegistry);
    this.session = getCassandraSession(contactPoint, keyspace);
  }

  private Session getCassandraSession(String contactPoint, String keyspace) {
    Cluster cluster = Cluster.builder()
        .addContactPoint(contactPoint)
        .build();
    return cluster.connect(keyspace);
  }

  @Override
  public int count() {
    ResultSet result = session.execute("select * from traces");
    int spansCount = result.all().size();
    log.info("found {} traces in Cassandra", spansCount);
    return spansCount;
  }

  @Override
  public void close() {
    session.close();
  }
}

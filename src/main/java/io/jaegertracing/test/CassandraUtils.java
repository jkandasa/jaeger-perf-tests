package io.jaegertracing.test;

import static org.awaitility.Awaitility.await;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pavol Loffay
 */
public class CassandraUtils extends UntilNoChangeCounter {
  private static final Logger log = LoggerFactory.getLogger(CassandraUtils.class);

  private final Session session;

  public CassandraUtils(String contactPoint, String keyspace) {
    this.session = getCassandraSession(contactPoint, keyspace);
  }

  private Session getCassandraSession(String contactPoint, String keyspace) {
    Cluster cluster = Cluster.builder()
        .addContactPoint(contactPoint)
        .build();
    return cluster.connect(keyspace);
  }

  public int count() {
    ResultSet result = session.execute("select * from traces");
    int spansCount = result.all().size();
    log.info("found {} traces in Cassandra", spansCount);
    return spansCount;
  }

  @Override
  public void close() throws IOException {
    session.close();
  }
}

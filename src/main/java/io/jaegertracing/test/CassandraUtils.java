package io.jaegertracing.test;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pavol Loffay
 */
public class CassandraUtils {
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

  public int countTracesInCassandra() {
    ResultSet result = session.execute("select * from traces");
    return result.all().size();
  }

  public int countSpansUntilNoChange() {
    int spansCount = 0;
    boolean change = true;
    while (change) {
      int previous = spansCount;
      spansCount = countTracesInCassandra();
      log.info("found {} traces in Cassandra", spansCount);
      change = spansCount != previous;
      try {
        TimeUnit.MILLISECONDS.sleep(500);
      } catch (InterruptedException e) {
        log.error("Could not sleep between getting data from cassandra", e);
        e.printStackTrace();
      }
    }
    return spansCount;
  }
}

# Jaeger performance tests

This repository contains simple code which emits spans to jaeger services. 
It can be used to test performance of your Jaeger deployment.

To deploy Jaeger services follow official documentation or: https://gist.github.com/pavolloffay/20cc357b1c951e15fdd046deb5eb64d2

Note that some flags can affect performance of Jaeger services. If you know expected number of spans
queue sizes on agent and collector services should be adjusted accordingly:

Agent: `--processor.jaeger-compact.server-queue-size=N`
Collector: `--collector.queue-size=300000`

# Run tests

Create 300.000 spans

```bash
THREAD_COUNT=100 ITERATIONS=3000 JAEGER_MAX_QUEUE_SIZE=100000 USE_AGENT_OR_COLLECTOR=collector mvn exec:java
```

### Remove spans from Cassandra
```bash
echo "truncate jaeger_v1_test.traces;" | ccm node1 cqlsh
```

After the deletion verify that number of stored spans is zero.

### Get a count of spans in Cassandra
```bash
mvn clean TODO
```

or 

```bash
echo "SELECT COUNT(*) FROM jaeger_v1_test.traces;" | ccm node1 cqlsh
```

# Run on Kubernetes 
```bash
eval $(minishift docker-env)
mvn package -DskipTests=true  && docker build -t jaeger-perf-tests:latest .
kubectl run perf-tests --env CASSANDRA_CLUSTER_IP=cassandra --env CASSANDRA_KEYSPACE_NAME=jaeger_v1_dc1 --env JAEGER_COLLECTOR_HOST=jaeger-collector --env JAEGER_COLLECTOR_PORT=14268 --image-pull-policy=IfNotPresent --restart=Never --image=jaeger-perf-tests:latest
```

### Troubleshooting
Agent exposes metrics on http://localhost:5778/debug/vars.



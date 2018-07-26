# Jaeger performance tests

This repository contains simple code which emits spans to jaeger services. 
It can be used to test performance of your Jaeger deployment.

To deploy Jaeger services follow official documentation or: https://gist.github.com/pavolloffay/20cc357b1c951e15fdd046deb5eb64d2

Note that some flags can affect performance of Jaeger services. If you know expected number of spans
queue sizes on agent and collector services should be adjusted accordingly:

Agent: `--processor.jaeger-compact.server-queue-size=N`
Collector: `--collector.queue-size=300000`

## Run tests

Create 300k spans

```bash
QUERY_FROM=jaeger-query ITERATIONS=3000 THREAD_COUNT=100 mvn clean package exec:java
```

* `QUERY_FROM` can be set to `jaeger-query`, `elasticsearch`, `cassandra`

### Remove spans from Cassandra
```bash
echo "truncate jaeger_v1_test.traces;" | ccm node1 cqlsh
```

After the deletion verify that number of stored spans is zero.


```bash
echo "SELECT COUNT(*) FROM jaeger_v1_test.traces;" | ccm node1 cqlsh
```

## Run on Kubernetes 
```bash
eval $(minikube docker-env)
mvn package -DskipTests=true  && docker build -t jaeger-perf-tests:latest .
# cassandra
kubectl run perf-tests --env STORAGE=cassandra --env CASSANDRA_CLUSTER_IP=cassandra --env CASSANDRA_KEYSPACE_NAME=jaeger_v1_dc1 --env JAEGER_COLLECTOR_HOST=jaeger-collector --env JAEGER_COLLECTOR_PORT=14268 --image-pull-policy=IfNotPresent --restart=Never --image=jaeger-perf-tests:latest
# elasticsearch
kubectl run perf-tests --env ELASTIC_HOSTNAME=my-release-elasticsearch-client --env JAEGER_COLLECTOR_HOST=jaeger-collector --env JAEGER_COLLECTOR_PORT=14268 --image-pull-policy=IfNotPresent --restart=Never --image=jaeger-perf-tests:latest


kubectl logs -f  po/perf-tests
kubectl delete po/perf-tests
```

### Troubleshooting
Agent exposes metrics on http://localhost:5778/debug/vars.



## Simple standalone example of writing spans to Jaeger

# Start Cassandra and create the keyspace
+ `docker run --name=cassandra --rm -it -p 7000:7000 -p 9042:9042 cassandra:3.9`
+ `MODE=test ./plugin/storage/cassandra/schema/create.sh | cqlsh `

# Start the Collector and Agent
+ `export CASSANDRA_KEYSPACE_NAME=jaeger_v1_test`
+ `export CASSANDRA_CLUSTER_IP=<ctual ip of cassandra is running on, not localhost`
+ `docker run -it -e CASSANDRA_SERVERS=192.168.0.118 -e CASSANDRA_KEYSPACE=${CASSANDRA_KEYSPACE_NAME} --rm -p14267:14267 -p14268:14268 jaegertracing/jaeger-collector:latest` 
+ `docker run -it -e PROCESSOR_JAEGER_BINARY_SERVER_QUEUE_SIZE=100000 -e PROCESSOR_JAEGER_COMPACT_SERVER_QUEUE_SIZE=100000 -e COLLECTOR_HOST_PORT=${CASSANDRA_CLUSTER_IP}:14267 -p5775:5775/udp -p6831:6831/udp -p6832:6832/udp -p5778:5778/tcp jaegertracing/jaeger-agent:latest`
# Optional: Start the UI
+ `docker run -it -e CASSANDRA_SERVERS=${CASSANDRA_CLUSTER_IP} -e CASSANDRA_KEYSPACE=${CASSANDRA_KEYSPACE_NAME} -p16686:16686  jaegertracing/jaeger-query:latest`
`
Some flags can affect performance of Jaeger services. If we know expected number of spans
we should appropriately adjust queue sizes on agent and collector services:

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

### Just get a count of traces in Cassandra
```bash
mvn clean -Dtest=SimpleTest#countTraces test
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



pipeline {
    agent any
    tools {
        maven 'maven-3.5.3'
        jdk 'jdk8'
    }
    options {
        disableConcurrentBuilds()
        /*timeout(time: 8, unit: 'HOURS')*/
    }
    environment {
        JAEGER_AGENT_HOST = "localhost"
        JAEGER_COLLECTOR_HOST = "jaeger-collector"
        JAEGER_COLLECTOR_PORT = 14268
        JAEGER_QUERY_HOST = "jaeger-query"
        JAEGER_QUERY_PORT = 80
        JAEGER_QUERY_SERVICE_PORT = 80
        JAEGER_QUERY_URL = "http://jaeger-query:80"
        ELASTICSEARCH_HOST = "elasticsearch"
        ELASTICSEARCH_PORT = "9200"
        CASSANDRA_CLUSTER_IP = "cassandra"
        CASSANDRA_KEYSPACE_NAME="jaeger_v1_dc1"
        DEPLOYMENT_PARAMETERS="-pIMAGE_VERSION=latest -pCOLLECTOR_PODS=${COLLECTOR_PODS}"
        LOGS_COLLECTED="false"
    }
    parameters {
        choice(choices: 'elasticsearch\ncassandra',  name: 'SPAN_STORAGE_TYPE')
        
        choice(choices: 'http\nudp',  name: 'SENDER')
        string(name: 'NUMBER_OF_TRACERS', defaultValue: '10', description: 'The number of traces')
        string(name: 'NUMBER_OF_SPANS', defaultValue: '10', description: 'The number of spans')
        string(name: 'DELAY', defaultValue: '0', description: 'Delay')
        string(name: 'QUERY_FROM', defaultValue: 'jaeger-query', description: 'query from')
        string(name: 'JAEGER_QUERY_ASYNC', defaultValue: 'true', description: 'Jaeger query async')
        string(name: 'JAEGER_QUERY_LIMIT', defaultValue: '20000', description: 'Jaeger query limit')
        string(name: 'JAEGER_FLUSH_INTERVAL', defaultValue: '100', description: 'Jaeger flush interval')
        string(name: 'JAEGER_MAX_PACKET_SIZE', defaultValue: '0', description: 'Jaeger max pocket size')

        
        string(name: 'COLLECTOR_QUEUE_SIZE', defaultValue: '2000', description: '--collector.queue-size')
        string(name: 'COLLECTOR_NUM_WORKERS', defaultValue: '50', description: '--collector.num-workers')

        string(name: 'ES_IMAGE', defaultValue: 'registry.centos.org/rhsyseng/elasticsearch:5.5.2', description: 'ElasticSearch image.')
        string(name: 'ES_IMAGE_INSECURE', defaultValue: 'false', description: 'If image location not-secured(for HTTP) change to true')
        string(name: 'ES_MEMORY', defaultValue: '1Gi', description: 'Memory for each elasticsearch pod')
        string(name: 'ES_BULK_SIZE', defaultValue: '5000000', description: '--es.bulk.size')
        string(name: 'ES_BULK_WORKERS', defaultValue: '1', description: '--es.bulk.workers')
        string(name: 'ES_BULK_FLUSH_INTERVAL', defaultValue: '200ms', description: '--es.bulk.flush-interval')

        string(name: 'QUERY_STATIC_FILES', defaultValue: '', description: '--query.static-files')

        booleanParam(name: 'RUN_SMOKE_TESTS', defaultValue: false, description: 'Run smoke tests in addition to the performance tests')
        booleanParam(name: 'DELETE_JAEGER_AT_END', defaultValue: true, description: 'Delete Jaeger instance at end of the test')
        booleanParam(name: 'DELETE_JOB_AT_END', defaultValue: true, description: 'Delete test pods at end of the test')

        string(name: 'JAEGER_SAMPLING_RATE', defaultValue: '1.0', description: '0.0 to 1.0 percent of spans to record')
        string(name: 'JAEGER_AGENT_IMAGE', defaultValue: 'jaegertracing/jaeger-agent:latest', description: 'Jaeger agent Image')
        string(name: 'JAEGER_COLLECTOR_IMAGE', defaultValue: 'jaegertracing/jaeger-collector:latest', description: 'Jaeger collector image')
        string(name: 'JAEGER_QUERY_IMAGE', defaultValue: 'jaegertracing/jaeger-query:latest', description: 'Jaeger query image')
    }
    stages {
        stage('Set name and description') {
            steps {
                script {
                    currentBuild.displayName =params.SPAN_STORAGE_TYPE + " " + params.SENDER + " " + params.NUMBER_OF_TRACERS + " tracers " + params.NUMBER_OF_TRACERS + " spans and delay " + params.DELAY
                    currentBuild.description = currentBuild.displayName
                }
            }
        }
        stage('Delete Jaeger') {
            steps {
                sh 'oc delete -f https://raw.githubusercontent.com/RHsyseng/docker-rhel-elasticsearch/5.x/es-cluster-deployment.yml --grace-period=1 || true'
                sh 'oc delete all,template,daemonset,configmap -l jaeger-infra || true'
                sh 'env | sort'
            }
        }
        stage('Delete Old Job') {
            steps {
                sh 'oc delete job jaeger-standalone-performance-tests || true'
            }
        }
        stage('Cleanup, checkout, build') {
            steps {
                deleteDir()
                checkout scm
                sh 'ls -alF'
            }
        }
        stage('deploy Cassandra') {
            when {
                expression { params.SPAN_STORAGE_TYPE == 'cassandra'}
            }
            steps {
                sh '''
                    curl https://raw.githubusercontent.com/jaegertracing/jaeger-openshift/master/production/cassandra.yml --output cassandra.yml
                    oc create --filename cassandra.yml
                '''
            }
        }
        stage('deploy ElasticSearch') {
            when {
                expression { params.SPAN_STORAGE_TYPE == 'elasticsearch'}
            }
            steps {
                sh ' ./scripts/execute-es-cluster-deployment.sh'
            }
        }
        stage('deploy Jaeger with Cassandra') {
            when {
                expression { params.SPAN_STORAGE_TYPE == 'cassandra'}
            }
            steps {
                sh '''
                    curl https://raw.githubusercontent.com/jaegertracing/jaeger-openshift/master/production/configmap-cassandra.yml --output configmap-cassandra.yml
                    oc create -f ./configmap-cassandra.yml
                    curl https://raw.githubusercontent.com/jaegertracing/jaeger-openshift/master/production/jaeger-production-template.yml --output jaeger-production-template.yml
                    sed -i 's/jaegertracing\\/jaeger-collector:${IMAGE_VERSION}/'${JAEGER_COLLECTOR_IMAGE}'/g' jaeger-production-template.yml
                    sed -i 's/jaegertracing\\/jaeger-query:${IMAGE_VERSION}/'${JAEGER_QUERY_IMAGE}'/g' jaeger-production-template.yml
                    grep "image:" jaeger-production-template.yml
                    ./scripts/updateTemplateForCassandra.sh
                    oc process  ${DEPLOYMENT_PARAMETERS} -f jaeger-production-template.yml  | oc create -n ${PROJECT_NAME} -f -
                '''
            }
        }
        stage('deploy Jaeger with ElasticSearch') {
            when {
                expression { params.SPAN_STORAGE_TYPE == 'elasticsearch'}
            }
            steps {
                sh './scripts/deploy_jaeger_elasticsearch.sh'
            }
        }
        stage('Wait for Jaeger Deployment') {
            steps {
                openshiftVerifyService apiURL: '', authToken: '', namespace: '', svcName: 'jaeger-query', verbose: 'false'
                openshiftVerifyService apiURL: '', authToken: '', namespace: '', svcName: 'jaeger-collector', verbose: 'false'
            }
        }
        stage('Run performance tests'){
            steps{
                sh '''
                    pwd
                    git status
                    mvn clean install
                    mvn dependency:tree
                    mvn --activate-profiles openshift clean install fabric8:deploy \
                    	-Dsender=${SENDER} \
                    	-Dnumber.of.spans=${NUMBER_OF_SPANS} \
                    	-Dnumber.of.tracers=${NUMBER_OF_TRACERS} \
                    	-Ddelay=${DELAY} \
                    	-Dquery.from=${QUERY_FROM} \
                    	-Delastic.hostname=${ELASTIC_HOSTNAME} \
                    	-Djaeger.query.url=${JAEGER_QUERY_URL} \
                    	-Djaeger.query.async=${JAEGER_QUERY_ASYNC} \
                    	-Djaeger.query.limit=${JAEGER_QUERY_LIMIT} \
                    	-Djaeger.collector.host=${JAEGER_COLLECTOR_HOST} \
                    	-Djaeger.collector.port=${JAEGER_COLLECTOR_PORT} \
                    	-Djaeger.agent.host=${JAEGER_AGENT_HOST} \
                    	-Djaeger.agent.port=${JAEGER_AGENT_PORT} \
                    	-Djaeger.flush.interval=${JAEGER_FLUSH_INTERVAL} \
                    	-Djaeger.max.pocket.size=${JAEGER_MAX_PACKET_SIZE}
                '''
            }
        }
        stage('Validate images and UI') {
            steps {
                sh '''
                    oc describe pod -l jaeger-infra=query-pod | grep "Image:"
                    oc describe pod -l jaeger-infra=collector-pod | grep "Image:"
                    oc describe pod --selector='job-name=jaeger-standalone-performance-tests' | grep "Image:"
                    curl -i ${JAEGER_QUERY_HOST}:/${JAEGER_QUERY_SERVICE_PORT}/search
                '''
            }
        }
        stage('Collect logs'){
            steps{
                sh '''
                  ./scripts/collect_logs.sh
                  export LOGS_COLLECTED="true"
                  '''
            }
        }
        stage('Delete Jaeger at end') {
            when {
                expression { params.DELETE_JAEGER_AT_END  }
            }
            steps {
                script {
                    sh 'oc delete -f https://raw.githubusercontent.com/RHsyseng/docker-rhel-elasticsearch/5.x/es-cluster-deployment.yml --grace-period=1 || true'
                    sh 'oc delete all,template,daemonset,configmap -l jaeger-infra || true'
                }
            }
        }
        stage('Delete Job at end') {
            when {
                expression { params.DELETE_JAEGER_AT_END  }
            }
            steps {
                sh 'oc delete job jaeger-standalone-performance-tests || true'
            }
        }
        stage('Cleanup build pods') {
            steps {
                script {
                    sh 'oc get pods | grep Completed | awk {"print \\$1"} | xargs oc delete pod || true'
                }
            }
        }
    }

    post{
        always{
            script{
                env.TRACE_COUNT=readFile'standalone/tracesCreatedCount.txt'
                env.TRACES_FOUND_COUNT=readFile'standalone/tracesFoundCount.txt'
                currentBuild.description =" Traces create " + env.TRACE_COUNT + " Traces found " + env.TRACES_FOUND_COUNT

            }
        }
        failure {
            script {
                if (env.LOGS_COLLECTED == 'false') {
                  sh './scripts/collect_logs.sh'
                }
            }
        }
    }
}

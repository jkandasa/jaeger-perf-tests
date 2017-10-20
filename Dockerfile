FROM openjdk:alpine

ENV APP_HOME /app/

COPY target/perf-tests-1.0-SNAPSHOT-jar-with-dependencies.jar $APP_HOME

WORKDIR $APP_HOME
CMD java -jar perf-tests-1.0-SNAPSHOT-jar-with-dependencies.jar

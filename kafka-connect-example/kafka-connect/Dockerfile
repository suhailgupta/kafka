# Kafka Connect with Debezium PostgreSQL source, JDBC sink, and S3 sink
# Stage 1: Resolve Confluent JDBC and S3 connectors + deps via Maven
FROM eclipse-temurin:17-jdk AS maven-connectors
WORKDIR /tmp/connectors
COPY connectors-pom.xml pom.xml
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/* \
    && mvn dependency:copy-dependencies -DoutputDirectory=./lib -B

# Stage 2: Kafka Connect image with all plugins
FROM confluentinc/cp-kafka-connect:7.5.0

# Install Debezium PostgreSQL connector from Maven
ARG DEBEZIUM_VERSION=2.4.2.Final
RUN mkdir -p /usr/share/confluent-hub-components/debezium-connector-postgres \
    && curl -sfSL "https://repo1.maven.org/maven2/io/debezium/debezium-connector-postgres/${DEBEZIUM_VERSION}/debezium-connector-postgres-${DEBEZIUM_VERSION}-plugin.tar.gz" \
    | tar xz -C /usr/share/confluent-hub-components/debezium-connector-postgres --strip-components=1

# Install Confluent JDBC and S3 connectors (JARs + deps from Maven stage)
RUN mkdir -p /usr/share/confluent-hub-components/confluentinc-kafka-connect-jdbc \
             /usr/share/confluent-hub-components/confluentinc-kafka-connect-s3
COPY --from=maven-connectors /tmp/connectors/lib /usr/share/confluent-hub-components/confluentinc-kafka-connect-jdbc/
COPY --from=maven-connectors /tmp/connectors/lib /usr/share/confluent-hub-components/confluentinc-kafka-connect-s3/

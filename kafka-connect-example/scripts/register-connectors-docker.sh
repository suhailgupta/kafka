#!/bin/sh
# Register connectors with Kafka Connect (idempotent: create or skip if exists)
set -e
CONNECT_URL="${CONNECT_URL:-http://kafka-connect:8083}"

echo "Waiting for Kafka Connect at $CONNECT_URL ..."
for i in $(seq 1 60); do
  if curl -sf "$CONNECT_URL/" > /dev/null 2>&1; then
    echo "Kafka Connect is up."
    break
  fi
  echo "  attempt $i/60..."
  sleep 5
done

curl -sf "$CONNECT_URL/" > /dev/null || { echo "Kafka Connect not available"; exit 1; }

for name in orders-db-source orders-to-analytics-jdbc-sink orders-to-s3-sink; do
  if curl -sf "$CONNECT_URL/connectors/$name" > /dev/null 2>&1; then
    echo "Connector $name already exists, skipping."
  else
    case $name in
      orders-db-source)       file="debezium-orders-source.json" ;;
      orders-to-analytics-jdbc-sink) file="jdbc-analytics-sink.json" ;;
      orders-to-s3-sink)      file="s3-orders-sink.json" ;;
      *) echo "Unknown connector $name"; continue ;;
    esac
    echo "Creating connector $name from $file..."
    curl -sf -X POST -H "Content-Type: application/json" \
      --data @"/connectors/$file" \
      "$CONNECT_URL/connectors" && echo "  OK" || echo "  Failed"
  fi
done

echo "Done. Connectors:"
curl -sf "$CONNECT_URL/connectors" || true

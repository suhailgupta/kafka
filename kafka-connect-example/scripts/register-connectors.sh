#!/bin/sh
# Register Kafka Connect connectors (run from project root after docker-compose up)
# Usage: ./scripts/register-connectors.sh
set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
CONNECT_URL="${CONNECT_URL:-http://localhost:8083}"
CONNECTORS_DIR="$PROJECT_ROOT/connectors"

echo "Waiting for Kafka Connect at $CONNECT_URL ..."
until curl -sf "$CONNECT_URL/" > /dev/null; do
  echo "  waiting..."
  sleep 3
done
echo "Kafka Connect is up."

echo "Creating Debezium PostgreSQL source connector..."
curl -sf -X POST -H "Content-Type: application/json" \
  --data @"$CONNECTORS_DIR/debezium-orders-source.json" \
  "$CONNECT_URL/connectors" || true

echo "Creating JDBC sink connector..."
curl -sf -X POST -H "Content-Type: application/json" \
  --data @"$CONNECTORS_DIR/jdbc-analytics-sink.json" \
  "$CONNECT_URL/connectors" || true

echo "Creating S3 sink connector..."
curl -sf -X POST -H "Content-Type: application/json" \
  --data @"$CONNECTORS_DIR/s3-orders-sink.json" \
  "$CONNECT_URL/connectors" || true

echo "Connectors registered. List:"
curl -sf "$CONNECT_URL/connectors" | jq .

#!/bin/bash

# OrderFlow Kafka Topic Creation Script
# Creates all event topics with 6 partitions (replication factor 1 for single-broker EC2 dev)

set -e

KAFKA_BROKER="${KAFKA_BROKER:-kafka:9092}"
PARTITIONS=6
REPLICATION=1
DLQ_PARTITIONS=3

echo "========================================="
echo "Creating Kafka topics on broker: $KAFKA_BROKER"
echo "========================================="

# Wait for Kafka to be ready
echo "Waiting for Kafka to be ready..."
until kafka-topics --bootstrap-server "$KAFKA_BROKER" --list >/dev/null 2>&1; do
  echo "  Kafka not ready yet, retrying in 5s..."
  sleep 5
done
echo "Kafka is ready."

create_topic() {
  local topic=$1
  local partitions=$2
  echo "Creating topic: $topic (partitions=$partitions, replication=$REPLICATION)"
  kafka-topics --bootstrap-server "$KAFKA_BROKER" \
    --create \
    --if-not-exists \
    --topic "$topic" \
    --partitions "$partitions" \
    --replication-factor "$REPLICATION"
}

create_topic "order-events"     $PARTITIONS
create_topic "inventory-events" $PARTITIONS
create_topic "payment-events"   $PARTITIONS
create_topic "shipping-events"  $PARTITIONS
create_topic "dlq-events"       $DLQ_PARTITIONS

echo "========================================="
echo "Topics created. Current topic list:"
echo "========================================="
kafka-topics --bootstrap-server "$KAFKA_BROKER" --list

echo ""
echo "Topic creation complete!"

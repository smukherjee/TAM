#!/bin/bash
set -e
NIFI="http://localhost:8091/nifi-api"

echo "📊 Setting up NiFi Prometheus Monitoring..."

# Check existing
EXISTING=$(curl -s "$NIFI/flow/reporting-tasks" | jq -r '.reportingTasks[] | select(.component.type | contains("PrometheusReportingTask")) | .id')

if [ -n "$EXISTING" ]; then
    echo "✅ Prometheus Reporting Task already exists: $EXISTING"
else
    echo "Creating Prometheus Reporting Task..."
    TASK_ID=$(curl -s -X POST "$NIFI/controller/reporting-tasks" \
        -H "Content-Type: application/json" \
        -d '{
            "revision": { "version": 0 },
            "component": {
                "type": "org.apache.nifi.reporting.prometheus.PrometheusReportingTask",
                "name": "Prometheus Reporting",
                "properties": {
                    "prometheus-reporting-task-metrics-endpoint-port": "9092",
                    "prometheus-reporting-task-instance-id": "tam-nifi"
                }
            }
        }' | jq -r '.id')
    echo "   Task ID: $TASK_ID"
    
    # Start it
    echo "Starting..."
    curl -s -X PUT "$NIFI/reporting-tasks/$TASK_ID/run-status" \
        -H "Content-Type: application/json" \
        -d '{"revision":{"version":1},"state":"RUNNING"}' > /dev/null
    
    echo "✅ Started."
fi

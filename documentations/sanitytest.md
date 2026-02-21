# System Sanity Test Checklist

**Date**: 2025-12-24  
**Purpose**: Manual verification steps to ensure all system components are properly deployed and functional after updates.

## Prerequisites

- [ ] Docker and Docker Compose installed
- [ ] All containers running: `docker-compose -f docker-compose.dev.yml ps` shows the following services as "Up" or "Up (healthy)":
  - `tam-backend-1` (Spring Boot API)
  - `tam-frontend-1` (React dashboard)
  - `tam-timescaledb-1` (PostgreSQL/TimescaleDB)
  - `tam-redis-1` (Cache)
  - `tam-minio-1` (Object storage)
  - `tam-redpanda-1` (Kafka)
  - `tam-ksqldb-server-1` (Stream processing)
  - `tam-ksqldb-cli-1` (ksqlDB CLI)
  - `tam-nifi-1` (Data ingestion)
  - `tam-grafana-1` (Monitoring)
  - `tam-prometheus-1` (Metrics)
  - `tam-superset-1` (BI/Analytics)
  - `tam-loki-1` (Log aggregation)
  - `tam-redpanda-console-1` (Kafka management UI)
- [ ] Network connectivity to localhost ports

## 1. Backend Service (Spring Boot)

**Port**: 8080  
**Purpose**: Core API and business logic

### Health Check

- [ ] Visit <http://localhost:8080/actuator/health>
- [ ] Verify JSON response shows `"status": "UP"`
- [ ] Check all components: db, redis, diskSpace, ssl are "UP"

### API Endpoints

- [ ] Test basic endpoint: `curl http://localhost:8080/api/flights`
- [ ] Verify JSON response contains flight data or empty array
- [ ] Test another endpoint: `curl http://localhost:8080/api/alerts`
- [ ] Verify JSON response contains alert data or empty array

### Java Version Verification

- [ ] Run: `docker exec tam-backend-1 java -version`
- [ ] Confirm OpenJDK 21.x.x is running

### Manual Simulation Testing (if auto-start fails)

- [ ] Run ADS-B simulation: `./simulate_ba249.sh` (simulates flight data)
- [ ] Run Telit simulation: `./simulate_ek500.sh` (simulates vehicle data)
- [ ] Verify data ingestion: Check that data appears in NiFi and flows to Kafka topics

## 2. Frontend (React/TypeScript)

**Port**: 3000  
**Purpose**: Web user interface

### Basic Access

- [ ] Visit <http://localhost:3000> in browser
- [ ] Verify page loads without errors
- [ ] Check browser console for JavaScript errors

### UI Elements

- [ ] Verify main navigation loads
- [ ] Test responsive design on different screen sizes
- [ ] Check for any broken images or links

### API Integration

- [ ] Perform actions that call backend APIs
- [ ] Verify data loads from backend

## 3. Database (TimescaleDB/PostgreSQL)

**Port**: 5432  
**Purpose**: Primary data storage with time-series extensions

### Database Connection Test

- [ ] Run: `docker exec -it tam-timescaledb-1 psql -U postgres -d utam`
- [ ] Verify connection succeeds

### Schema Verification

- [ ] List tables: `\dt`
- [ ] Verify expected tables exist (flights, vehicles, alerts, etc.)
- [ ] Check TimescaleDB extensions: `SELECT * FROM pg_extension WHERE extname = 'timescaledb';`

### Data Integrity

- [ ] Run basic queries on sample data
- [ ] Verify foreign key constraints
- [ ] Check for any corrupted data

## 4. Redis Cache

**Port**: 6379  
**Purpose**: High-performance caching layer

### Redis Connection Test

- [ ] Run: `docker exec -it tam-redis-1 redis-cli ping`
- [ ] Verify "PONG" response

### Basic Operations

- [ ] Set test key: `SET test_key "hello"`
- [ ] Get test key: `GET test_key`
- [ ] Verify value returns correctly

### Persistence

- [ ] Check if data persists across container restarts
- [ ] Verify Redis configuration for persistence

## 5. MinIO Object Storage

**Port**: 9000 (API), 9001 (Console)  
**Purpose**: S3-compatible object storage for archival

### Console Access

- [ ] Visit <http://localhost:9001>
- [ ] Login with default credentials (minio/minio123)
- [ ] Verify buckets list loads

### API Test

- [ ] Run: `curl http://localhost:9000/minio/health/live`
- [ ] Verify "OK" response

### Bucket Operations

- [ ] Create test bucket via console
- [ ] Upload test file
- [ ] Download and verify file integrity
- [ ] Test tenant-isolated bucket creation (tenant-{id}-archive pattern)

## 6. Kafka (Redpanda)

**Port**: 9092  
**Purpose**: Event streaming and messaging

### Kafka Connection Test

- [ ] Run: `docker exec -it tam-redpanda-1 rpk cluster info`
- [ ] Verify cluster is healthy

### REST API Tests

- [ ] Check topics via API: `curl http://localhost:18082/topics`
- [ ] Verify expected topics exist: flight-raw-json, vehicle-raw-json, alerts-json
- [ ] Verify topic count matches expectations (at least 10 topics)
- [ ] **Note**: REST API provides programmatic access alternative to CLI tools

### REST API Message Verification

- [ ] **Note**: Message consumption via REST API may have limited support in current Redpanda version
- [ ] Use CLI tools for detailed message inspection: `docker exec -it tam-redpanda-1 rpk topic consume flight-raw-json --num 5`
- [ ] Verify message format: JSON with icao, lat, lon fields
- [ ] Check tenant headers: Verify X-Tenant-ID headers are present

## 7. ksqlDB

**Port**: 8088  
**Purpose**: Stream processing for real-time analytics

### REST API Test

- [ ] Visit <http://localhost:8088/info>
- [ ] Verify ksqlDB server info returns

### Query Test

- [ ] Run: `docker exec -it tam-ksqldb-cli-1 ksql http://ksqldb-server:8088`
- [ ] Execute: `SHOW STREAMS;`
- [ ] Verify streams are listed

### Stream Processing

- [ ] Test tenant ID extraction from messages
- [ ] Verify domain-prefixed topics are processed correctly

## 8. NiFi

**Port**: 8091 (Web UI), 8092-8094 (API)  
**Purpose**: Data ingestion and routing

### Web UI Access

- [ ] Visit <http://localhost:8091/nifi>
- [ ] Verify NiFi canvas loads
- [ ] Check for any flow errors

### NiFi REST API Tests

- [ ] Check NiFi status: `curl http://localhost:8091/nifi-api/flow/status`
- [ ] Verify flow is running: Check "controllerStatus" shows "running"
- [ ] Check processors: `curl http://localhost:8091/nifi-api/flow/process-groups/root/processors`
- [ ] Verify processors are running: Check "state" is "RUNNING" for active processors
- [ ] Check queue status: `curl http://localhost:8091/nifi-api/flow/status?recursive=true`
- [ ] Verify no queue backlogs: Check "queuedCount" values
- [ ] **Note**: API checks provide programmatic verification alternative to UI inspection

### Data Ingestion Endpoints

- [ ] Test ADS-B data endpoint: `curl -X POST http://localhost:8090/contentListener -H "Content-Type: application/json" -d '{"icao": "ABC123", "lat": 40.7128, "lon": -74.0060}'`
- [ ] Test Telit data endpoint: `curl -X POST http://localhost:8090/contentListener -H "Content-Type: application/json" -d '{"vehicleId": "V001", "lat": 40.7128, "lon": -74.0060}'`
- [ ] Verify data reaches Kafka: Check that messages appear in flight-raw-json and vehicle-raw-json topics

## 9. Grafana

**Port**: 3001  
**Purpose**: Visualization and monitoring dashboards

### Access

- [ ] Visit <http://localhost:3001>
- [ ] Login with admin/admin
- [ ] Verify dashboard loads

### Dashboards

- [ ] Check infrastructure metrics dashboard
- [ ] Verify tenant variable functionality
- [ ] Test cache metrics panels
- [ ] Verify analytics dashboards load without errors (check for "column does not exist" errors)
- [ ] Test Flight Operations dashboard queries
- [ ] Test Turnaround Performance dashboard queries

### Data Sources

- [ ] Verify Prometheus data source connection
- [ ] Check Loki logs integration
- [ ] Test dashboard refresh and updates

## 10. Prometheus

**Port**: 9090  
**Purpose**: Metrics collection and alerting

### Web UI

- [ ] Visit <http://localhost:9090>
- [ ] Verify targets page shows all services up
- [ ] Check metrics collection

### Prometheus Query Test

- [ ] Query: `up{job="backend"}`
- [ ] Verify backend metrics are collected
- [ ] Test custom metrics (cache hits/misses, tenant counters)

### Alerting

- [ ] Check alert rules configuration
- [ ] Verify alert manager integration

## 11. Superset

**Port**: 8089  
**Purpose**: Business intelligence and data exploration

### Superset Access

- [ ] Visit <http://localhost:8089>
- [ ] Login with admin/admin
- [ ] Verify dashboard loads

### Data Connections

- [ ] Check TimescaleDB connection
- [ ] Verify datasets are accessible
- [ ] Test chart creation

### Superset Dashboards

- [ ] Review existing dashboards
- [ ] Test data filtering and drill-down

## 12. Loki

**Port**: 3100  
**Purpose**: Log aggregation

### Loki API Test

- [ ] Run: `curl http://localhost:3100/ready`
- [ ] Verify "ready" response

### Log Queries

- [ ] Test log queries in Grafana Explore
- [ ] Verify logs from all services are ingested
- [ ] Check log retention and querying performance

## 13. Redpanda Console

**Port**: 8090  
**Purpose**: Kafka cluster management UI

### Redpanda Console Access

- [ ] Visit <http://localhost:8090>
- [ ] Verify topics and messages display
- [ ] Check consumer group status

### Monitoring

- [ ] Review topic throughput metrics
- [ ] Check for message lag
- [ ] Verify schema registry integration

## Integration Tests

### End-to-End Data Flow

- [ ] Start simulation: Verify backend auto-starts simulation or run manual scripts
- [ ] Monitor NiFi ingestion: Check that data is received on NiFi endpoints
- [ ] Verify Kafka publishing: Confirm messages appear in flight-raw-json and vehicle-raw-json topics
- [ ] Check ksqlDB processing: Verify streams process the incoming data
- [ ] Validate backend consumption: Ensure data is stored in TimescaleDB
- [ ] Confirm frontend display: Check that flight/vehicle data appears in dashboard

### Tenant Isolation

- [ ] Test multi-tenant data separation
- [ ] Verify tenant-specific buckets in MinIO
- [ ] Check tenant headers in Kafka messages

### Archival System

- [ ] Trigger domain event archiving
- [ ] Verify Parquet files created in tenant buckets
- [ ] Check lifecycle policies applied

## Performance Verification

### Load Testing

- [ ] Run simulated data ingestion
- [ ] Monitor system metrics in Grafana
- [ ] Verify no performance degradation

### Resource Usage

- [ ] Check container resource usage
- [ ] Monitor memory and CPU consumption
- [ ] Verify auto-scaling if configured

## Final Checklist

- [ ] All services healthy and accessible
- [ ] Data flows end-to-end without errors
- [ ] Monitoring and logging functional
- [ ] Security measures in place (tenant isolation, encryption)
- [ ] Performance meets requirements
- [ ] No critical errors in logs

## Troubleshooting

If any check fails:

1. Check container logs: `docker-compose -f docker-compose.dev.yml logs [service]`
2. Verify network connectivity between containers
3. Check configuration files for syntax errors
4. Restart failed services: `docker-compose -f docker-compose.dev.yml restart [service]`
5. Review docker-compose.dev.yml for port conflicts or missing dependencies

# TAM Platform Architecture - Component Guide & Migration Path

> **Version**: 1.0  
> **Date**: 2025-12-20  
> **Purpose**: This document details each component in the Dev and Prod architecture stacks, explains their role, and provides the migration path from Dev to Production across different environments (On-Premise, Azure, AWS).

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Component Mapping: Dev vs Prod](#component-mapping-dev-vs-prod)
3. [Detailed Component Guide](#detailed-component-guide)
4. [Migration Path](#migration-path)
5. [Environment-Specific Configurations](#environment-specific-configurations)
6. [Quick Reference](#quick-reference)

---

## Architecture Overview

The TAM Platform follows a **Portable Architecture** pattern where:
- The **Core Logic** (NiFi flows, ksqlDB queries, Spring Boot services) remains identical across environments
- The **Infrastructure Components** (Message Queue, Storage, BI Tools) are swapped based on deployment target

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         PORTABLE CORE                                   │
│  Apache NiFi → Kafka Protocol → ksqlDB → Spring Boot → SQL Standard    │
└─────────────────────────────────────────────────────────────────────────┘
        │                  │                  │                  │
        ▼                  ▼                  ▼                  ▼
   ┌─────────┐      ┌─────────────┐     ┌──────────┐      ┌─────────┐
   │  DEV    │      │  ON-PREM    │     │  AZURE   │      │   AWS   │
   │Redpanda │      │  Strimzi    │     │Event Hubs│      │   MSK   │
   │ MinIO   │      │  MinIO      │     │  ADLS    │      │   S3    │
   │Superset │      │ StarRocks   │     │ Synapse  │      │Redshift │
   └─────────┘      └─────────────┘     └──────────┘      └─────────┘
```

---

## Component Mapping: Dev vs Prod

| Layer | Dev (Docker) | Prod On-Prem | Prod Azure | Prod AWS |
|-------|--------------|--------------|------------|----------|
| **Message Queue** | Redpanda | Strimzi Kafka | Azure Event Hubs | Amazon MSK |
| **Schema Registry** | Redpanda (built-in) | Confluent/Apicurio | Azure Schema Registry | AWS Glue Schema Registry |
| **Stream Processing** | ksqlDB (single) | ksqlDB Cluster | ksqlDB on AKS | ksqlDB on EKS |
| **Object Storage** | MinIO | MinIO Cluster | Azure Data Lake Gen2 | Amazon S3 |
| **Data Warehouse** | TimescaleDB | StarRocks | Azure Synapse | Amazon Redshift |
| **BI Tool** | Apache Superset | Apache Superset | PowerBI Embedded | Amazon QuickSight |
| **Metrics** | Prometheus | Prometheus | Azure Monitor | CloudWatch |
| **Logs** | Loki | Loki | Azure Log Analytics | CloudWatch Logs |
| **Traces** | (none in dev) | Jaeger/Tempo | Application Insights | AWS X-Ray |
| **Identity** | (none in dev) | Keycloak | Azure AD | AWS Cognito |

---

## Detailed Component Guide

### 1. Ingestion Layer

#### Apache NiFi

| Aspect | Dev | Production |
|--------|-----|------------|
| **Deployment** | Single node Docker | 3-node HA cluster on K8s |
| **Image** | `apache/nifi:1.25.0` | Same |
| **Memory** | 2GB | 8GB per node |
| **Storage** | Docker volumes | Persistent Volume Claims |
| **Registry** | None | NiFi Registry for version control |

**Purpose**: Visual data flow management. Handles all protocol adapters (HTTP, SFTP, MQTT).

**Why NiFi**: Team skill match (Java, visual). Excellent for batch file processing (AODB, CDM).

**Dev Config**:
```yaml
nifi:
  image: apache/nifi:1.25.0
  ports:
    - "8091:8080"
  environment:
    - NIFI_WEB_HTTP_PORT=8080
    - SINGLE_USER_CREDENTIALS_USERNAME=admin
    - SINGLE_USER_CREDENTIALS_PASSWORD=admin123
```

---

### 2. Message Queue Layer

#### Redpanda (Dev Only)

| Aspect | Value |
|--------|-------|
| **Purpose** | Lightweight Kafka-compatible broker for local development |
| **Image** | `docker.redpanda.com/redpandadata/redpanda:latest` |
| **Memory** | 512MB - 1GB |
| **Ports** | 9092 (Kafka), 8081 (Schema Registry), 9644 (Admin) |

**Why Redpanda for Dev**: 
- Single binary (no Zookeeper)
- Starts in <5 seconds
- 100% Kafka API compatible
- Built-in Schema Registry

**Dev Config**:
```yaml
redpanda:
  image: docker.redpanda.com/redpandadata/redpanda:latest
  command:
    - redpanda start
    - --smp 1
    - --memory 512M
    - --mode dev-container
    - --kafka-addr PLAINTEXT://0.0.0.0:9092
    - --advertise-kafka-addr PLAINTEXT://redpanda:9092
  ports:
    - "9092:9092"
    - "8081:8081"
    - "9644:9644"
```

#### Strimzi Kafka (Prod On-Prem)

| Aspect | Value |
|--------|-------|
| **Purpose** | Production Kafka on Kubernetes |
| **Deployment** | Strimzi Operator |
| **Replicas** | 6 brokers minimum |
| **Replication Factor** | 3 |

**Why Strimzi**: Kubernetes-native operator. Handles rolling upgrades, TLS, and RBAC.

#### Azure Event Hubs (Prod Azure)

| Aspect | Value |
|--------|-------|
| **Purpose** | Managed Kafka-compatible service |
| **Tier** | Standard or Premium |
| **Partitions** | Configure per topic (max 32 for Standard) |

**Connection String**:
```
Endpoint=sb://<namespace>.servicebus.windows.net/;SharedAccessKeyName=<key>
```

#### Amazon MSK (Prod AWS)

| Aspect | Value |
|--------|-------|
| **Purpose** | Managed Apache Kafka |
| **Broker Type** | kafka.m5.large (minimum for prod) |
| **Storage** | 1TB per broker |

---

### 3. Stream Processing Layer

#### ksqlDB

| Aspect | Dev | Production |
|--------|-----|------------|
| **Deployment** | Single node | 3+ node cluster |
| **Image** | `confluentinc/ksqldb-server:0.29.0` | Same |
| **Memory** | 1GB | 8GB per node |

**Purpose**: Streaming SQL for CEP (Complex Event Processing). Handles alerts like "Vehicle speed > 25 km/h".

**Why ksqlDB over Flink**: 
- SQL-based (matches PowerBI analyst skills)
- Lower operational complexity
- Sufficient for current use cases

**Example Query**:
```sql
CREATE TABLE vehicle_alerts AS
SELECT 
    vehicle_id,
    MAX(speed) as max_speed,
    COUNT(*) as violation_count
FROM vehicle_stream
WHERE speed > 25
WINDOW TUMBLING (SIZE 1 MINUTE)
GROUP BY vehicle_id
EMIT CHANGES;
```

#### Apache Flink (Future/Optional)

| Aspect | Value |
|--------|-------|
| **When to Use** | Complex stream joins, exactly-once processing |
| **Deployment** | Flink Kubernetes Operator |
| **Skill Required** | Java/Scala developers |

**Migration Trigger**: Move to Flink when ksqlDB cannot handle:
- Joining 3+ streams
- Complex windowing with late arrivals
- Exactly-once semantics for financial data

---

### 4. Business Services Layer

#### Spring Boot Microservices

| Service | Purpose | Primary Dependencies |
|---------|---------|---------------------|
| **Live Service** | Real-time positions via WebSocket/gRPC | Kafka, TimescaleDB, Redis |
| **Alert Service** | Alert generation and management | ksqlDB, TimescaleDB |
| **Turnaround Service** | CDM milestone tracking | TimescaleDB |
| **Report Service** | Batch report generation | Object Storage, Data Warehouse |

**Configuration Portability** (Spring Boot `application.yml`):
```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/utam}
  
minio:
  endpoint: ${S3_ENDPOINT:http://localhost:9000}
  access-key: ${S3_ACCESS_KEY:minioadmin}
  secret-key: ${S3_SECRET_KEY:minioadmin}
```

---

### 5. Data Storage Layer

#### TimescaleDB (Operational)

| Aspect | Dev | Production |
|--------|-----|------------|
| **Image** | `timescale/timescaledb:latest-pg16` | Same |
| **Replicas** | 1 | 2 (primary + standby) |
| **Retention** | 7 days (hypertable) | 90 days |

**Purpose**: Time-series data for real-time queries. Powers the Live Dashboard.

#### Object Storage (Data Lake)

| Environment | Technology | S3 API Endpoint |
|-------------|------------|-----------------|
| Dev | MinIO | `http://minio:9000` |
| On-Prem | MinIO Cluster | `http://minio.internal:9000` |
| Azure | ADLS Gen2 | `https://<account>.dfs.core.windows.net` |
| AWS | S3 | `https://s3.<region>.amazonaws.com` |

**Purpose**: Long-term storage for Parquet files. Source for Data Warehouse.

#### Data Warehouse

| Environment | Technology | Query Interface |
|-------------|------------|-----------------|
| Dev | (use TimescaleDB) | PostgreSQL |
| On-Prem | StarRocks | MySQL Protocol |
| Azure | Synapse Analytics | T-SQL |
| AWS | Redshift Serverless | PostgreSQL |

**Purpose**: OLAP queries for BI dashboards. Powers Superset/PowerBI/QuickSight.

---

### 6. Presentation Layer

#### BI Tools

| Environment | Tool | Connection |
|-------------|------|------------|
| Dev / On-Prem | Apache Superset | JDBC to StarRocks/TimescaleDB |
| Azure | PowerBI Embedded | DirectQuery to Synapse |
| AWS | Amazon QuickSight | JDBC to Redshift |

**Portability Strategy**: Create standard SQL views in the Data Warehouse. BI tools consume views, not raw tables.

---

### 7. Observability Layer

#### OpenTelemetry (The Abstraction)

All services emit telemetry via OpenTelemetry. The OTel Collector routes to environment-specific backends.

**Spring Boot Agent**:
```dockerfile
ENV JAVA_TOOL_OPTIONS="-javaagent:/otel/opentelemetry-javaagent.jar"
ENV OTEL_SERVICE_NAME=live-service
ENV OTEL_EXPORTER_OTLP_ENDPOINT=${OTEL_ENDPOINT:http://otel-collector:4317}
```

| Signal | Dev/On-Prem | Azure | AWS |
|--------|-------------|-------|-----|
| Metrics | Prometheus | Azure Monitor | CloudWatch |
| Logs | Loki | Log Analytics | CloudWatch Logs |
| Traces | Jaeger/Tempo | App Insights | X-Ray |

---

## Migration Path

### Phase 1: Local Development

**Goal**: Developers can run the full stack on their laptop.

**Stack**:
```
Docker Compose:
  - Redpanda (1 container)
  - NiFi (1 container)
  - ksqlDB (1 container)
  - TimescaleDB (1 container)
  - MinIO (1 container)
  - Spring Boot Apps (2-4 containers)
  - Superset (1 container)
  - Prometheus + Grafana (2 containers)
```

**Total**: ~12 containers, ~8GB RAM

---

### Phase 2: Staging (Pre-Prod)

**Goal**: Validate on Kubernetes with production-like components.

**Changes from Dev**:
| Component | Dev → Staging |
|-----------|---------------|
| Message Queue | Redpanda → Strimzi Kafka (3 brokers) |
| Storage | Docker volumes → Persistent Volumes |
| Secrets | Environment variables → HashiCorp Vault |
| Ingress | Direct ports → Traefik/Nginx Ingress |

**Deployment**:
```bash
# Install via Helm
helm upgrade --install tam ./charts/tam-platform \
  -f values-staging.yaml \
  --namespace tam-staging
```

---

### Phase 3: Production

**Goal**: Full HA, security, and compliance.

#### Production Checklist

| Category | Requirement |
|----------|-------------|
| **HA** | All stateful services have 2+ replicas |
| **Backup** | Daily backups for TimescaleDB, MinIO |
| **TLS** | All internal traffic encrypted |
| **Auth** | Keycloak integrated with client LDAP |
| **Audit** | All data access logged |
| **DR** | Multi-AZ deployment, documented RTO/RPO |

---

### Migration Commands

#### Dev to Staging
```bash
# Build and push images
docker-compose build
docker tag tam-backend:latest registry.company.com/tam-backend:v1.0.0
docker push registry.company.com/tam-backend:v1.0.0

# Deploy to K8s
kubectl apply -f k8s/staging/
```

#### Staging to Production (On-Prem)
```bash
helm upgrade --install tam ./charts/tam-platform \
  -f values-onprem-prod.yaml \
  --namespace tam-prod \
  --set kafka.bootstrapServers=strimzi-kafka-bootstrap:9092 \
  --set storage.endpoint=http://minio.storage:9000
```

#### Staging to Production (Azure)
```bash
helm upgrade --install tam ./charts/tam-platform \
  -f values-azure-prod.yaml \
  --namespace tam-prod \
  --set kafka.bootstrapServers=<namespace>.servicebus.windows.net:9093 \
  --set storage.endpoint=https://<account>.dfs.core.windows.net \
  --set observability.backend=azure-monitor
```

#### Staging to Production (AWS)
```bash
helm upgrade --install tam ./charts/tam-platform \
  -f values-aws-prod.yaml \
  --namespace tam-prod \
  --set kafka.bootstrapServers=<msk-bootstrap>:9092 \
  --set storage.endpoint=https://s3.ap-south-1.amazonaws.com \
  --set observability.backend=cloudwatch
```

---

## Environment-Specific Configurations

### values-dev.yaml (Docker Compose)
```yaml
kafka:
  type: redpanda
  bootstrapServers: redpanda:9092

storage:
  type: minio
  endpoint: http://minio:9000
  bucket: tam-data

database:
  host: timescaledb
  port: 5432

observability:
  metrics: prometheus
  logs: loki
```

### values-onprem-prod.yaml
```yaml
kafka:
  type: strimzi
  bootstrapServers: strimzi-kafka-bootstrap:9092
  replicationFactor: 3

storage:
  type: minio
  endpoint: http://minio.storage.svc:9000
  bucket: tam-data

warehouse:
  type: starrocks
  host: starrocks-fe.analytics.svc
  port: 9030

bi:
  type: superset

observability:
  metrics: prometheus
  logs: loki
  traces: jaeger
```

### values-azure-prod.yaml
```yaml
kafka:
  type: eventhubs
  bootstrapServers: <namespace>.servicebus.windows.net:9093
  connectionString: ${EVENTHUBS_CONNECTION_STRING}

storage:
  type: adls
  endpoint: https://<account>.dfs.core.windows.net
  container: tam-data

warehouse:
  type: synapse
  connectionString: ${SYNAPSE_CONNECTION_STRING}

bi:
  type: powerbi
  workspaceId: ${POWERBI_WORKSPACE_ID}

observability:
  metrics: azure-monitor
  logs: log-analytics
  traces: application-insights
```

### values-aws-prod.yaml
```yaml
kafka:
  type: msk
  bootstrapServers: <msk-bootstrap>:9092

storage:
  type: s3
  endpoint: https://s3.ap-south-1.amazonaws.com
  bucket: tam-data-prod

warehouse:
  type: redshift
  cluster: tam-redshift-cluster
  database: tam

bi:
  type: quicksight  # or superset

observability:
  metrics: cloudwatch
  logs: cloudwatch-logs
  traces: xray
```

---

## Quick Reference

### Docker Compose Commands (Dev)
```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f kafka

# Stop all
docker-compose down

# Reset (delete data)
docker-compose down -v
```

### Helm Commands (Staging/Prod)
```bash
# Install
helm install tam ./charts/tam-platform -f values-<env>.yaml

# Upgrade
helm upgrade tam ./charts/tam-platform -f values-<env>.yaml

# Rollback
helm rollback tam 1

# Uninstall
helm uninstall tam
```

### Useful Endpoints

| Service | Dev URL | Purpose |
|---------|---------|---------|
| NiFi | http://localhost:8091 | Data flow management |
| Redpanda Console | http://localhost:9644 | Kafka topics |
| ksqlDB | http://localhost:8088 | Stream processing |
| Superset | http://localhost:8088 | BI dashboards |
| Grafana | http://localhost:3001 | Operational metrics |
| MinIO Console | http://localhost:9001 | Object storage |
| Spring Boot API | http://localhost:8080 | Application API |
| React Dashboard | http://localhost:3000 | Frontend |

---

## Appendix: Diagram Files

| Diagram | File | Description |
|---------|------|-------------|
| Dev Architecture | [architecture_dev_20251220.mermaid](./architecture_dev_20251220.mermaid) | Lightweight Docker stack |
| Prod Architecture | [architecture_prod_20251220.mermaid](./architecture_prod_20251220.mermaid) | Portable production stack |

---

*Document maintained by TAM Platform Team*

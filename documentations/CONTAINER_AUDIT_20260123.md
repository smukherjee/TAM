# TAM Platform - Container & Service Audit Report
**Date:** January 23, 2026  
**Scope:** All Docker services, Kafka topics, and integrations

---

## 🔍 Audit Summary

### ❌ REMOVED (Unused Services)

#### 1. **ksqlDB** - Stream Processing Engine
- **Status:** ✅ REMOVED
- **Reason:** Not used anywhere in the application
- **Containers Removed:**
  - `tam-ksqldb-server-1` (port 8088)
  - `tam-ksqldb-cli-1`
- **Files Cleaned:**
  - Removed from `docker-compose.dev.yml`
  - Removed from `Makefile` (help text, commands)
  - Removed from `infrastructure/prometheus/prometheus.yml`
- **Evidence:**
  - Zero Java code references to ksqlDB
  - Only default `KSQL_PROCESSING_LOG` stream existed
  - Init scripts in `infrastructure/ksqldb/` never executed
- **Impact:** None - all stream processing handled by Spring Boot Kafka consumers

---

## ✅ ACTIVE & IN USE (Keep)

### Core Services

#### 1. **Redpanda (Kafka)** - Message Broker
- **Status:** ✅ ACTIVE, ESSENTIAL
- **Containers:**
  - `tam-redpanda-1` (port 9092)
  - `tam-redpanda-console-1` (port 8090)
- **Usage:**
  - 7 active Kafka consumers in backend
  - Topics: `flight-raw-json`, `vehicle-raw-json`, `turnaround-raw-json`, etc.
- **Code References:** `@KafkaListener` in 9 Java files
- **Health:** ✅ Healthy

#### 2. **NiFi** - Data Ingestion Layer
- **Status:** ✅ ACTIVE, ESSENTIAL
- **Container:** `tam-nifi-1` (port 8091, 8092-8094)
- **Usage:**
  - HTTP ingestion on ports 8092-8094
  - Processes flight/vehicle/CV event data
  - Publishes to Kafka topics
- **Code References:** MockAdsbGenerators send to `http://nifi:8092/adsb-ingest`
- **Health:** ✅ Healthy, ListenHTTP working

#### 3. **TimescaleDB** - Primary Database
- **Status:** ✅ ACTIVE, ESSENTIAL
- **Container:** `tam-timescaledb-1` (port 5432)
- **Usage:**
  - Stores flights (12,393 records), vehicles, turnarounds
  - JPA repositories, Spring Data
  - Init scripts auto-run from `infrastructure/db/init/`
- **Code References:** All `@Entity` models, `@Repository` interfaces
- **Health:** ✅ Healthy

#### 4. **Redis** - Cache Layer
- **Status:** ✅ ACTIVE, IN USE
- **Container:** `tam-redis-1` (port 6379)
- **Usage:**
  - Flight caching (300s TTL)
  - Active vehicle sets
  - Session management
- **Code References:**
  - `RedisService.java` - Used by `FlightConsumer`, `VehicleService`
  - `RedisCacheService.java` in platform-core
- **Health:** ✅ Healthy

#### 5. **MinIO** - Object Storage
- **Status:** ✅ ACTIVE, IN USE
- **Containers:**
  - `tam-minio-1` (port 9000, 9001)
  - `tam-minio-init` (one-time bucket creation)
- **Usage:**
  - Archives raw flight/vehicle JSON
  - Stores reports
  - Buckets: `tam-raw-data`, `tam-processed-data`, `tam-reports`
- **Code References:**
  - `MinioService.java` - Used by `VehicleService`, `FlightConsumer`
  - Upload path: `archives/raw/{ICAO}/{YYYY}/{MM}/{DD}/{HH}/`
- **Health:** ⚠️ Down (Prometheus reports "down" but container running)

---

### Observability Stack

#### 6. **Prometheus** - Metrics Collection
- **Status:** ✅ ACTIVE, IN USE
- **Container:** `tam-prometheus-1` (port 9090)
- **Usage:**
  - Scrapes `/actuator/prometheus` from backend
  - Monitors NiFi, Redpanda
  - Custom metrics via Micrometer
- **Code References:**
  - `MeterRegistry` injected in 7+ classes
  - Custom counters: `simulator.events.generated`, `tenant.requests`, `cache.hits/misses`
- **Active Targets:**
  - ✅ `prometheus` - Up
  - ✅ `redpanda` - Up
  - ✅ `nifi` - Up
  - ⚠️ `minio` - Down (scrape failing)
- **Health:** ✅ Healthy

#### 7. **Grafana** - Metrics Visualization
- **Status:** ✅ ACTIVE, CONFIGURED
- **Container:** `tam-grafana-1` (port 3001)
- **Usage:**
  - Dashboards for system monitoring
  - Data sources: Prometheus, Loki, TimescaleDB
  - Pre-configured via `infrastructure/grafana/provisioning/`
- **Dashboards:** Located in `infrastructure/grafana/dashboards/`
- **Health:** ✅ Healthy
- **Access:** http://localhost:3001 (admin/admin)

#### 8. **Loki** - Log Aggregation
- **Status:** ⚠️ RUNNING, MINIMAL USE
- **Container:** `tam-loki-1` (port 3100)
- **Usage:**
  - Configured as Grafana data source
  - No explicit log shipping from backend
  - Relies on Docker log driver (not configured)
- **Code References:** None in backend
- **Recommendation:** 
  - ✅ Keep - Useful for debugging via Grafana
  - ⚠️ Configure log shipping for full value

---

### Analytics & BI

#### 9. **Superset** - Business Intelligence
- **Status:** ⚠️ RUNNING, NOT INTEGRATED
- **Container:** `tam-superset-1` (port 8089)
- **Usage:**
  - Initialized with admin user
  - Connected to TimescaleDB
  - No dashboards created
- **Code References:** None
- **Recommendation:**
  - ⏸️ Optional - Can disable to save resources
  - ✅ Keep if planning BI dashboards
- **Access:** http://localhost:8089 (admin/admin)

---

### Application Services

#### 10. **Backend** - Spring Boot API
- **Status:** ✅ ACTIVE, ESSENTIAL
- **Container:** `tam-backend-1` (port 8080)
- **Language:** Java 21, Spring Boot 3.x
- **Features:**
  - REST API endpoints
  - Kafka consumers (7 active)
  - WebSocket (STOMP)
  - Mock data generators
- **Health:** ✅ Healthy

#### 11. **Frontend** - React Dashboard
- **Status:** ✅ ACTIVE, ESSENTIAL
- **Container:** `tam-frontend-1` (port 3000)
- **Stack:** React, TypeScript, Vite, Tailwind CSS
- **Features:**
  - Map visualization (Leaflet)
  - WebSocket real-time updates
  - Dark theme UI
- **Health:** ✅ Running

---

## 📊 Kafka Topics Audit

### Active Topics (In Use)

| Topic | Producer | Consumer | Purpose |
|-------|----------|----------|---------|
| `flight-raw-json` | NiFi (port 8092) | `FlightConsumer.java` | Flight telemetry |
| `vehicle-raw-json` | NiFi (port 8093) | `VehicleService.java`, `VehicleAlertService.java` | Vehicle tracking |
| `turnaround-raw-json` | NiFi (port 8094) | `CVEventConsumer.java`, `TurnaroundService.java`, `TurnaroundRawConsumer.java` | Turnaround events |
| `turnaround-events` | Backend | `TurnaroundEventConsumer.java` | Processed turnaround data |

### No Unused Topics Found
All topics in Redpanda are actively consumed by Spring Boot services.

---

## 🗄️ Volume Usage

### Persistent Volumes

| Volume | Service | Size | Purpose |
|--------|---------|------|---------|
| `tam_timescaledb_data` | TimescaleDB | ~500MB | Flight/vehicle/turnaround data |
| `tam_nifi_data` | NiFi | ~200MB | Flow files, state |
| `tam_nifi_conf` | NiFi | ~50MB | Configuration |
| `tam_nifi_logs` | NiFi | ~100MB | Logs |
| `tam_nifi_provenance` | NiFi | ~300MB | Provenance data |
| `tam_redpanda_data` | Redpanda | ~100MB | Kafka topics, offsets |
| `tam_minio_data` | MinIO | ~50MB | Archived JSON |
| `tam_redis_data` | Redis | ~10MB | Cached data |
| `tam_prometheus_data` | Prometheus | ~100MB | Metrics TSDB |
| `tam_grafana_data` | Grafana | ~50MB | Dashboards, users |
| `tam_loki_data` | Loki | ~50MB | Logs |
| `tam_superset_data` | Superset | ~100MB | Metadata DB |

**Total:** ~1.6GB (estimated)

---

## 🎯 Recommendations

### 1. **Keep as Essential:**
- ✅ Redpanda (Kafka)
- ✅ NiFi
- ✅ TimescaleDB
- ✅ Redis
- ✅ MinIO
- ✅ Backend
- ✅ Frontend

### 2. **Keep for Observability:**
- ✅ Prometheus - Actively used
- ✅ Grafana - Useful for monitoring
- ⚠️ Loki - Useful but needs log shipping config

### 3. **Optional (Can Disable):**
- ⏸️ **Superset** - Not integrated, can disable with:
  ```yaml
  profiles: ["analytics"]  # Add to superset service
  ```
  Then start only when needed: `docker-compose --profile analytics up superset`

### 4. **Already Removed:**
- ❌ ksqlDB - No usage, removed

### 5. **Fix Required:**
- ⚠️ MinIO Prometheus scraping - Check metrics endpoint or disable in prometheus.yml

---

## 🧹 Files to Clean (Optional)

### Unused Configuration
- `infrastructure/ksqldb/ksql-init.sql` - Can delete
- `infrastructure/ksqldb/` directory - Can delete

### Keep for Reference
- `infrastructure/superset/` - Keep if planning to use BI
- `infrastructure/loki/` - Keep for log aggregation
- `infrastructure/grafana/` - Keep, actively used

---

## 📝 Architecture Summary

### Current Data Flow
```
MockAdsbGenerators (Backend)
    ↓ HTTP POST
NiFi ListenHTTP (8092-8094)
    ↓ Process JSON
Kafka Topics (Redpanda)
    ↓ Consume
Spring Boot Consumers
    ↓ Split
    ├─→ TimescaleDB (Persist)
    ├─→ Redis (Cache)
    ├─→ MinIO (Archive)
    └─→ WebSocket (Broadcast)
         ↓
Frontend (Real-time updates)
```

### Observability Flow
```
Backend Spring Boot
    ↓ Micrometer Metrics
Prometheus (Scrape /actuator/prometheus)
    ↓ Query
Grafana (Visualize)

Backend Logs → (Future: Loki) → Grafana
```

---

## 🔄 Container Resource Impact

### Before Cleanup:
- **14 containers** running
- **~2.5GB RAM** usage (estimated)

### After ksqlDB Removal:
- **12 containers** running  
- **~2.2GB RAM** usage (estimated)
- **Savings:** ~300MB RAM, 2 containers

---

## ✅ Audit Complete

**Removed:** ksqlDB (unused)  
**Kept:** All essential and actively used services  
**Optional:** Superset (can disable if not using BI)  
**Healthy:** 11/12 containers (MinIO scraping issue minor)

All changes have been applied to:
- ✅ `docker-compose.dev.yml`
- ✅ `Makefile`
- ✅ `infrastructure/prometheus/prometheus.yml`
- ✅ ksqlDB containers stopped and removed

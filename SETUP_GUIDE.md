# TAM Platform Setup Guide

This guide provides detailed instructions for setting up and running the TAM Platform, including all necessary configuration scripts.

## Prerequisites

Ensure you have the following installed:
- **Docker Desktop** (or Docker Engine + Docker Compose)
- **Java 21** (for local backend development)
- **Node.js 18+** (for local frontend development)
- **Make** (optional, for using the Makefile)
- **jq** (required for NiFi setup scripts)
- **curl** (required for health checks and scripts)

## Quick Start (Docker)

The easiest way to run the platform is using the provided Makefile and Docker Compose.

1.  **Start Infrastructure & Services**
    ```bash
    make dev-up
    ```
    *This starts Redpanda (Kafka), NiFi, TimescaleDB, MinIO, Redis, Backend, and Frontend.*

2.  **Wait for NiFi**
    NiFi takes about 60-90 seconds to fully start. You can check its status with:
    ```bash
    docker-compose logs -f nifi
    ```
    Wait until you see the message: `NiFi has started. The UI is available at...`

3.  **Configure NiFi Flows (Crucial Step)**
    Once NiFi is up, you **MUST** run the setup script to create the ingestion pipelines. Without this, no data will flow.
    ```bash
    make setup-nifi
    ```
    *If you don't have Make, run:* `bash infrastructure/nifi/setup-nifi.sh`

4.  **Verify Data Flow**
    - **Frontend**: [http://localhost:3000](http://localhost:3000) - Should show moving aircraft/vehicles.
    - **NiFi UI**: [http://localhost:8091/nifi](http://localhost:8091/nifi) - Should show 3 process groups (ADSB, Vehicle, CV Event).
    - **Redpanda Console**: [http://localhost:8090](http://localhost:8090) - Should show topics like `flight-raw-json` receiving data.

## Detailed Setup Steps

### 1. Infrastructure

The `docker-compose.dev.yml` file defines the entire infrastructure.

- **Start**: `docker-compose -f docker-compose.dev.yml up -d`
- **Stop**: `docker-compose -f docker-compose.dev.yml down`
- **Logs**: `docker-compose -f docker-compose.dev.yml logs -f`

### 2. Database (TimescaleDB)

The database is automatically initialized using scripts in `infrastructure/db/init/`.
- `01-init-schema.sql`: Creates tables and hypertables.
- `02-multi-tenant.sql`: Sets up tenant data.

If you need to reset the DB:
```bash
make reset
make dev-up
```

### 3. NiFi (Ingestion)

NiFi requires a one-time setup after the container is created.
- **Script**: `infrastructure/nifi/setup-nifi.sh`
- **Function**: Creates Process Groups, ListenHTTP processors, and PublishKafka processors.
- **Troubleshooting**: If NiFi is unhealthy, restart it: `docker-compose restart nifi`.

### 4. Backend (Spring Boot)

- **Location**: `backend/`
- **Configuration**: `backend/src/main/resources/application.yml`
- **Simulation**: The backend contains `MockAdsbGenerator` and `MockTelitGenerator` which send data to NiFi.
- **Build**: `mvn clean package`

### 5. Frontend (React)

- **Location**: `frontend/`
- **Configuration**: `frontend/src/services/api.ts` (points to Backend API)
- **Build**: `npm run build`

## Troubleshooting Common Issues

### "NiFi Connection Refused"
- **Cause**: NiFi container is not yet ready or is unhealthy.
- **Fix**: Check logs `docker-compose logs nifi`. If it's stuck, `docker-compose restart nifi`. Wait for it to be healthy before running `make setup-nifi`.

### "Empty Map / No Data"
- **Cause**: NiFi flows are not created or Backend is not consuming Kafka.
- **Fix**:
    1. Check NiFi UI (http://localhost:8091/nifi). If empty, run `make setup-nifi`.
    2. Check Backend logs `docker-compose logs backend`. Look for "Generated flight data" and "Consumed flight event".

### "500 Internal Server Error"
- **Cause**: Database schema mismatch or connection issue.
- **Fix**: Check Backend logs for JDBC errors. Ensure `infrastructure/db/init` scripts match the JPA entities.

## Useful Commands

| Command | Description |
|---------|-------------|
| `make dev-up` | Start all services |
| `make setup-nifi` | Configure NiFi flows (Run after NiFi is up) |
| `make dev-logs` | Tail all logs |
| `make dev-down` | Stop all services |
| `make reset` | **Destructive**: Stop services and delete all data volumes |

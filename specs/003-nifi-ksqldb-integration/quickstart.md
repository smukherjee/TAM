# Quickstart: Platform Phase 1

**Feature**: `003-nifi-ksqldb-integration`

## Prerequisites
- Docker & Docker Compose
- Java 17+
- Node.js 18+

## Setup

1. **Start Infrastructure**:
   ```bash
   docker-compose -f docker-compose.dev.yml up -d
   ```
   *Verifies: Kafka, Zookeeper, Postgres, Redis, MinIO, NiFi.*

2. **Apply DB Fixes**:
   ```bash
   docker exec -i tam-postgres psql -U postgres -d tam < infrastructure/db/init/03-fix-schema-mismatches.sql
   ```

3. **Start Backend**:
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```

4. **Start Frontend**:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

## Verification

1. **Check Redis**:
   ```bash
   docker exec -it tam-redis redis-cli keys "flight:*"
   ```
   Should show keys after Mock Generator starts.

2. **Check WebSockets**:
   Open Browser DevTools -> Network -> WS.
   Verify connection to `ws://localhost:8080/ws`.
   Verify subscription to `/topic/flights/VIDP`.

3. **Check MinIO**:
   Open `http://localhost:9001` (admin/password).
   Check `tam-data` bucket.

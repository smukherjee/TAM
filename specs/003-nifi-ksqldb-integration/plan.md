# Implementation Plan: Platform Phase 1 (Infrastructure & Real-time)

**Branch**: `003-nifi-ksqldb-integration` | **Date**: 2025-12-21 | **Spec**: [specs/003-nifi-ksqldb-integration/spec.md](specs/003-nifi-ksqldb-integration/spec.md)
**Input**: Feature specification from `specs/003-nifi-ksqldb-integration/spec.md`

## Summary

Transition the TAM project from MVP to Platform Phase 1 by introducing enterprise-grade infrastructure components (Redis, MinIO) and real-time capabilities (WebSockets). This phase also enforces strict multi-tenancy across the database layer and fixes schema inconsistencies found during the MVP review.

## Technical Context

**Language/Version**: Java 17 (Spring Boot 3.x), TypeScript 5.x (React 18)
**Primary Dependencies**: 
- Backend: `spring-boot-starter-data-redis`, `spring-boot-starter-websocket`, `minio`
- Frontend: `@stomp/stompjs` (for WebSockets)
**Storage**: 
- Primary: PostgreSQL 16 + TimescaleDB
- Cache: Redis 7.x
- Object Store: MinIO (S3 Compatible)
**Testing**: JUnit 5 (Backend), Vitest/Jest (Frontend)
**Target Platform**: Docker Compose (Local Dev), Linux Containers (Prod)
**Project Type**: Web Application (Monorepo: Backend + Frontend)
**Performance Goals**: < 100ms latency for flight updates via WebSocket.
**Constraints**: Must run locally via `docker-compose`.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Principle I (Simplicity)**: ⚠️ **Warning**. Adding Redis and MinIO increases operational complexity. *Justification*: Required for "Platform Phase 1" scalability and real-time requirements.
- **Principle II (Containerization)**: ✅ **Pass**. Redis and MinIO are already present in `docker-compose.dev.yml` (implied by user request to use them).
- **Principle IV (Tech Stack)**: ⚠️ **Warning**. Redis and MinIO are additions to the strict MVP stack. *Action*: Update Constitution to include these as "Platform Core" components.

## Project Structure

### Documentation (this feature)

```text
specs/003-nifi-ksqldb-integration/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
└── tasks.md             # Phase 2 output
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/utam/
│   ├── config/              # RedisConfig, WebSocketConfig, MinioConfig
│   ├── controller/          # FlightController (WebSocket updates)
│   ├── service/             # FlightService (Redis caching)
│   └── model/               # Updated Entities
└── src/main/resources/

frontend/
├── src/
│   ├── components/Map/      # Real-time Map updates
│   └── services/            # WebSocket Service
```

**Structure Decision**: Standard Spring Boot + React Monorepo structure.

## Complexity Tracking

- **Redis**: Adds cache invalidation complexity.
- **WebSockets**: Adds state management complexity on frontend (reconnection logic).
- **Multi-Tenancy**: Adds strict filtering requirements to all queries.

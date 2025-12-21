# Feature Specification: Platform Phase 1 (Infrastructure & Real-time)

**Feature Branch**: `003-nifi-ksqldb-integration`
**Created**: 2025-12-21
**Status**: Draft
**Input**: User request: "Move from MVP to Platform Phase 1. Architecture should use Redis, MinIO, and WebSockets. Fix DB consistency and multi-tenancy."

## User Scenarios & Testing

### User Story 1 - Real-time Flight Updates (WebSockets)
As an Ops User, I want to see flight movements on the map instantly without manual refresh or polling lag.

**Acceptance Scenarios**:
1. **Given** a flight updates its position, **When** the backend receives the update, **Then** it pushes the new position to the frontend via WebSocket (`/topic/flights`).
2. **Given** the frontend is connected, **When** a WebSocket message arrives, **Then** the map updates the icon position immediately.

### User Story 2 - High Performance Data Access (Redis)
As a System, I want to cache active flight and vehicle data so that the database is not overwhelmed by frequent read requests.

**Acceptance Scenarios**:
1. **Given** a flight position update, **When** it is processed, **Then** the "Active Flights" cache in Redis is updated.
2. **Given** a user requests the current state, **When** the API is called, **Then** the system fetches data from Redis instead of the DB.

### User Story 3 - Multi-Tenant Analytics
As a Tenant Admin (e.g., LIRN), I want to see analytics ONLY for my airport so that data privacy is maintained.

**Acceptance Scenarios**:
1. **Given** I am logged in as a LIRN user, **When** I view the "Hourly Flight Stats", **Then** I only see data where `icao_code = 'LIRN'`.
2. **Given** the system aggregates data, **When** the materialized view is refreshed, **Then** it groups by `icao_code`.

### User Story 4 - Data Lake Storage (MinIO)
As a Data Engineer, I want to archive raw JSON events to object storage so that we have a historical record for audit and replay.

**Acceptance Scenarios**:
1. **Given** a raw event (Flight/Vehicle), **When** it is ingested, **Then** it is asynchronously archived to a MinIO bucket (e.g., `tam-datalake`).

## Requirements

### Functional Requirements
- **FR-001**: System MUST use **WebSockets (STOMP)** to push real-time updates to the frontend.
- **FR-002**: System MUST use **Redis** to cache the current state of all active entities (Flights, Vehicles).
- **FR-003**: System MUST use **MinIO** to store generated reports and/or raw event archives.
- **FR-004**: Database Schema MUST support **Multi-Tenancy** (all tables and views must have `icao_code`).
- **FR-005**: Turnaround Events MUST use the **Start/Stop Event Model** (not Milestone model) to match the ingestion stream.
- **FR-006**: Alerts MUST be stored in a **Hypertable** with `icao_code` support.

### Non-Functional Requirements
- **NFR-001**: WebSocket latency should be < 100ms from Backend to Frontend.
- **NFR-002**: Redis cache should expire stale entities after 5 minutes of inactivity.

### Technical Constraints
- **Backend**: Spring Boot 3.x
- **Frontend**: React + TypeScript (Vite)
- **Cache**: Redis (Containerized)
- **Storage**: MinIO (Containerized)
- **Streaming**: Kafka + KSQLDB (Optional/Future)

## Key Entities (Updated)

- **TurnaroundEvent**: `eventUniqueId`, `activityType`, `eventType` (Start/Stop), `eventTimeStamp`, `stand`, `icaoCode`.
- **Alert**: `alertId`, `type`, `entityId`, `value`, `timestamp`, `icaoCode`.

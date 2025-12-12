# Implementation Plan: Core MVP Tracking & Alerting

**Branch**: `001-mvp-core-tracking` | **Date**: 2025-12-12 | **Spec**: [specs/001-mvp-core-tracking/spec.md](specs/001-mvp-core-tracking/spec.md)
**Input**: Feature specification from `/specs/001-mvp-core-tracking/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Implement the Core MVP for UTAM including Live Flight Tracking, Live Vehicle Tracking, Speed Violation Alerting, and Turnaround Management. The system will use a Monorepo structure with **Apache NiFi** for data ingestion, a Spring Boot backend for processing, and a React frontend for visualization. Communication is event-driven via Kafka, and data is stored in PostgreSQL with TimescaleDB.

## Technical Context

**Language/Version**: Java 17+ (Spring Boot 3.x), TypeScript (React 18+)
**Primary Dependencies**: Apache NiFi (Ingestion), Spring Boot Web, Spring Kafka, React, Leaflet, react-leaflet
**Storage**: PostgreSQL 16+ with TimescaleDB extension
**Testing**: JUnit 5 (Backend), Jest/React Testing Library (Frontend)
**Target Platform**: Docker Containers (Local Development via Docker Compose)
**Project Type**: Monorepo (Backend + Frontend + Infrastructure)
**Performance Goals**: End-to-end latency < 5s (95%), Dashboard load < 3s
**Constraints**: Local execution only, No Kubernetes, HTTP Polling (2-3s refresh)
**Scale/Scope**: MVP: 10 concurrent flights, 10 concurrent vehicles

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **Simplicity First**: Scope limited to Tracking, Alerting, Turnaround. No K8s.
- [x] **Containerization**: Docker Compose specified for all components (including NiFi).
- [x] **Simulation Driven**: Mock ADSB, TelIT, and CV generators included.
- [x] **Tech Stack Compliance**: NiFi, Spring Boot, React, Kafka, PG16+Timescale confirmed.
- [x] **Documentation**: API contracts and architecture defined.

## Project Structure

### Documentation (this feature)

```text
specs/001-mvp-core-tracking/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
backend/
├── src/
│   ├── main/
│   │   ├── java/com/utam/
│   │   │   ├── config/          # Kafka, DB, Security config
│   │   │   ├── controller/      # REST APIs (Query only)
│   │   │   ├── model/           # Entities (Flight, Vehicle, Alert, Turnaround)
│   │   │   ├── service/         # Business Logic (Processing, Alerting)
│   │   │   ├── repository/      # DB Access
│   │   │   ├── simulation/      # Mock Generators (Targeting NiFi)
│   │   └── resources/
│   └── test/
└── pom.xml

frontend/
├── src/
│   ├── components/
│   │   ├── Map/                 # Leaflet Map Component
│   │   ├── Dashboard/           # Main Dashboard View
│   │   ├── Alerts/              # Alert List
│   │   ├── Turnaround/          # Gantt Chart
│   ├── services/                # API Client (Polling)
│   ├── types/                   # TS Interfaces
│   └── App.tsx
├── public/
└── package.json

docker-compose.yml               # Orchestration (NiFi, Kafka, DB, Backend, Frontend)
```

**Structure Decision**: Monorepo with `backend` (Spring Boot) and `frontend` (React) directories at root, plus `docker-compose.yml` managing the infrastructure including the new NiFi container.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| None | N/A | N/A |

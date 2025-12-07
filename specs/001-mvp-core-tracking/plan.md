# Implementation Plan: [FEATURE]

**Branch**: `[###-feature-name]` | **Date**: [DATE] | **Spec**: [link]
**Input**: Feature specification from `/specs/[###-feature-name]/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Implement the Core MVP for UTAM including Live Flight Tracking, Live Vehicle Tracking, and Speed Violation Alerting. The system will use a Monorepo structure with a Spring Boot backend and React frontend, communicating via REST APIs and using Kafka for internal message passing. Data will be stored in PostgreSQL with TimescaleDB.

## Technical Context

**Language/Version**: Java 17+ (Spring Boot 3.x), TypeScript (React 18+)
**Primary Dependencies**: Spring Boot Web, Spring Kafka, React, Leaflet, react-leaflet
**Storage**: PostgreSQL 16+ with TimescaleDB extension
**Testing**: JUnit 5 (Backend), Jest/React Testing Library (Frontend)
**Target Platform**: Docker Containers (Local Development via Docker Compose)
**Project Type**: Monorepo (Backend + Frontend)
**Performance Goals**: End-to-end latency < 5s (95%), Dashboard load < 3s
**Constraints**: Local execution only, No Kubernetes, HTTP Polling (2-3s refresh)
**Scale/Scope**: MVP: 10 concurrent flights, 10 concurrent vehicles

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **Simplicity First**: Scope limited to Tracking & Alerting. No K8s.
- [x] **Containerization**: Docker Compose specified for all components.
- [x] **Simulation Driven**: Mock ADSB and TelIT generators included in architecture.
- [x] **Tech Stack Compliance**: Spring Boot, React, Kafka, PG16+Timescale confirmed.
- [x] **Documentation**: API contracts and architecture defined.

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
backend/
├── src/
│   ├── main/
│   │   ├── java/com/utam/
│   │   │   ├── config/          # Kafka, DB, Security config
│   │   │   ├── controller/      # REST APIs (Ingestion, Query)
│   │   │   ├── model/           # Entities (Flight, Vehicle, Alert)
│   │   │   ├── service/         # Business Logic (Processing, Alerting)
│   │   │   ├── repository/      # DB Access
│   │   │   └── simulation/      # Mock Generators
│   │   └── resources/
│   └── test/
└── pom.xml

frontend/
├── src/
│   ├── components/
│   │   ├── Map/                 # Leaflet Map Component
│   │   ├── Dashboard/           # Main Dashboard View
│   │   └── Alerts/              # Alert List
│   ├── services/                # API Client (Polling)
│   ├── types/                   # TS Interfaces
│   └── App.tsx
├── public/
└── package.json

docker-compose.yml               # Orchestration
```

**Structure Decision**: Monorepo with `backend` (Spring Boot) and `frontend` (React) directories at root.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |

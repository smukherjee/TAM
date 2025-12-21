# Implementation Plan: Turnaround UI Overhaul

**Branch**: `001-turnaround-ui-overhaul` | **Date**: 2025-12-21 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `specs/001-turnaround-ui-overhaul/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Refactor the Turnaround screen into a multi-view dashboard (Grid, Map, Detail) with real-time status, video feeds, and alerts. Backend will be updated to persist turnaround sessions and generate mock flight schedules.

## Technical Context

**Language/Version**: Java 17+ (Backend), TypeScript 5.x (Frontend)
**Primary Dependencies**: Spring Boot 3.x, React 18+, TailwindCSS
**Storage**: PostgreSQL 16+ (TimescaleDB)
**Testing**: JUnit 5 (Backend), Vitest (Frontend)
**Target Platform**: Docker Containers (Linux)
**Project Type**: Web Application
**Performance Goals**: < 2s latency for Grid View updates
**Constraints**: No external media server (use mock video), No K8s
**Scale/Scope**: ~10-20 active stands, single airport terminal

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **Simplicity First**: Avoiding complex media servers; using mock video.
- [x] **Containerization**: All new components (Mock Generator) will run within existing containers.
- [x] **Simulation Driven**: Explicitly using Mock Data Generator for schedules.
- [x] **Tech Stack Compliance**: Using Spring Boot and React as mandated.

## Project Structure

### Documentation (this feature)

```text
specs/001-turnaround-ui-overhaul/
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
│   ├── model/           # TurnaroundSession, TurnaroundTask, Alert
│   ├── service/         # TurnaroundService, MockDataGenerator, RuleEngine
│   └── controller/      # TurnaroundController, AlertController
└── tests/

frontend/
├── src/
│   ├── components/
│   │   ├── Dashboard/   # GridView, StatusCard
│   │   ├── Map/         # MapView, StandMarker
│   │   ├── Turnaround/  # DetailedView, GanttChart, VideoPlayer
│   │   └── Alerts/      # AlertSidebar
│   ├── pages/
│   │   └── TurnaroundPage.tsx
│   └── services/
└── tests/
```

**Structure Decision**: Standard Web Application structure (Backend + Frontend).

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

N/A - Compliant.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |

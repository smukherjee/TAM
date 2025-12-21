# Phase 0: Research & Decisions

**Feature**: Turnaround UI Overhaul
**Date**: 2025-12-21

## 1. Gantt Chart Library

**Decision**: `gantt-task-react`
**Rationale**:
- Lightweight and built for React functional components.
- Supports "controlled component" pattern, allowing synchronization with the video player slider via state.
- Custom rendering capabilities allow for "Planned vs Actual" visualization (e.g., using grouped tasks or custom SVG rendering within cells).
**Alternatives Considered**:
- `dhtmlx-gantt`: Too heavy, steep learning curve, non-native React integration.
- Custom SVG: Too much effort to rebuild scrolling/zooming/dependency logic from scratch.

## 2. Airport Map Visualization

**Decision**: Raw SVG with `react-zoom-pan-pinch`
**Rationale**:
- The terminal map is static and has a small number of stands (~20).
- Raw SVG allows direct manipulation of CSS classes (e.g., `fill-red-500`) based on stand status, which is performant and simple.
- `react-zoom-pan-pinch` provides the necessary interaction (zoom/pan) without the overhead of a full GIS library.
**Alternatives Considered**:
- `react-leaflet`: Overkill for a single terminal building; requires coordinate mapping.
- Canvas API: Harder to style and handle click events compared to SVG DOM elements.

## 3. Mock Data Generation

**Decision**: Spring Boot `@Scheduled` Service -> Kafka
**Rationale**:
- Simulates the real production flow (Ingestion -> Processing -> DB -> Frontend).
- Verifies the entire pipeline including Kafka consumers and WebSocket updates.
- Decouples simulation logic from business logic.
**Implementation Details**:
- Use `datafaker` (or simple randomizer) to generate realistic flight numbers and timestamps.
- Publish events to `flight-raw-json` and `turnaround-events` topics.

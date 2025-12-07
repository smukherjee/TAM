# Research & Decisions: Core MVP Tracking

**Feature**: Core MVP Tracking & Alerting
**Date**: 2025-12-07

## 1. Mock Data Generation

**Problem**: Need to simulate ADS-B (Flight) and TelIT (Vehicle) data streams for the MVP.
**Decision**: Implement as internal components within the Spring Boot Backend.
**Rationale**:

- Reduces operational complexity (fewer containers to manage).
- Easier to control simulation state (start/stop/reset) via the same API.
- Sufficient for MVP scale (10 entities).
**Alternatives Considered**:
- Separate Python scripts: Rejected to avoid multi-language complexity in backend.
- External tools: Rejected to ensure reproducibility without external dependencies.

## 2. Map Visualization

**Problem**: Need a lightweight map component for the React frontend.
**Decision**: Leaflet (via `react-leaflet`).
**Rationale**:

- Lightweight and easy to integrate with React.
- Good support for custom icons (needed for Flight/Vehicle distinction).
- Free tile providers available.
**Alternatives Considered**:
- Google Maps: Rejected due to API key/cost requirements.
- Mapbox: Rejected due to API key requirement.

## 3. Real-time Updates

**Problem**: Frontend needs to show moving entities.
**Decision**: HTTP Polling (2-3s interval).
**Rationale**:

- Simple to implement and debug.
- Meets the latency requirement (< 5s).
- Avoids complexity of WebSockets for the initial MVP.
**Alternatives Considered**:
- WebSockets: Better for real-time, but higher complexity for MVP. Deferred to future phases.
- Server-Sent Events (SSE): Good middle ground, but polling is sufficient for 10 entities.

## 4. Database

**Problem**: Need to store time-series data for tracking.
**Decision**: PostgreSQL 16 with TimescaleDB extension.
**Rationale**:

- Mandated by Constitution.
- Efficient for time-series data (positions).
- Standard SQL interface.

## 5. Message Broker

**Problem**: Decouple ingestion from processing.
**Decision**: Apache Kafka (Single Node).
**Rationale**:

- Mandated by Constitution.
- Handles high throughput if we scale later.
- "Single Node" configuration sufficient for MVP local dev.

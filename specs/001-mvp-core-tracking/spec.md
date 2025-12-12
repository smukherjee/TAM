# Feature Specification: Core MVP Tracking & Alerting

**Feature Branch**: `001-mvp-core-tracking`
**Created**: 2025-12-07
**Status**: Draft
**Input**: User description: "Implement core MVP: Live Flight/Vehicle Tracking (Mock ADSB/TelIT), Basic Speed Alerting, and React Dashboard. Use Spring Boot, Kafka, TimescaleDB."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Live Flight Tracking (Priority: P1)

As an Airport Operations User, I want to view real-time positions of aircraft on a map so that I can monitor airside traffic flow.

**Why this priority**: Core value proposition of the UTAM system; essential for situational awareness.

**Independent Test**: Can be tested by running the Mock ADSB Generator and verifying aircraft icons appear and move on the dashboard map.

**Acceptance Scenarios**:

1. **Given** the Mock ADSB Generator is running, **When** it emits flight position data, **Then** the dashboard map displays aircraft icons at the correct coordinates.
2. **Given** an aircraft is moving, **When** new data points arrive, **Then** the aircraft icon position updates on the map within 5 seconds.
3. **Given** a flight has landed (Status=Landed), **When** the status updates, **Then** the icon reflects the landed state or is removed (based on business rule).

---

### User Story 2 - Live Vehicle Tracking (Priority: P1)

As an Airport Operations User, I want to view real-time positions of ground vehicles on the map so that I can track resource allocation and safety.

**Why this priority**: Critical for monitoring ground operations and safety compliance.

**Independent Test**: Can be tested by running the Mock TelIT Generator and verifying vehicle icons appear and move on the dashboard map.

**Acceptance Scenarios**:

1. **Given** the Mock TelIT Generator is running, **When** it emits vehicle position data, **Then** the dashboard map displays vehicle icons at the correct coordinates.
2. **Given** a vehicle is moving, **When** new data points arrive, **Then** the vehicle icon position updates on the map within 5 seconds.
3. **Given** a vehicle is stopped (Status=STOP), **When** the status updates, **Then** the vehicle icon indicates the stopped state (e.g., color change).

---

### User Story 3 - Speed Violation Alerting (Priority: P2)

As a Safety Officer, I want to receive immediate alerts when a vehicle exceeds the speed limit so that I can enforce safety regulations.

**Why this priority**: Key safety feature for the MVP; demonstrates the "Alerting" capability.

**Independent Test**: Can be tested by configuring the Mock Generator to emit a high-speed value and verifying an alert appears on the dashboard.

**Acceptance Scenarios**:

1. **Given** a vehicle speed limit is defined (default: 30 km/h), **When** a vehicle reports a speed higher than the limit, **Then** the system generates a "Speed Violation" alert.
2. **Given** a Speed Violation alert is generated, **When** the user views the dashboard, **Then** the alert is visible in the "Active Alerts" list.
3. **Given** an alert is generated, **When** the user clicks it, **Then** the map focuses on the offending vehicle.

---

### User Story 4 - Turnaround Management (Priority: P2)

As an Airport Operations User, I want to view the turnaround status of aircraft on a Gantt chart so that I can monitor ground handling efficiency.

**Why this priority**: Provides visibility into critical path activities on the ground.

**Independent Test**: Can be tested by running the Mock CV Event Generator and verifying the Gantt chart updates with new bars.

**Acceptance Scenarios**:

1. **Given** the Mock CV Event Generator is running, **When** it emits start/stop events for an activity (e.g., Fueling), **Then** the Gantt chart displays a corresponding bar.
2. **Given** a specific stand is selected, **When** the user changes the selection, **Then** the chart filters to show only events for that stand.
3. **Given** an activity is ongoing (Start received, no Stop), **When** time passes, **Then** the bar extends to the current time.

### Edge Cases

- What happens when the Mock Generator stops sending data? (System should log the interruption error, attempt to reconnect 3 times, and then alert the administrator).
- How does the system handle invalid coordinates (e.g., Lat > 90)? (Ingestion layer should discard data points with invalid coordinates and log a warning).
- What happens if Kafka is down? (Adapter should log error and retry or fail safe).

## Clarifications

### Session 2025-12-07

- Q: Which map library should be used for the frontend? → A: Leaflet (via react-leaflet).
- Q: Which map tile provider should be used? → A: OpenStreetMap (OSM).
- Q: How should the frontend receive updates? → A: HTTP Polling (every 2-3 seconds).
- Q: What is the preferred repository structure? → A: Monorepo (Single Repo).
- Q: What are the default credentials for the local environment? → A: admin / admin.
- Q: What is the default speed limit for vehicle alerts? → A: 30 km/h.
- Q: What is the default map center location? → A: Indira Gandhi International Airport (IGIA).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST ingest simulated ADS-B flight data via the standard flight data interface (`POST /api/adsblivedata`).
- **FR-002**: System MUST ingest simulated TelIT vehicle data via the standard vehicle data interface (`POST /veh_live_data_con`).
- **FR-003**: System MUST publish raw ingestion data to a message broker for asynchronous processing.
- **FR-004**: System MUST process vehicle streams to detect speed violations based on a configurable threshold (default: 30 km/h).
- **FR-005**: System MUST persist latest position and alert history to a time-series database.
- **FR-006**: System MUST expose an API for the Frontend to fetch current positions of all active flights and vehicles.
- **FR-007**: System MUST expose an API for the Frontend to fetch active alerts.
- **FR-008**: Frontend MUST render an interactive map using **Leaflet** and **OpenStreetMap** tiles with distinct icons for Flights and Vehicles.
- **FR-009**: Frontend MUST auto-refresh data via **HTTP Polling** (every 2-3s) to show movement in near real-time.
- **FR-010**: Frontend map MUST default to centering on **Indira Gandhi International Airport (IGIA)** on load.
- **FR-011**: System MUST ingest simulated Computer Vision (CV) turnaround events via `POST /api/turnaround/events`.
- **FR-012**: System MUST persist turnaround events to the database.
- **FR-013**: System MUST expose an API to fetch turnaround events by stand or time range.
- **FR-014**: Frontend MUST render a Gantt chart visualizing the duration of turnaround activities (e.g., Boarding, Fueling).

### Non-Functional Requirements

- **NFR-001**: System MUST use Basic Authentication with default credentials (`admin`/`admin`) for the local MVP environment.
- **NFR-002**: System MUST NOT use Kubernetes for deployment; it MUST use Docker Compose for local orchestration.

### Key Entities

- **Flight**: `LivePlotId`, `Callsign`, `Latitude`, `Longitude`, `Speed`, `Heading`, `Timestamp`.
- **Vehicle**: `VehicleNo`, `Type`, `Latitude`, `Longitude`, `Speed`, `Status`, `Timestamp`.
- **Alert**: `AlertId`, `Type` (SPEED_VIOLATION), `EntityId` (VehicleNo), `Value` (Speed), `Timestamp`, `Location`.
- **TurnaroundEvent**: `EventUniqueId`, `CameraId`, `ActivityType`, `EventType` (Start/Stop), `EventTimeStamp`, `Stand`.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: End-to-end latency (Source generation to Dashboard display) is under 5 seconds for 95% of updates.
- **SC-002**: System handles a simulated load of 10 concurrent flights and 10 concurrent vehicles without degradation.
- **SC-003**: 100% of speed violations generated by the mock source are detected and alerted.
- **SC-004**: Dashboard loads initial map state with all active entities in under 3 seconds.

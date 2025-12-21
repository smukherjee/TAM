# Feature Specification: Turnaround UI Overhaul

**Feature Branch**: `001-turnaround-ui-overhaul`
**Created**: 2025-12-21
**Status**: Draft
**Input**: User description: "check the attached screenshots and plan for refactoring the turnaround screen.. THink deeply and throughly the changes which would need to happen at all layers including backend,kafka,nifi, infrastructure etc.."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Turnaround Dashboard (Grid View) (Priority: P1)

As an Operations Manager, I want to view a grid of all active aircraft stands with their real-time status, video feeds, and key process indicators (Bags, Fuel, Catering, etc.) so that I can quickly identify which turnarounds are at risk.

**Why this priority**: This is the primary monitoring view for the operations center, replacing the current simple list. It provides high-density information needed for situational awareness.

**Independent Test**: Can be tested by mocking the "Active Turnarounds" API and verifying the grid renders correctly with live video placeholders and status pills changing color based on data.

**Acceptance Scenarios**:

1. **Given** multiple active turnarounds, **When** I open the dashboard, **Then** I see a grid of cards, each representing a stand.
2. **Given** a stand with a delayed process (e.g., Fueling), **When** I view its card, **Then** the corresponding status pill is red/orange.
3. **Given** a live video feed is available, **When** I look at the card, **Then** I see the video stream playing.

---

### User Story 2 - Detailed Turnaround Analysis (Timeline View) (Priority: P1)

As a Turnaround Coordinator, I want to click on a specific stand to see a detailed Gantt chart of all operations (planned vs. actual) alongside the live video and flight metadata (SIRT, TOBT, etc.), so that I can investigate the root cause of delays.

**Why this priority**: Essential for deep-dive analysis and resolving specific issues identified in the dashboard.

**Independent Test**: Can be tested by navigating to a specific flight ID and verifying the timeline renders tasks with correct start/end times and the video feed loads.

**Acceptance Scenarios**:

1. **Given** I am on the dashboard, **When** I click a stand card, **Then** I am navigated to the Detailed View.
2. **Given** a turnaround with multiple tasks (Catering, Cleaning), **When** I view the timeline, **Then** I see bars representing the duration of each task, aligned to the time axis.
3. **Given** flight metadata (e.g., In-Block time), **When** I view the details, **Then** I see these timestamps clearly displayed.

---

### User Story 3 - Airport Map View (Priority: P2)

As an Apron Controller, I want to toggle to a map view of the airport terminal showing the spatial location of stands and their status, so that I can understand the physical context of operations (e.g., congestion in a specific cul-de-sac).

**Why this priority**: Provides spatial awareness that the grid view lacks, useful for managing ground traffic and resource allocation.

**Independent Test**: Can be tested by toggling the "Map" switch and verifying the SVG/Map renders with interactive stand markers.

**Acceptance Scenarios**:

1. **Given** I am on the dashboard, **When** I click the "Map" toggle, **Then** the view switches to an airport layout map.
2. **Given** a stand is occupied, **When** I look at the map, **Then** I see an aircraft icon on that stand.
3. **Given** a stand has a critical alert, **When** I look at the map, **Then** the stand marker is highlighted in red.

---

### User Story 4 - Real-time Alerts Sidebar (Priority: P2)

As an Ops Controller, I want to see a persistent sidebar of critical alerts (e.g., "Ground power not connected", "Fueling delayed") sorted by severity, so that I never miss a safety or performance issue.

**Why this priority**: Ensures critical issues are pushed to the user even if they are looking at a different part of the screen.

**Independent Test**: Can be tested by injecting a mock alert and verifying it appears in the sidebar with the correct severity color.

**Acceptance Scenarios**:

1. **Given** a new critical alert is generated, **When** I am on any turnaround page, **Then** the alert appears at the top of the sidebar.
2. **Given** an alert is resolved, **When** the system updates, **Then** the alert is moved to "Archived" or removed from the active list.

### Edge Cases

- What happens when a video feed is unavailable? (Should show a placeholder/error state).
- What happens when flight schedule data is missing? (Should show "N/A" or estimate based on actuals).
- How does the system handle network disconnection? (Should show a global "Offline" warning).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST provide a "Grid View" displaying active turnarounds as cards with: Stand ID, Flight Number, Live Video Feed, and Status Pills for key processes (Bags, Fuel, Catering, Pax, Bridge, Pushback).
- **FR-002**: The system MUST provide a "Map View" rendering the airport layout with interactive stand markers indicating status and occupancy.
- **FR-003**: The system MUST provide a "Detailed View" for a single turnaround, featuring a Gantt chart comparing Planned vs. Actual times for all service tasks.
- **FR-004**: The system MUST display a persistent "Alerts Sidebar" listing active issues (High, Medium, Low severity) with timestamps.
- **FR-010**: The Backend MUST employ a unified rule engine to generate `Alert` entities for both process delays (SOP violations) and safety/compliance issues.
- **FR-005**: The System MUST persist a `TurnaroundSession` entity in the database, creating or updating it dynamically whenever relevant Flight Schedule or Computer Vision events are received.
- **FR-006**: The System MUST calculate the status (Not Started, In Progress, Finished, Delayed) of each sub-task based on event timestamps and hardcoded SOP rules defined in the backend service.
- **FR-007**: The Frontend MUST play mock looping video clips (e.g., MP4/HLS assets) for each stand to simulate live feeds without requiring a real media server.
- **FR-008**: The System MUST include a Mock Data Generator service to produce realistic flight schedule timestamps (SIRT, TOBT, etc.) for development and demonstration purposes.
- [ ] FR-009: The Detailed View MUST include a synchronized timeline slider that controls the playback of the mock MP4 video, allowing users to scrub through the turnaround process.
- **FR-011**: The System MUST publish generated turnaround events to the Kafka topic `turnaround-events` via the NiFi ingestion gateway.

### Key Entities *(include if feature involves data)*

- **TurnaroundSession**: Represents a single flight's turnaround process. Contains: Flight ID, Stand ID, ICAO Code, Schedule (SIRT, TOBT), List of `TurnaroundTask`s, Overall Status.
- **TurnaroundTask**: Represents a specific service (e.g., Fueling). Contains: Type, Status (Pending, Active, Done), ICAO Code, StartTime, EndTime, SLA/Target Time.
- **Alert**: Represents a deviation or issue. Contains: Severity, Message, Timestamp, ICAO Code, Related Stand/Flight.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can view the status of 10+ stands simultaneously in the Grid View with < 2 second latency for status updates.
- **SC-002**: The Detailed View loads the Gantt chart and video feed in under 1 second.
- **SC-003**: Critical alerts (e.g., Safety Violation) appear in the sidebar within 5 seconds of the event occurring in the real world.
- **SC-004**: The Map View accurately reflects the spatial status of all stands (Occupied/Empty/Alert) matching the Grid View data.

## Assumptions

- **Video Feeds**: We assume live video streams are available via a standard URL (e.g., HLS) for each camera/stand.
- **Map Data**: We assume a static SVG or GeoJSON representation of the airport terminal is available.
- **Flight Schedule**: We assume flight schedule data (SIRT, TOBT) is available in the system or can be ingested to calculate delays.
- **SOP Rules**: We assume there are defined rules for when a task is considered "Delayed" (e.g., Fueling must start within 10 mins of In-Block).

## Clarifications

### Session 2025-12-21

- Q: Where should SOP rules for task delays be defined? → A: Hardcoded in Backend (Java constants) for MVP simplicity.
- Q: How is the Turnaround Session state managed? → A: Dynamic Session Creation (Persisted) - Database record updated by events.
- Q: What is the source of flight schedule data? → A: Mock Data Generator - Background service simulating airport schedule.
- Q: How are video feeds handled for development? → A: Mock Looping Video - Static assets cycled by frontend.
- Q: How are Alerts generated? → A: Unified Rule Engine - Backend generates standard Alert objects for both delays and safety issues.

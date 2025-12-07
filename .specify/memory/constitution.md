<!--
Sync Impact Report:
- Version change: 1.0.0 -> 1.1.0 (Architecture Visualization)
- Modified principles: None.
- Added sections: Architecture Diagram (Visualized MVP Scope).
- Removed sections: None.
- Templates requiring updates: None.
- Follow-up TODOs: None.
-->
# Unified Total Airside Management (UTAM) MVP Constitution

## Core Principles

### I. Simplicity First (MVP Focus)

Focus strictly on MVP scope: Live Flight Tracking, Live Vehicle Tracking, Basic Alerting, and Simple Dashboard. Avoid over-engineering; specifically, NO Kubernetes for this phase. Stick to the defined scope to ensure rapid delivery of the proof-of-concept.

### II. Containerization & Local Dev

All components MUST run in Docker containers. The development environment MUST be fully local and reproducible via `docker-compose up`. No cloud dependencies should be required for the core MVP functionality.

### III. Simulation Driven

Since this is a POC, the system MUST rely on mock data generators for ADSB (Flight) and TelIT (Vehicle) data. The system must be able to ingest and process these simulated streams effectively as if they were live data.

### IV. Tech Stack Compliance

Strict adherence to the defined stack:

- Backend: Spring Boot 3.x (Java 17+)
- Frontend: React 18+ with TypeScript
- Message Broker: Apache Kafka (single node)
- Database: PostgreSQL 16+ with TimescaleDB extension

### V. Documentation & Clean Code

Maintain clear documentation for setup, architecture, and API contracts. Follow standard coding conventions for Java and TypeScript. Code should be self-documenting where possible, with comments explaining "why" rather than "what".

## Architecture & Constraints

### Architecture

```mermaid
graph TB
    %% ========== SIMULATION SOURCES (MVP) ==========
    subgraph "Simulation Sources"
        ADSB[Mock ADS-B Generator<br/>Flight Tracking]
        TELIT[Mock TelIT Generator<br/>Vehicle Tracking]
    end
    
    %% ========== INGESTION LAYER ==========
    subgraph "Ingestion Layer"
        ADSB_ADAPTER[ADS-B Adapter<br/>POST /api/adsblivedata]
        VEHICLE_ADAPTER[Vehicle Adapter<br/>POST /veh_live_data_con]
    end
    
    %% ========== MESSAGE BROKER ==========
    subgraph "Message Broker"
        KAFKA[Apache Kafka<br/>Single Node]
        
        subgraph "Topics"
            TOPIC_FLIGHT[flight-raw-avro]
            TOPIC_VEHICLE[vehicle-raw-avro]
            TOPIC_ALERTS[alerts-json]
        end
    end
    
    %% ========== PROCESSING LAYER ==========
    subgraph "Processing Layer"
        STREAM_FLIGHT[Flight Processor<br/>Spring Boot]
        STREAM_VEHICLE[Vehicle Processor<br/>Spring Boot]
        STREAM_ALERT[Alert Processor<br/>Spring Boot]
    end
    
    %% ========== CORE SERVICES ==========
    subgraph "Core Services"
        API_SVC[Backend API Service<br/>Spring Boot]
    end
    
    %% ========== DATA STORAGE ==========
    subgraph "Data Storage"
        TSDB[(PostgreSQL 16 +<br/>TimescaleDB)]
    end
    
    %% ========== PRESENTATION LAYER ==========
    subgraph "Presentation Layer"
        WEB_PORTAL[Web Portal<br/>React + TypeScript]
    end
    
    %% ========== CONNECTIONS ==========
    %% Sources -> Ingestion
    ADSB -->|JSON Array| ADSB_ADAPTER
    TELIT -->|JSON Array| VEHICLE_ADAPTER
    
    %% Ingestion -> Kafka
    ADSB_ADAPTER --> TOPIC_FLIGHT
    VEHICLE_ADAPTER --> TOPIC_VEHICLE
    
    %% Kafka -> Processing
    TOPIC_FLIGHT --> STREAM_FLIGHT
    TOPIC_VEHICLE --> STREAM_VEHICLE
    TOPIC_FLIGHT --> STREAM_ALERT
    TOPIC_VEHICLE --> STREAM_ALERT
    STREAM_ALERT --> TOPIC_ALERTS
    
    %% Processing -> Storage/Services
    STREAM_FLIGHT --> TSDB
    STREAM_VEHICLE --> TSDB
    STREAM_ALERT --> TSDB
    
    %% Services -> Storage
    API_SVC --> TSDB
    
    %% Presentation
    WEB_PORTAL -->|REST API| API_SVC
    
    %% Styling
    style ADSB fill:#e1f5fe
    style TELIT fill:#e1f5fe
    style KAFKA fill:#fff3e0
    style TSDB fill:#ffebee
    style WEB_PORTAL fill:#e3f2fd
```

- **Microservices**: Even for MVP, separate concerns (e.g., Ingestion, Processing, Web).
- **Event-Driven**: Communication via Kafka for real-time data updates.
- **REST API**: For frontend-backend interaction and configuration.
- **Time-Series Storage**: Use TimescaleDB for efficient storage of tracking data.

### Interface Specifications (ICD Alignment)

To ensure future extendibility, the MVP components must adhere to the interfaces defined in `Azure UTAM DIAL ICD Ver 1.0`.

#### 1. Flight Data (ADS-B)

- **Source**: Mock Generator (simulating ADS-B feed).
- **Endpoint**: `POST /api/adsblivedata`
- **Format**: JSON Array of Objects.
- **Key Fields**: `LivePlotId`, `TrackId`, `Time` (Unix), `Latitude`, `Longitude`, `Speed`, `Heading`, `FlightLevel`, `Callsign`, `FlightNumber`, `AcType`, `FlightStatus`.

#### 2. Vehicle Data (TelIT)

- **Source**: Mock Generator (simulating TelIT feed).
- **Endpoint**: `POST /veh_live_data_con`
- **Format**: JSON Array of Objects.
- **Key Fields**: `Vehicle_Name`, `Vehicle_No`, `Latitude`, `Longitude`, `Speed`, `Angle`, `Status` (STOP/RUNNING/IDLE), `Vehicletype`, `GPSActualTime`, `IGN`, `Power`.

#### 3. Alerting (Functional Requirement)

- **Scope**: Vehicle Speed Violations (as per MVP).
- **Logic**: Trigger alert when `Speed` > Limit (configurable).
- **Output**: Alert event with `Timestamp`, `VehicleNo`, `Speed`, `Location`.

### Constraints

- **Performance**: System must handle the simulated load of flights and vehicles without significant lag.
- **Security**: Basic security practices (no hardcoded secrets), but advanced auth is not a priority for MVP unless specified.

## Development Workflow

### Workflow

- **Feature Branches**: All work done in feature branches.
- **Local Testing**: Verify functionality locally using Docker Compose before merging.
- **Build Verification**: Ensure `docker-compose build` passes.
- **Code Review**: Peer review required for all changes, checking against these principles.

## Governance

### Amendment Procedure

Amendments to this constitution require team agreement. Changes must be documented in the Sync Impact Report of the update.

### Versioning Policy

This constitution follows Semantic Versioning (MAJOR.MINOR.PATCH).

- MAJOR: Backward incompatible governance/principle removals or redefinitions.
- MINOR: New principle/section added or materially expanded guidance.
- PATCH: Clarifications, wording, typo fixes.

### Compliance

All Pull Requests and design reviews MUST verify compliance with these principles. Complexity must be justified.

**Version**: 1.1.0 | **Ratified**: 2025-12-07 | **Last Amended**: 2025-12-07

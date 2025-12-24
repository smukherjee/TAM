# TOGAF Information Systems Architecture - Data (Phase C) - TAM Specific

## Data Entity/Data Component Catalog - From TAM Schema

### Core Data Entities (From schema.sql)

| Entity | Description | Business Function | Data Type |
|--------|-------------|-------------------|-----------|
| **flights** | Aircraft flight information with real-time tracking | Flight Coordination | Hypertable (TimescaleDB) |
| **vehicles** | Ground vehicle information and tracking | Ground Operations | Hypertable (TimescaleDB) |
| **alerts** | System alerts and notifications | Monitoring | Hypertable (TimescaleDB) |
| **turnaround_events** | Turnaround process events | Ground Operations | Hypertable (TimescaleDB) |
| **turnarounds** | Turnaround session management | Turnaround Management | Hypertable (TimescaleDB) |
| **turnaround_activities** | Individual turnaround tasks | Task Management | Transactional |
| **stands** | Airport stand information | Resource Management | Master Data |
| **turnaround_alerts** | Turnaround-specific alerts | Monitoring | Transactional |

### Data Components (From TAM Implementation)

| Component | Description | Data Entities | Owner |
|-----------|-------------|---------------|-------|
| **Flight Management** | Flight scheduling and tracking | flights | Operations |
| **Turnaround Management** | Turnaround process tracking | turnarounds, turnaround_activities | Operations |
| **Resource Management** | Resource allocation and tracking | stands, vehicles | Operations |
| **Monitoring System** | Real-time data collection | alerts, turnaround_alerts | IT |
| **Analytics Engine** | Data analysis and predictions | flights, turnarounds (historical) | Analytics |
| **Maintenance System** | Maintenance tracking | Future capability | Maintenance |
| **User Management** | User authentication and authorization | Future capability | IT |
| **Location Services** | Airport layout and navigation | stands | Operations |

## Data Entity/Business Function Matrix - TAM Specific

| Data Entity | Flight Coordination | Ground Operations | Maintenance | Analytics | Compliance |
|-------------|--------------------|-------------------|-------------|----------|------------|
| flights | ✓ (Primary) | ✓ (Reference) | ✓ (Reference) | ✓ (Primary) | ✓ (Reference) |
| vehicles | ✓ (Reference) | ✓ (Primary) | ✓ (Reference) | ✓ (Primary) | ✓ (Reference) |
| turnarounds | ✓ (Reference) | ✓ (Primary) | ✓ (Reference) | ✓ (Primary) | ✓ (Reference) |
| turnaround_activities | ✓ (Reference) | ✓ (Primary) | ✓ (Reference) | ✓ (Primary) | ✓ (Reference) |
| stands | ✓ (Primary) | ✓ (Primary) | ✓ (Reference) | ✓ (Reference) | ✓ (Reference) |
| alerts | ✓ (Reference) | ✓ (Primary) | ✓ (Primary) | ✓ (Primary) | ✓ (Primary) |
| turnaround_alerts | ✓ (Reference) | ✓ (Primary) | ✓ (Primary) | ✓ (Primary) | ✓ (Primary) |
| turnaround_events | ✓ (Reference) | ✓ (Primary) | ✓ (Reference) | ✓ (Primary) | ✓ (Reference) |

## Application/Data Matrix - TAM Specific

| Application | flights | vehicles | turnarounds | turnaround_activities | stands | alerts | turnaround_alerts | turnaround_events |
|-------------|--------|----------|------------|----------------------|--------|-------|-------------------|------------------|
| **Flight Management System** | ✓ (R/W) | ✓ (R) | ✓ (R) | ✓ (R) | ✓ (R) | ✓ (R) | ✓ (R) | ✓ (R) |
| **Turnaround Management System** | ✓ (R) | ✓ (R) | ✓ (R/W) | ✓ (R/W) | ✓ (R/W) | ✓ (R) | ✓ (R/W) | ✓ (R/W) |
| **Resource Management System** | ✓ (R) | ✓ (R) | ✓ (R) | ✓ (R) | ✓ (R/W) | ✓ (R) | ✓ (R) | ✓ (R) |
| **Monitoring System** | ✓ (R) | ✓ (R) | ✓ (R) | ✓ (R) | ✓ (R) | ✓ (R/W) | ✓ (R/W) | ✓ (R/W) |
| **Analytics Engine** | ✓ (R) | ✓ (R) | ✓ (R) | ✓ (R) | ✓ (R) | ✓ (R) | ✓ (R) | ✓ (R) |
| **Mobile Application** | ✓ (R) | ✓ (R) | ✓ (R) | ✓ (R) | ✓ (R) | ✓ (R) | ✓ (R) | ✓ (R) |
| **Web Dashboard** | ✓ (R) | ✓ (R) | ✓ (R) | ✓ (R) | ✓ (R) | ✓ (R) | ✓ (R) | ✓ (R) |

## Logical Data Diagram - TAM Database Schema

```mermaid
classDiagram
    class flights {
        +time TIMESTAMPTZ
        +callsign VARCHAR
        +latitude DOUBLE PRECISION
        +longitude DOUBLE PRECISION
        +speed DOUBLE PRECISION
        +status VARCHAR
        +flight_level DOUBLE PRECISION
    }
    
    class vehicles {
        +id UUID
        +gpsactualtime TIMESTAMPTZ
        +vehicle_no VARCHAR
        +latitude DOUBLE PRECISION
        +longitude DOUBLE PRECISION
        +speed DOUBLE PRECISION
        +status VARCHAR
    }
    
    class turnarounds {
        +id VARCHAR PRIMARY KEY
        +stand_id VARCHAR
        +aircraft_registration VARCHAR
        +inbound_flight_number VARCHAR
        +outbound_flight_number VARCHAR
        +status VARCHAR
        +planned_turnaround_time INTEGER
        +actual_turnaround_time INTEGER
    }
    
    class turnaround_activities {
        +id VARCHAR PRIMARY KEY
        +turnaround_id VARCHAR
        +activity_type VARCHAR
        +activity_name VARCHAR
        +planned_start_time TIMESTAMPTZ
        +planned_end_time TIMESTAMPTZ
        +status VARCHAR
    }
    
    class stands {
        +id VARCHAR PRIMARY KEY
        +terminal VARCHAR
        +stand_type VARCHAR
        +status VARCHAR
        +has_jetbridge BOOLEAN
        +has_gpu BOOLEAN
    }
    
    class alerts {
        +timestamp TIMESTAMPTZ
        +alert_id UUID
        +type VARCHAR
        +entity_id VARCHAR
        +latitude DOUBLE PRECISION
        +longitude DOUBLE PRECISION
    }
    
    class turnaround_alerts {
        +id VARCHAR PRIMARY KEY
        +turnaround_id VARCHAR
        +severity VARCHAR
        +alert_type VARCHAR
        +message TEXT
        +timestamp TIMESTAMPTZ
    }
    
    class turnaround_events {
        +event_time_stamp TIMESTAMPTZ
        +event_unique_id VARCHAR
        +camera_id VARCHAR
        +activity_type VARCHAR
        +stand VARCHAR
    }
    
    turnarounds "1" -- "*" turnaround_activities : contains>
    turnarounds "1" -- "*" turnaround_alerts : generates>
    turnarounds "1" -- "1" stands : assigned_to>
    flights "1" -- "1" turnarounds : related_to>
    vehicles "1" -- "1" turnarounds : assigned_to>
```

## Data Dissemination Diagram - TAM Data Flow

```mermaid
flowchart TD
    A[Data Sources] -->|Flight/Vehicle Data| B[NiFi Ingestion Layer]
    B -->|Kafka Topics| C[Spring Boot Data Processing]
    C -->|JPA Repositories| D[TimescaleDB Storage]
    D -->|Hypertable Queries| E[Analytics Engine]
    D -->|Real-time Queries| F[Operational Systems]
    D -->|Historical Data| G[Reporting Layer]
    E -->|Predictive Models| H[Delay Predictions]
    E -->|Dashboard Data| I[Visualizations]
    F -->|WebSocket| J[Real-time Monitoring]
    F -->|Alert System| K[User Notifications]
    G -->|Management Reports| L[Performance Reports]
    G -->|Compliance Reports| M[Regulatory Reports]
```

## Data Security Diagram - TAM Implementation

```mermaid
flowchart TD
    A[Data Sources] -->|Encrypted| B[Secure Ingestion]
    B -->|Authenticated| C[Spring Security Layer]
    C -->|Authorized| D[Data Processing]
    D -->|Encrypted| E[TimescaleDB Storage]
    E -->|Role-Based| F[Data Access]
    F -->|Audited| G[User Applications]
    G -->|Monitored| H[Security Logging]
    H --> I[Compliance Reporting]
```

## Class Diagram - TAM Domain Model

```mermaid
classDiagram
    class FlightManagement {
        +processFlightData()
        +updateFlightStatus()
        +getFlightInfo()
    }
    
    class TurnaroundManagement {
        +createTurnaroundSession()
        +updateTurnaroundStatus()
        +getTurnaroundInfo()
    }
    
    class ResourceManagement {
        +allocateStand()
        +releaseStand()
        +getStandStatus()
    }
    
    class MonitoringSystem {
        +collectSensorData()
        +generateAlert()
        +getRealTimeStatus()
    }
    
    class AnalyticsEngine {
        +analyzeTurnaroundPerformance()
        +predictDelays()
        +generateInsights()
    }
    
    class AlertService {
        +createAlert()
        +acknowledgeAlert()
        +resolveAlert()
    }
    
    FlightManagement --> TurnaroundManagement : triggers
    FlightManagement --> ResourceManagement : requests
    TurnaroundManagement --> ResourceManagement : allocates
    TurnaroundManagement --> MonitoringSystem : monitors
    MonitoringSystem --> AnalyticsEngine : provides_data
    AnalyticsEngine --> TurnaroundManagement : provides_insights
    MonitoringSystem --> AlertService : generates_alerts
    AlertService --> TurnaroundManagement : affects
```

## Class Hierarchy Diagram - TAM Entity Relationships

```mermaid
classDiagram
    class BaseEntity {
        <<abstract>>
        +created_at TIMESTAMPTZ
        +updated_at TIMESTAMPTZ
        +status VARCHAR
    }
    
    class Flight extends BaseEntity {
        +callsign VARCHAR
        +latitude DOUBLE PRECISION
        +longitude DOUBLE PRECISION
        +updateStatus()
        +validate()
    }
    
    class Vehicle extends BaseEntity {
        +vehicle_no VARCHAR
        +speed DOUBLE PRECISION
        +updateLocation()
        +validate()
    }
    
    class Turnaround extends BaseEntity {
        +stand_id VARCHAR
        +aircraft_registration VARCHAR
        +calculateDuration()
        +updateProgress()
    }
    
    class TurnaroundActivity extends BaseEntity {
        +activity_type VARCHAR
        +planned_start_time TIMESTAMPTZ
        +updateStatus()
        +calculateVariance()
    }
    
    class Stand extends BaseEntity {
        +terminal VARCHAR
        +stand_type VARCHAR
        +checkAvailability()
        +updateStatus()
    }
    
    class Alert extends BaseEntity {
        +alert_type VARCHAR
        +severity VARCHAR
        +trigger()
        +resolve()
    }
    
    Flight "1" -- "1" Turnaround : related_to
    Turnaround "1" -- "*" TurnaroundActivity : contains
    Turnaround "1" -- "1" Stand : assigned_to
    Turnaround "1" -- "*" Alert : generates
    Vehicle "1" -- "1" Turnaround : assigned_to
```

## Data Migration Diagram - TAM Implementation

```mermaid
flowchart TD
    A[Legacy Systems] --> B[Data Extraction]
    B --> C[Data Transformation]
    C --> D[Data Cleansing]
    D --> E[Data Validation]
    E --> F[Data Loading]
    F --> G[TAM System]
    G --> H[Data Verification]
    H -->|Issues| C
    H -->|Success| I[Go-Live]
```

## Data Lifecycle Diagram - TAM Data Management

```mermaid
flowchart TD
    A[Data Creation] --> B[Data Processing]
    B --> C[TimescaleDB Storage]
    C --> D[Data Usage]
    D --> E[Data Archiving]
    E --> F[Data Retention]
    F --> G[Data Deletion]
    D -->|Updates| B
    C -->|Backup| H[Data Backup]
    H --> C
```

## Conceptual Data Diagram - TAM Data Model

```mermaid
flowchart TD
    A[Operational Data] --> B[Flight Operations]
    A --> C[Ground Operations]
    A --> D[Turnaround Operations]
    B --> E[Flight Tracking]
    B --> F[Aircraft Status]
    C --> G[Vehicle Tracking]
    C --> H[Resource Allocation]
    D --> I[Turnaround Sessions]
    D --> J[Activity Management]
    A --> K[Reference Data]
    K --> L[Stand Information]
    K --> M[Alert Types]
    K --> N[Activity Types]
```

## Solution Data Flow Diagram - TAM Architecture

```mermaid
flowchart TD
    A[External Systems] -->|Flight Data| B[NiFi Integration Layer]
    C[Sensors] -->|Real-time Data| B
    D[User Input] -->|Manual Data| B
    B -->|Kafka Topics| E[Spring Boot Processing Engine]
    E -->|JPA Repositories| F[TimescaleDB Operational Database]
    E -->|Historical Queries| G[TimescaleDB Data Warehouse]
    F -->|Real-time APIs| H[Operational Applications]
    G -->|Analytics Queries| I[Analytics Applications]
    H -->|REST APIs| J[React Dashboards]
    I -->|Predictive Models| K[ML Models]
    I -->|Performance Reports| L[Management Reports]
    H -->|WebSocket| M[Alert Systems]
    M -->|Notifications| N[User Interfaces]
```

## TimescaleDB Hypertable Architecture

```mermaid
flowchart TD
    A[TimescaleDB Instance] --> B[Hypertable: flights]
    A --> C[Hypertable: vehicles]
    A --> D[Hypertable: alerts]
    A --> E[Hypertable: turnaround_events]
    A --> F[Hypertable: turnarounds]
    
    B --> G[Time Partitioning: time]
    C --> H[Time Partitioning: gpsactualtime]
    D --> I[Time Partitioning: timestamp]
    E --> J[Time Partitioning: event_time_stamp]
    F --> K[Time Partitioning: outbound_scheduled_departure]
    
    L[Query Performance] --> M[Time-based Queries]
    L --> N[Range Queries]
    L --> O[Aggregation Functions]
    
    P[Data Retention] --> Q[Automatic Partitioning]
    P --> R[Retention Policies]
    P --> S[Compression]
```

## Kafka Data Flow Diagram - TAM Event Streaming

```mermaid
flowchart TD
    A[Data Sources] -->|Flight Data| B[NiFi Listeners]
    A -->|Vehicle Data| B
    A -->|Sensor Data| B
    B -->|Kafka Producers| C[Redpanda Topics]
    C -->|flight-updates| D[Flight Consumer]
    C -->|vehicle-updates| E[Vehicle Consumer]
    C -->|alert-events| F[Alert Consumer]
    D -->|Spring Boot Service| G[FlightRepository]
    E -->|Spring Boot Service| H[VehicleRepository]
    F -->|Spring Boot Service| I[AlertService]
    G --> J[TimescaleDB]
    H --> J
    I --> J
```
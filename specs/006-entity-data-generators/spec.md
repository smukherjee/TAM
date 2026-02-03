# Feature Specification: Entity Data Generators

**Feature Branch**: `006-entity-data-generators`  
**Created**: 2026-02-03  
**Status**: Clarified  
**Input**: User description: "Create realistic data generators for all entities in the application so that all screens have enough data to show all features of the application"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Generate Comprehensive Demo Data (Priority: P1)

As a developer or demo presenter, I want to generate realistic sample data for all entities in the system so that all screens display meaningful content and demonstrate the full capabilities of the application.

**Why this priority**: Without realistic data, the application screens appear empty or show insufficient data to demonstrate features like maps, dashboards, analytics, and reports.

**Independent Test**: Run the data generators and verify all screens (Map, Turnaround, Asset Management, Analytics, Reports) display populated data.

**Acceptance Scenarios**:

1. **Given** an empty database with only tenant configurations, **When** I run the data generators, **Then** all entity tables are populated with realistic data
2. **Given** the data generators have run, **When** I view the Map page, **Then** I see vehicles and flights with GPS coordinates moving on the map
3. **Given** the data generators have run, **When** I view the Turnaround page, **Then** I see active turnaround sessions with tasks and alerts
4. **Given** the data generators have run, **When** I view the Asset Management page, **Then** I see a list of assets with categories, locations, and statuses

---

### User Story 2 - Multi-Tenant Data Generation (Priority: P1)

As a platform operator, I want data generators to produce data for multiple tenants (VIDP, YBBN, LIRN) so that I can demonstrate multi-tenant isolation and tenant-specific views.

**Why this priority**: Multi-tenancy is a core feature; each tenant should have its own realistic dataset with airport-specific characteristics.

**Independent Test**: Run generators for each tenant and verify data is correctly isolated per tenant.

**Acceptance Scenarios**:

1. **Given** multiple tenants exist (VIDP, YBBN), **When** I run data generators, **Then** each tenant has its own set of flights, vehicles, and assets
2. **Given** data exists for multiple tenants, **When** I filter by tenant VIDP, **Then** I only see Delhi airport-specific data
3. **Given** data exists for multiple tenants, **When** I filter by tenant YBBN, **Then** I only see Brisbane airport-specific data

---

### User Story 3 - Continuous Real-Time Data Simulation (Priority: P2)

As a user testing the live tracking features, I want data generators to continuously produce real-time position updates so that the map and tracking screens show dynamic movement.

**Why this priority**: Static data doesn't demonstrate the real-time tracking capabilities of the system.

**Independent Test**: Enable continuous generation and observe position updates on the map.

**Acceptance Scenarios**:

1. **Given** continuous generation is enabled, **When** I watch the map page, **Then** I see vehicles and flights moving in real-time
2. **Given** continuous generation is running, **When** I view asset movement trail, **Then** I see new trail points appearing periodically
3. **Given** continuous generation is running, **When** a vehicle enters a restricted zone, **Then** a zone violation is generated

---

### User Story 4 - Generate Security and Compliance Data (Priority: P2)

As a security operator, I want the data generators to create restricted zones, zone violations, and movement discrepancies so that I can test security monitoring features.

**Why this priority**: Security features like zone violations and discrepancy reports need data to demonstrate their value.

**Independent Test**: Run security data generators and verify violation and discrepancy reports show data.

**Acceptance Scenarios**:

1. **Given** restricted zones are defined, **When** data generators run, **Then** some assets are placed inside restricted zones
2. **Given** zone violations exist, **When** I view the Restricted Zone Report page, **Then** I see violation entries with severity levels
3. **Given** movement discrepancies exist, **When** I view the Movement Discrepancy Report page, **Then** I see discrepancy entries with types and severities

---

### User Story 5 - Generate Turnaround Operations Data (Priority: P1)

As an operations manager, I want realistic turnaround session data with all 30+ sub-processes, correlated vehicle movements, and milestone timings so that I can demonstrate Deep Turnaround-style monitoring and efficiency tracking.

**Why this priority**: Turnaround management is the core value proposition; it needs complete sessions with all ground service equipment coordinated with flight arrivals/departures.

**Independent Test**: View the Turnaround page and verify all sub-processes, vehicle correlations, and timing milestones are visible.

**Acceptance Scenarios**:

1. **Given** a flight arrives at a stand, **When** data generators run, **Then** a turnaround session is created with AIBT (Actual In-Block Time) matching the flight arrival
2. **Given** a turnaround session is active, **When** I view its detail page, **Then** I see a Gantt-style timeline showing all 30+ sub-processes with planned vs actual times
3. **Given** turnaround tasks include fuel, catering, cleaning, baggage, and boarding, **When** I view the session, **Then** each task shows vehicle assignments with arrival/departure times
4. **Given** a turnaround completes, **When** the aircraft departs, **Then** AOBT (Actual Off-Block Time) is recorded and total turnaround duration is calculated
5. **Given** multiple turnarounds are active, **When** I view the operations dashboard, **Then** I see turnarounds in states: On-Time, At-Risk, Delayed, Completed

---

### User Story 5a - Vehicle-to-Flight Correlation (Priority: P1)

As an operations controller, I want to see which ground service vehicles are assigned to which flights so that I can monitor turnaround progress in real-time.

**Why this priority**: The closed-loop optimization requires correlating vehicle positions with turnaround tasks to enable predictive alerts.

**Independent Test**: Select a turnaround session and verify all assigned vehicles are listed with their current positions and task status.

**Acceptance Scenarios**:

1. **Given** a turnaround session exists, **When** I view vehicle assignments, **Then** I see: Fuel Truck, Catering Trucks (fore/aft), Baggage Tugs, Belt Loaders, GPU, Pushback Tug
2. **Given** a fuel truck is assigned to Flight AI101, **When** I track the fuel truck, **Then** I see its path from fuel depot → aircraft stand → next assignment
3. **Given** baggage loading is in progress, **When** I view the baggage vehicles, **Then** I see multiple tugs and their cart counts moving between baggage hall and aircraft
4. **Given** a catering truck arrives at aircraft, **When** the high-lift reaches door height, **Then** a "Catering Positioned" milestone is recorded
5. **Given** pushback is requested, **When** the pushback tug arrives at the nose gear, **Then** the "Pushback Ready" milestone is recorded with tug ID

---

### User Story 5b - Comprehensive Ground Service Equipment Fleet (Priority: P1)

As a ground handler manager, I want all types of ground service equipment (GSE) to be simulated with realistic fleet sizes so that I can demonstrate resource allocation scenarios.

**Why this priority**: Real turnaround optimization requires visibility into all 15+ vehicle types with realistic quantities per airport.

**Independent Test**: View the vehicle fleet and verify all GSE categories are represented with appropriate quantities.

**Acceptance Scenarios**:

1. **Given** VIDP airport, **When** I view the vehicle fleet, **Then** I see: 20 Fuel Trucks, 15 Catering Trucks, 30 Baggage Tugs, 25 Belt Loaders, 20 GPUs, 15 Pushback Tugs, 10 Passenger Stairs, 8 Water Trucks, 8 Lavatory Trucks, 5 De-icing Trucks
2. **Given** each vehicle type has specific routes, **When** vehicles move, **Then** fuel trucks use fuel depot routes, catering trucks use catering facility routes, baggage tugs use baggage hall routes
3. **Given** a vehicle is assigned to a turnaround, **When** the task completes, **Then** the vehicle is released and moves to its next assignment or parking area
4. **Given** multiple turnarounds need the same vehicle type, **When** resources are constrained, **Then** realistic queuing occurs and delays propagate

---

### User Story 5c - Extensive Turnaround Alerts (Priority: P1)

As an operations supervisor, I want to see predictive alerts 30+ minutes in advance showing delay risks and financial impact so that I can take proactive action to prevent cascading delays.

**Why this priority**: The $100-150/minute delay cost and $11M+ annual revenue opportunity requires extensive alerting to demonstrate value.

**Independent Test**: View the alerts dashboard and verify alerts appear for various delay scenarios with financial impact estimates.

**Acceptance Scenarios**:

1. **Given** a fuel truck is running 10 minutes late, **When** this impacts the turnaround, **Then** an alert appears: "Fuel Delay Risk - Flight AI101 - ETA +10min - Cost Impact: $1,500"
2. **Given** baggage loading is 15 minutes behind schedule, **When** I view alerts, **Then** I see: "Baggage Loading Critical - Recommend: Deploy additional tug - Slot at Risk"
3. **Given** catering has not arrived and boarding is scheduled in 20 minutes, **When** the system predicts impact, **Then** an alert shows: "Catering Not Positioned - Boarding Delay Risk - Network Cascade: 3 downstream flights"
4. **Given** a departure slot will be missed, **When** the delay exceeds threshold, **Then** a critical alert shows: "Slot Miss Imminent - Flight BA249 - Revenue Impact: $50,000 - Recommend: Gate change or slot rebooking"
5. **Given** crew duty time limits approach, **When** a delay compounds, **Then** an alert shows: "Crew Duty Violation Risk - Flight LH760 - If delayed >15min, cancellation required - Cost: $150,000"

---

### User Story 5d - Financial Impact Tracking (Priority: P1)

As an airport CFO, I want to see the financial impact of delays and the value of prevented delays so that I can measure ROI of the turnaround optimization system.

**Why this priority**: The business case requires quantifying $100-150/minute delay costs and $11M+ capacity revenue gains.

**Independent Test**: View the financial dashboard and verify delay costs and saved costs are calculated and displayed.

**Acceptance Scenarios**:

1. **Given** a turnaround is delayed by 20 minutes, **When** I view the session, **Then** I see: "Delay Cost: $2,500 (airline) + $15,000 (slot value)"
2. **Given** an alert was actioned and delay prevented, **When** I view the session, **Then** I see: "Delay Prevented: 15 minutes - Value Protected: $22,500"
3. **Given** a daily operations summary, **When** I view analytics, **Then** I see: "Total Delays: 45 min - Cost: $6,750 | Delays Prevented: 120 min - Value Saved: $18,000"
4. **Given** monthly analytics, **When** I view the trend, **Then** I see: "Capacity Gained: 5 additional ATMs/day - Revenue Impact: $11M annualized"
5. **Given** cascade events occurred, **When** I view impact analysis, **Then** I see: "Network Cascade Events: 3 - Total Impact: $500,000 - Downstream Flights Affected: 12"

---

### User Story 6 - Generate Analytics and Hotspot Data (Priority: P3)

As an analyst, I want sufficient historical data so that analytics dashboards and hotspot analysis show meaningful patterns.

**Why this priority**: Analytics features require historical data to display trends and patterns.

**Independent Test**: View Analytics and Hotspot Analysis pages with generated historical data.

**Acceptance Scenarios**:

1. **Given** historical movement data exists, **When** I view the Analytics page, **Then** I see charts with meaningful data points
2. **Given** movement trail data spans multiple days, **When** I view Hotspot Analysis, **Then** I see heat maps with activity concentrations
3. **Given** historical turnaround data exists, **When** I view turnaround analytics, **Then** I see performance trends over time

---

### User Story 7 - Realistic Arrival and Departure Flight Data (Priority: P1)

As a demo presenter, I want flight data to reflect realistic arrival and departure schedules based on actual airport flight information so that the application shows authentic flight tracking.

**Why this priority**: Realistic flight data is essential for demonstrating the core tracking functionality with recognizable flight patterns.

**Independent Test**: Compare generated flight data against known arrival/departure patterns for each airport.

**Acceptance Scenarios**:

1. **Given** VIDP (Delhi) airport is selected, **When** flight data is generated, **Then** arriving flights approach from realistic directions with decreasing altitude and departing flights climb away from the airport
2. **Given** YBBN (Brisbane) airport is selected, **When** flight data is generated, **Then** flight numbers, airlines, and routes match realistic Brisbane traffic patterns
3. **Given** a flight is marked as "arriving", **When** I track its movement, **Then** it moves toward the airport runway with realistic speed and heading changes
4. **Given** a flight is marked as "departing", **When** I track its movement, **Then** it moves away from the airport with increasing altitude and speed

---

### User Story 8 - Admin Path Drawing for Vehicle Movement (Priority: P1)

As an administrator, I want to draw movement paths on a map so that I can define realistic vehicle routes within the airport for data generation.

**Why this priority**: Manual path definition enables precise control over vehicle movement patterns that match actual airport taxiways and service roads.

**Independent Test**: Draw a path on the admin screen and verify generated vehicle data follows that path.

**Acceptance Scenarios**:

1. **Given** I am on the Data Generator Admin screen, **When** I click points on the airport map, **Then** a path is drawn connecting those points
2. **Given** I have drawn a vehicle path, **When** I save the path, **Then** it is stored with a name and vehicle type association
3. **Given** saved paths exist, **When** I run vehicle data generation, **Then** vehicles follow the defined paths with realistic timing
4. **Given** I am editing a path, **When** I drag a waypoint, **Then** the path updates in real-time
5. **Given** a path is defined, **When** I preview it, **Then** I see an animated vehicle moving along the path

---

### User Story 9 - GeoJSON Zone Editor for Restricted Areas (Priority: P1)

As an administrator, I want to draw and edit restricted zones on a map (similar to geojson.io) so that I can define security boundaries visually without writing GeoJSON manually.

**Why this priority**: Visual zone editing is essential for accurately defining complex polygon boundaries for security zones.

**Independent Test**: Draw a polygon zone on the map and verify it is saved correctly with all properties.

**Acceptance Scenarios**:

1. **Given** I am on the Zone Editor screen, **When** I select the polygon tool, **Then** I can click points to define a zone boundary
2. **Given** I am drawing a polygon, **When** I click the starting point again, **Then** the polygon is closed and can be saved
3. **Given** a zone polygon exists, **When** I click on it, **Then** I can edit its vertices by dragging them
4. **Given** I am editing a zone, **When** I set zone properties (name, type, authorized categories), **Then** these are saved with the geometry
5. **Given** zones exist, **When** I export them, **Then** I receive valid GeoJSON that can be imported into other tools
6. **Given** I have a GeoJSON file, **When** I import it, **Then** the zones are displayed on the map and can be edited

---

### User Story 10 - Airport Boundary Constraint for Ground Assets (Priority: P2)

As a system user, I want all ground vehicle and asset movements to stay within airport boundaries so that the simulation appears realistic.

**Why this priority**: Ground assets should never appear outside the airport perimeter in a realistic simulation.

**Independent Test**: Run generators and verify no vehicle or asset positions fall outside airport boundaries.

**Acceptance Scenarios**:

1. **Given** airport boundaries are defined for VIDP, **When** vehicle data is generated, **Then** all positions are within the Delhi airport perimeter
2. **Given** airport boundaries are defined for YBBN, **When** asset data is generated, **Then** all positions are within the Brisbane airport perimeter
3. **Given** a defined vehicle path exits the airport boundary, **When** I try to save it, **Then** I receive a validation warning

---

### Edge Cases

- What happens when data generators are run on a database with existing data?
  - *Assumption: Generators should check for existing records and add data incrementally without duplicates*
- What happens when a tenant doesn't exist in the database?
  - *Assumption: Generators should skip or log a warning if referenced tenant is missing*
- How does the system handle very large data generation requests?
  - *Assumption: Generators should have configurable batch sizes and support gradual data population*
- What happens when a drawn path extends outside airport boundaries?
  - *Assumption: System should warn the user and optionally clip the path to airport boundaries*
- What happens when imported GeoJSON contains invalid geometry?
  - *Assumption: System should validate and report errors, allowing partial import of valid features*
- What happens when flight schedules change mid-simulation?
  - *Assumption: New flight data should seamlessly integrate without disrupting active flight tracking*

## Requirements *(mandatory)*

### Functional Requirements

#### Core Data Generation

- **FR-001**: System MUST provide data generators for all core entities: Flight, Vehicle, Asset, User, Tenant
- **FR-002**: System MUST provide data generators for tracking entities: AssetLocationRegister, AssetMovementTrail
- **FR-003**: System MUST provide data generators for security entities: RestrictedZone, ZoneViolation, MovementDiscrepancy
- **FR-004**: System MUST provide data generators for turnaround entities: TurnaroundSession, TurnaroundTask, Alert
- **FR-005**: System MUST provide data generators for sensor data: VehicleAlert (SensorAlerts)
- **FR-006**: System MUST provide data generators for configuration: TenantConfiguration

#### Data Realism

- **FR-007**: Generated GPS coordinates MUST be within realistic bounds for each airport (VIDP: Delhi, YBBN: Brisbane)
- **FR-008**: Generated flight numbers MUST follow realistic airline codes (AI, BA, LH, EK, QF, etc.)
- **FR-009**: Generated vehicle IDs MUST follow realistic naming conventions (TUG-001, FUEL-01, GPU-003)
- **FR-010**: Generated turnaround timings MUST follow realistic airport operations (30-90 minute turnarounds)
- **FR-011**: Generated assets MUST have realistic categories (Ground Support Equipment, Baggage Handling, etc.)

#### Realistic Flight Generation

- **FR-022**: Flight generator MUST distinguish between arriving and departing flights
- **FR-023**: Arriving flights MUST show decreasing altitude and speed as they approach the airport
- **FR-024**: Departing flights MUST show increasing altitude and speed as they leave the airport
- **FR-025**: Flight headings MUST be appropriate for approach/departure paths for each airport runway
- **FR-026**: Flight data MUST include realistic airline codes for each airport (e.g., Air India, IndiGo for VIDP; Qantas, Virgin for YBBN)
- **FR-027**: Flight schedules SHOULD reflect typical traffic patterns (peak hours, quiet periods)

#### Airport Boundary Constraints

- **FR-028**: All ground vehicle positions MUST remain within defined airport perimeter boundaries
- **FR-029**: All ground asset positions MUST remain within defined airport perimeter boundaries
- **FR-030**: Airport boundaries MUST be defined as GeoJSON polygons for each tenant
- **FR-031**: System MUST validate generated positions against airport boundary before persisting

#### Multi-Tenant Support

- **FR-012**: All generators MUST support tenant-specific data generation with tenant_code parameter
- **FR-013**: Generated data MUST include appropriate timezone handling per tenant
- **FR-014**: Generators MUST support at least these tenants: VIDP (Delhi), YBBN (Brisbane)

#### Continuous Generation

- **FR-015**: Generators MUST support one-time batch generation mode for initial data population
- **FR-016**: Generators MUST support continuous scheduled generation for real-time simulation
- **FR-017**: Generators MUST be configurable via application properties (rate, volume, enabled/disabled)
- **FR-075**: System MUST implement configurable data retention with default of 90 days rolling cleanup
- **FR-076**: Retention period MUST be configurable per entity type via application properties

#### Observability

- **FR-079**: Generators MUST log key events (start, stop, batch completion, errors) at appropriate log levels
- **FR-080**: Generators MUST expose Micrometer metrics for: records generated per entity type, generation rate, error count, active generators
- **FR-081**: System MUST provide a health endpoint indicating generator status (running, stopped, error)
- **FR-082**: Metrics MUST be scrapable by Prometheus for monitoring dashboards

#### Data Relationships

- **FR-018**: Generated VehicleAlerts MUST reference existing vehicle IDs
- **FR-019**: Generated TurnaroundTasks MUST be linked to TurnaroundSessions
- **FR-020**: Generated ZoneViolations MUST reference existing RestrictedZones and Assets
- **FR-021**: Generated MovementDiscrepancies MUST reference existing Assets

#### Admin Path Drawing Interface

- **FR-032**: System MUST provide an admin screen for data generator configuration
- **FR-077**: Admin screens (path drawing, zone editor, generator config) MUST require ADMIN role authentication
- **FR-033**: Admin screen MUST display an interactive map of the selected airport
- **FR-034**: Users MUST be able to draw movement paths by clicking waypoints on the map
- **FR-035**: Paths MUST be saveable with a name, vehicle type, and optional schedule
- **FR-036**: Users MUST be able to edit existing paths by dragging waypoints
- **FR-037**: Users MUST be able to delete paths
- **FR-038**: System MUST provide path preview showing animated vehicle movement along the path
- **FR-039**: Paths MUST be stored in a format that allows replay for data generation

#### GeoJSON Zone Editor

- **FR-040**: System MUST provide a visual zone editor similar to geojson.io
- **FR-041**: Zone editor MUST support drawing polygons by clicking vertices
- **FR-042**: Zone editor MUST support editing existing polygons by dragging vertices
- **FR-043**: Zone editor MUST support deleting vertices and entire polygons
- **FR-044**: Zone editor MUST allow setting zone properties (name, type, authorized asset categories, description)
- **FR-045**: Zone editor MUST support importing GeoJSON files
- **FR-046**: Zone editor MUST support exporting zones as GeoJSON
- **FR-047**: Zone editor MUST validate geometry (no self-intersecting polygons, minimum 3 vertices)
- **FR-083**: Zone editor MUST support undo/redo with standard keyboard shortcuts (Ctrl+Z/Ctrl+Y)
- **FR-048**: Zone editor MUST display zone boundaries with color-coding by zone type

#### Turnaround-Flight Correlation

- **FR-049**: Turnaround sessions MUST be automatically linked to arriving flights when aircraft reaches stand
- **FR-050**: Turnaround sessions MUST record AIBT (Actual In-Block Time) matching flight arrival timestamp
- **FR-051**: Turnaround sessions MUST record AOBT (Actual Off-Block Time) when aircraft departs
- **FR-052**: Each turnaround MUST include all 30+ sub-processes as defined by Deep Turnaround methodology
- **FR-053**: Turnaround milestones MUST include: Chocks On, Doors Open, Jetbridge Connect, GPU Connect, Fuel Start, Fuel Stop, Catering Start, Catering Complete, Cleaning Start, Cleaning Complete, Boarding Start, Boarding Complete, Doors Close, Jetbridge Disconnect, Pushback Ready, Chocks Off
- **FR-054**: Each milestone MUST have planned time and actual time for comparison
- **FR-055**: Turnaround generator MUST create sessions in various states: On-Time, At-Risk, Delayed, Completed

#### Ground Service Equipment (GSE) Fleet

- **FR-056**: System MUST generate the following vehicle types with realistic quantities per airport:
  - Fuel Trucks (20 per hub)
  - Catering High-Lift Trucks (15 per hub, fore and aft assignments)
  - Baggage Tugs with Carts (30 tugs, 100+ carts)
  - Belt Loaders (25 per hub)
  - Ground Power Units/GPU (20 per hub)
  - Pushback Tugs (15 per hub)
  - Passenger Stairs (10 per hub)
  - Water Service Trucks (8 per hub)
  - Lavatory Service Trucks (8 per hub)
  - De-icing Trucks (5 per hub, seasonal)
  - Air Start Units/ASU (5 per hub)
  - Passenger Buses (10 per hub)
  - Cargo Loaders (10 per hub)
- **FR-057**: Each vehicle type MUST have defined depot/parking locations
- **FR-058**: Vehicles MUST follow realistic routes: depot → stand → next assignment or return

#### Vehicle-to-Turnaround Assignment

- **FR-059**: Each turnaround task MUST be assigned to specific vehicle(s)
- **FR-078**: Vehicle assignments MUST use proximity-based auto-assignment (nearest available vehicle of required type)
- **FR-060**: Vehicle assignments MUST include: arrival time at stand, task start, task complete, departure from stand
- **FR-061**: System MUST track vehicle utilization across multiple turnarounds
- **FR-062**: When vehicle is delayed, all dependent turnarounds MUST show impact
- **FR-063**: Vehicle position updates MUST correlate with turnaround task progress

#### Extensive Alert Generation

- **FR-064**: System MUST generate predictive alerts 30+ minutes before delays occur
- **FR-065**: Alert types MUST include:
  - Vehicle Delay Risk (fuel, catering, baggage, etc.)
  - Task Behind Schedule
  - Milestone Missed
  - Slot at Risk
  - Slot Missed
  - Network Cascade Warning
  - Crew Duty Time Warning
  - Passenger Connection Risk
  - Equipment Malfunction
  - Weather Impact
  - Gate Change Required
  - SLA Breach Imminent
- **FR-066**: Each alert MUST include:
  - Severity level (Critical, High, Medium, Low)
  - Affected flight(s)
  - Financial impact estimate
  - Recommended action
  - Time remaining to resolve
- **FR-067**: Alerts MUST show cost impact using standard rates ($100-150/minute delay)
- **FR-068**: Critical alerts MUST include network cascade impact (downstream flights affected)

#### Financial Impact Tracking

- **FR-069**: System MUST calculate delay cost per turnaround (delay_minutes × $125/minute)
- **FR-070**: System MUST calculate slot value at risk ($20,000-$80,000 per movement depending on time of day)
- **FR-071**: System MUST track "prevented delays" when alerts are actioned
- **FR-072**: System MUST calculate value protected from prevented delays
- **FR-073**: System MUST generate daily/weekly/monthly financial summaries:
  - Total delay minutes and cost
  - Prevented delay minutes and value saved
  - Additional ATMs enabled
  - Annualized revenue impact
- **FR-074**: Financial data MUST demonstrate the $11M+ annual opportunity from 5-minute turnaround improvement

### Key Entities

The following entities require data generators:

| Entity | Table | Description |
|--------|-------|-------------|
| **Tenant** | tenants | Airport tenants (VIDP, YBBN, LIRN) |
| **User** | users | System users with roles |
| **Flight** | flights | Aircraft position and status data (arrivals and departures) |
| **Vehicle** | vehicles | Ground vehicle tracking data (15+ GSE types) |
| **VehicleType** | vehicle_types | GSE categories with depot locations and capacities |
| **Asset** | assets | Physical assets (equipment, tools) |
| **VehicleAlert** | sensor_alerts | Alert events from vehicle sensors |
| **TurnaroundEvent** | turnaround_events | Raw turnaround activity events (30+ sub-processes) |
| **TurnaroundSession** | turnaround_sessions | Complete turnaround operations with AIBT/AOBT |
| **TurnaroundTask** | turnaround_tasks | Individual tasks within turnarounds with vehicle assignments |
| **TurnaroundMilestone** | turnaround_milestones | Key timing points (Chocks, Doors, Fuel, Catering, Boarding, Pushback) |
| **Alert** | turnaround_alerts | Predictive alerts with severity and financial impact |
| **VehicleAssignment** | vehicle_assignments | Links vehicles to turnaround tasks with timing |
| **RestrictedZone** | restricted_zones | Security zones with boundaries |
| **ZoneViolation** | zone_violations | Unauthorized zone entries |
| **MovementDiscrepancy** | movement_discrepancies | Tracking anomalies |
| **AssetLocationRegister** | asset_location_register | Current asset positions |
| **AssetMovementTrail** | asset_movement_trail | Historical position records |
| **TenantConfiguration** | tenant_configurations | Tenant-specific settings |
| **VehiclePath** | vehicle_paths | User-defined movement paths for vehicles |
| **AirportBoundary** | airport_boundaries | GeoJSON polygon defining airport perimeter |
| **FinancialMetric** | financial_metrics | Delay costs, saved costs, capacity gains |
| **Stand** | stands | Aircraft parking positions with coordinates |
| **Depot** | depots | Vehicle parking/service locations (fuel, catering, baggage hall) |

## Success Criteria *(mandatory)*

### Measurable Outcomes

#### Core Data Generation
- **SC-001**: All 24 entity types have working data generators that produce valid records
- **SC-002**: Each airport tenant has at least 50 assets, 150+ vehicles (across all GSE types), and 20 active flights after generation
- **SC-003**: Each tenant has at least 20 turnaround sessions in various states (on-time, at-risk, delayed, completed)
- **SC-004**: At least 5 restricted zones per airport with sample zone violations
- **SC-005**: Movement trail table contains at least 5000 historical position records per tenant
- **SC-006**: All frontend screens display populated data after running generators (no empty states)
- **SC-007**: Continuous generation mode produces at least 5 position updates per second during simulation
- **SC-008**: Generated data passes database constraint validation (no foreign key errors, valid enums)
- **SC-009**: Generators complete initial data population within 120 seconds for all entities

#### Flight-Turnaround Correlation
- **SC-010**: 100% of generated coordinates fall within valid airport boundaries
- **SC-011**: Each airport has at least 10 arriving and 10 departing flights per hour during peak simulation
- **SC-012**: Arriving flights show descending altitude profile; departing flights show ascending profile
- **SC-013**: Every arriving flight creates a corresponding turnaround session with correct AIBT
- **SC-014**: Every turnaround includes all 30+ sub-processes with timing data
- **SC-015**: 100% of turnaround milestones are recorded with planned vs actual times

#### Vehicle-Turnaround Correlation
- **SC-016**: Each turnaround has at least 8 distinct vehicle assignments (fuel, catering, baggage, etc.)
- **SC-017**: Vehicle movements are traceable from depot → stand → next assignment
- **SC-018**: Vehicle position timestamps correlate with turnaround task start/complete times
- **SC-019**: When a vehicle is delayed, dependent turnarounds show updated predictions
- **SC-020**: GSE fleet includes all 15 vehicle types with realistic quantities

#### Alerts and Financial Impact
- **SC-021**: System generates at least 50 alerts per hour during active simulation
- **SC-022**: Alerts include all 12 types (Vehicle Delay, Task Behind, Slot Risk, etc.)
- **SC-023**: 100% of alerts include financial impact estimate
- **SC-024**: Critical alerts show network cascade impact (downstream flights)
- **SC-025**: Financial dashboard shows daily totals: delay cost, prevented cost, capacity gains
- **SC-026**: System demonstrates $11M+ annualized opportunity from 5-min turnaround improvement

#### Admin Tools
- **SC-027**: Admin path drawing screen allows creation of at least 20 vehicle paths per airport
- **SC-028**: GeoJSON zone editor supports polygon creation, editing, import, and export
- **SC-029**: Users can define a vehicle path and see generated data follow that path within 30 seconds
- **SC-030**: Zone editor validates geometry and prevents saving of invalid polygons
- **SC-031**: Airport boundary polygons are defined for VIDP and YBBN with accurate perimeters

## Assumptions

- Java 17+ is used with Spring Boot 3.x framework
- Generators will be placed in `/backend/src/main/java/com/utam/simulation/` package
- PostGIS extension is available for spatial data (Point, Polygon types)
- Existing Mock*Generator classes provide patterns to follow
- Generators can use Spring's scheduling features for continuous mode
- Database schema and tables already exist from entity definitions
- Frontend will use a mapping library (e.g., Leaflet, MapLibre) for path drawing and zone editing
- Airport boundary data can be sourced from OpenStreetMap or similar public sources
- Admin screens will be React components in the existing frontend structure
- Delay cost calculations use industry standard: $100-150/minute for airlines, $500-$1000/minute for slot value
- Financial impact calculations are based on Deep Turnaround + TurnaroundControl methodology
- Stand coordinates and depot locations are defined for each airport

## Clarifications

### Session 2026-02-03

- Q: What is the data retention policy for generated simulation data? → A: 90 days rolling retention, configurable via application properties
- Q: Who can access admin tools (path drawing, zone editor)? → A: ADMIN role only
- Q: How are vehicles assigned to turnaround tasks? → A: Proximity-based auto-assignment (nearest available vehicle)
- Q: What observability is required for data generators? → A: Full observability (logs + Micrometer metrics + health endpoint)
- Q: How do users undo mistakes in zone/path editors? → A: Undo/redo with Ctrl+Z/Ctrl+Y keyboard shortcuts

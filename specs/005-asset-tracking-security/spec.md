# Feature Specification: Asset Tracking & Security Module

**Feature ID**: 005  
**Feature Name**: Asset Tracking Security - Restricted Zones, Movement Trail & Discrepancy Detection  
**Priority**: HIGH  
**Created**: 2026-01-28  
**Status**: In Development

---

## Overview

This feature implements comprehensive security and tracking capabilities for ground support equipment (GSE) and assets across airport operations. It provides real-time monitoring of asset movements, automated detection of restricted zone violations, and identification of movement discrepancies to enhance operational safety and compliance.

---

## Business Context

### Problem Statement

Airport operations face critical challenges:
1. **Security Risks**: Assets entering restricted areas (runways, fuel zones, cargo security areas) without authorization
2. **Asset Mismanagement**: Lost or misplaced equipment leading to operational delays
3. **Compliance Gaps**: No auditable trail of asset movements for regulatory compliance
4. **Operational Inefficiency**: Manual tracking of asset locations is error-prone and time-consuming

### Business Value

- **Safety**: Prevent unauthorized access to critical zones, reducing incident risk by 70%
- **Compliance**: Full audit trail for regulatory requirements (ICAO, TSA, local aviation authorities)
- **Efficiency**: Reduce asset search time from 15-30 minutes to under 1 minute
- **Cost Savings**: Prevent equipment damage/loss, estimated savings of $50K-100K annually per airport
- **Accountability**: Clear ownership and acknowledgment of violations

---

## User Stories

### US1: View Restricted Zone Violations Report

**As a** Ground Handling Manager  
**I want to** view all restricted zone violations in the past 24 hours  
**So that** I can take corrective action and maintain airport security compliance

**Acceptance Criteria:**
- [ ] Report displays all zone violations with asset name, zone, time, duration
- [ ] Violations are color-coded by severity (CRITICAL=red, HIGH=orange, MEDIUM=yellow, LOW=blue)
- [ ] Can filter by: date range, zone type, asset category, severity, acknowledged status
- [ ] Can sort by timestamp, duration, severity
- [ ] Each violation shows: asset details, zone entered, entry time, duration in zone, current status
- [ ] Can acknowledge violations with resolution notes
- [ ] Export to PDF/Excel
- [ ] Real-time updates when new violations occur

**Priority**: P0 (Critical)

---

### US2: View Movement Discrepancy Report

**As an** Asset Manager  
**I want to** see all movement discrepancies for my assets  
**So that** I can investigate tracking issues and maintain asset register accuracy

**Acceptance Criteria:**
- [ ] Report displays discrepancies: unexpected movement, location mismatch, speed anomaly, missing tracking
- [ ] Shows expected vs actual location with distance deviation in meters
- [ ] Can filter by: date range, discrepancy type, asset category, severity, acknowledged status
- [ ] Map view shows expected location (pin A) vs actual location (pin B) with line showing deviation
- [ ] Each discrepancy includes description of what triggered it
- [ ] Can acknowledge discrepancies with investigation notes
- [ ] Export to PDF/Excel
- [ ] Alerts when critical discrepancies detected (>500m deviation)

**Priority**: P0 (Critical)

---

### US3: View Asset Movement Trail

**As a** Security Officer  
**I want to** view the complete movement history of any asset  
**So that** I can investigate incidents and verify asset usage patterns

**Acceptance Criteria:**
- [ ] Select asset from dropdown or search by asset ID/name
- [ ] Timeline shows all positions for selected date range (default: last 24 hours)
- [ ] Map visualization shows movement path with color-coded segments:
  - Green: Normal zones
  - Orange: Controlled zones
  - Red: Restricted/Prohibited zones
- [ ] Timeline includes: timestamp, location, zone, speed, status
- [ ] Can play back movement with animation (1-60x speed)
- [ ] Highlights zone entries/exits with markers
- [ ] Shows dwell time in each zone
- [ ] Export trail data to CSV
- [ ] Filter trail by zone type or time range

**Priority**: P0 (Critical)

---

### US4: Configure Restricted Zones

**As a** System Administrator  
**I want to** define restricted zones with authorization rules  
**So that** the system automatically detects violations

**Acceptance Criteria:**
- [ ] Create zone with: name, type (PROHIBITED/RESTRICTED/CONTROLLED/MAINTENANCE), boundary polygon
- [ ] Define authorized asset categories for each zone
- [ ] Define specific authorized asset IDs (exceptions)
- [ ] Set zone as active/inactive
- [ ] Visual map editor to draw zone boundaries
- [ ] Preview which assets would be authorized/unauthorized
- [ ] Save zone configuration
- [ ] Audit log of zone configuration changes

**Priority**: P1 (High)

---

## Functional Requirements

### FR1: Restricted Zone Management

**FR1.1**: System shall support four zone types:
- **PROHIBITED**: No assets allowed (e.g., active runways)
- **RESTRICTED**: Only specific categories allowed (e.g., fuel zones → fuel trucks only)
- **CONTROLLED**: Authorized categories with logging (e.g., cargo security)
- **MAINTENANCE**: Maintenance vehicles only during specific hours

**FR1.2**: Zone authorization logic:
- If zone has authorized_asset_categories: Check asset.category IN authorized_asset_categories
- If zone has authorized_asset_ids: Check asset.id IN authorized_asset_ids
- If asset matches either rule: AUTHORIZED
- If PROHIBITED zone: ALWAYS unauthorized (overrides exceptions)
- If no rules defined: Default UNAUTHORIZED for RESTRICTED/PROHIBITED, AUTHORIZED for CONTROLLED/MAINTENANCE

**FR1.3**: Zone boundaries defined as PostGIS POLYGON with WGS84 coordinates

**FR1.4**: Zones are tenant-specific (multi-tenancy maintained)

---

### FR2: Movement Trail Capture

**FR2.1**: System shall capture asset positions every 5 seconds from vehicles table

**FR2.2**: Each trail entry includes:
- Asset identifier (linked to asset register)
- Latitude/longitude (PostGIS POINT)
- Speed, heading, altitude
- Current zone name
- Restricted zone ID (if in restricted zone)
- Status
- Timestamp

**FR2.3**: Trail data retained for 90 days, then archived to cold storage

**FR2.4**: Mapping logic:
- Match vehicles.vehicle_id to assets.qr_id (QR code identifier)
- If no match, create unlinked trail with vehicle_id only
- Log unmatched vehicles for manual resolution

**FR2.5**: Real-time ingestion with <10 second latency

---

### FR3: Zone Violation Detection

**FR3.1**: System shall detect violations in real-time when:
- Asset enters zone without authorization
- Asset category not in authorized_asset_categories
- Asset enters PROHIBITED zone

**FR3.2**: Violation severity assignment:
- **CRITICAL**: PROHIBITED zone entry
- **HIGH**: RESTRICTED zone with no authorization
- **MEDIUM**: CONTROLLED zone without authorization
- **LOW**: MAINTENANCE zone after hours

**FR3.3**: Duration calculation:
- Continuous time in zone from entry to exit
- If multiple entries: Track each separately
- Report total cumulative time + longest continuous stay

**FR3.4**: Violation record includes:
- Entry timestamp, entry location
- Duration in seconds
- Asset and zone details
- Severity level
- Acknowledged status

**FR3.5**: Violations persist indefinitely for audit compliance

---

### FR4: Movement Discrepancy Detection

**FR4.1**: System shall detect five discrepancy types:

**UNEXPECTED_MOVEMENT**: 
- Asset status = "Maintenance" or "Out of Service" but position changed
- Deviation > 50 meters within 1 hour

**LOCATION_MISMATCH**:
- Asset register location ≠ actual GPS location
- Tolerance: 100 meters

**SPEED_ANOMALY**:
- Speed exceeds category maximum:
  - Pushback: 15 km/h
  - Fuel truck: 25 km/h
  - Passenger bus: 30 km/h
  - Emergency: 50 km/h
  - Default: 20 km/h

**MISSING_TRACKING**:
- No position update for >10 minutes for asset with status "In Use"

**DUPLICATE_SIGNAL**:
- Same asset ID reported at 2+ locations simultaneously (>500m apart)

**FR4.2**: Discrepancy severity:
- **CRITICAL**: DUPLICATE_SIGNAL, SPEED_ANOMALY >50% over limit
- **HIGH**: LOCATION_MISMATCH >500m, MISSING_TRACKING >30 min
- **MEDIUM**: UNEXPECTED_MOVEMENT, LOCATION_MISMATCH 100-500m
- **LOW**: SPEED_ANOMALY <20% over limit

**FR4.3**: Distance calculation using Haversine formula

**FR4.4**: Discrepancies auto-resolve after 24 hours if condition normalizes

---

### FR5: Reporting & Visualization

**FR5.1**: Restricted Zone Report:
- Table view with columns: Asset, Zone, Entry Time, Duration, Severity, Status, Actions
- Filters: Date range, Zone type, Asset category, Severity, Acknowledged
- Sort: Any column
- Pagination: 50 rows per page
- Acknowledge action: Opens modal for resolution notes

**FR5.2**: Movement Discrepancy Report:
- Table view with columns: Asset, Type, Expected, Actual, Deviation, Severity, Status, Actions
- Map view: Dual markers with connecting line
- Filters: Date range, Discrepancy type, Asset category, Severity, Acknowledged
- Acknowledge action: Opens modal for investigation notes

**FR5.3**: Movement Trail:
- Asset selector: Dropdown + autocomplete search
- Date range selector (max: 30 days)
- Map with polyline showing path
- Color coding: Green (normal), Orange (controlled), Red (restricted)
- Timeline scrubber: Drag to specific time
- Playback controls: Play/Pause, Speed (1x, 5x, 10x, 30x, 60x)
- Information panel: Current position details, zone, speed, timestamp
- Zone markers: Flag icons at entry/exit points

**FR5.4**: Export functionality:
- Formats: PDF (formatted report), Excel (raw data), CSV (trail data)
- Include filters applied
- Timestamp in filename

---

### FR6: Real-time Notifications

**FR6.1**: WebSocket events broadcast to connected clients:
- `zone_violation`: When new violation detected
- `movement_discrepancy`: When new discrepancy detected
- `asset_trail_update`: Every 30 seconds for tracked assets

**FR6.2**: Event payload includes:
- Event type
- Tenant code
- Affected asset details
- Severity
- Timestamp

**FR6.3**: Browser notifications for CRITICAL severity only

**FR6.4**: Toast notifications on report pages for new items

---

### FR7: Authorization & Access Control

**FR7.1**: Role-based access:

| Role | View Reports | Acknowledge Violations | Configure Zones | Export Data |
|------|--------------|------------------------|-----------------|-------------|
| ADMIN | ✅ All tenants | ✅ | ✅ | ✅ |
| GH (Ground Handling) | ✅ Own tenant | ✅ | ❌ | ✅ |
| AIRPORT_USER | ✅ Own tenant | ❌ | ❌ | ❌ |

**FR7.2**: Tenant isolation enforced at database query level

**FR7.3**: Acknowledgment audit: Record user ID, timestamp, notes

---

## Non-Functional Requirements

### Performance
- Trail data ingestion: <10 second latency
- Violation detection: <5 seconds from zone entry
- Report loading: <3 seconds for 1000 rows
- Map rendering: <2 seconds for 24-hour trail
- WebSocket event delivery: <1 second

### Scalability
- Support 500+ assets per tenant
- Handle 10,000+ trail points per asset per day
- Store 90 days of trail data (~450M rows for 500 assets)
- Support 100 concurrent users viewing reports

### Availability
- 99.5% uptime during operational hours (04:00-02:00 local time)
- Graceful degradation if GPS data temporarily unavailable

### Security
- All API endpoints require authentication
- Tenant data isolation enforced
- Audit log all configuration changes
- Encrypt sensitive data at rest

---

## Data Retention

| Data Type | Retention | Archive Strategy |
|-----------|-----------|------------------|
| Movement Trail | 90 days hot | Archive to S3/cold storage, keep aggregates |
| Zone Violations | Indefinite | No archival (audit requirement) |
| Movement Discrepancies | 1 year | Soft delete after 1 year |
| Zone Configurations | Indefinite | Version history maintained |

---

## Success Criteria

1. **Violation Detection Accuracy**: >95% true positive rate (validated against manual review)
2. **User Adoption**: 80% of GH managers use reports weekly within 1 month
3. **Incident Reduction**: 50% reduction in unauthorized zone entries within 3 months
4. **Performance**: All reports load within SLA (<3 seconds)
5. **Data Quality**: <5% unmatched vehicle-asset mappings

---

## Dependencies

- **PostGIS**: Geographic queries for zone detection
- **TimescaleDB**: Efficient time-series storage for trail data
- **Existing Assets Module**: Asset register must be populated
- **Existing Vehicles Tracking**: GPS data from vehicles table
- **WebSocket Infrastructure**: Real-time event broadcasting

---

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| GPS accuracy issues | High false positives | Implement 50m buffer zone, require 2 consecutive readings |
| Vehicle-Asset mapping gaps | Incomplete tracking | Provide manual mapping UI, alert admins of unmapped vehicles |
| High data volume | Storage costs | Implement compression, archival after 90 days |
| Complex zone geometries | Performance degradation | Use spatial indexes, simplify polygons to <100 vertices |
| User fatigue from false alarms | Low adoption | Tunable severity thresholds, ML-based filtering in future |

---

## Out of Scope (Future Enhancements)

- ML-based anomaly detection for movement patterns
- Predictive alerts (asset likely to enter restricted zone)
- Mobile app for field acknowledgment
- Integration with CCTV for visual verification
- Automated corrective actions (e.g., send alert to driver)
- Historical trend analysis dashboard
- Integration with maintenance scheduling system

---

## Acceptance Testing Scenarios

### Scenario 1: Detect Runway Incursion
1. Asset "Fuel Truck-001" enters PROHIBITED zone "Runway 09/27"
2. Violation created with CRITICAL severity
3. Real-time alert sent to all GH managers
4. Violation appears in report within 5 seconds
5. Manager acknowledges with notes "Driver error, retraining scheduled"
6. Violation marked as acknowledged but remains in report

### Scenario 2: Movement Trail Investigation
1. Select "Pushback Tractor-002" from dropdown
2. Set date range: Yesterday 06:00 to 18:00
3. Map displays 12-hour movement path
4. Path shows 4 entries into restricted "Maintenance Hangar Area" (authorized)
5. Timeline shows 8 hours stationary at Terminal 2
6. Export trail to CSV for incident report

### Scenario 3: Location Mismatch Detection
1. Asset register shows "Belt Loader-001" at "Terminal 1"
2. GPS shows actual location at "Cargo Area" (800m away)
3. Discrepancy created: LOCATION_MISMATCH, HIGH severity, 800m deviation
4. Map view shows expected (Terminal 1) vs actual (Cargo Area)
5. Asset manager investigates, updates register location
6. Acknowledges discrepancy with note "Register outdated, corrected"

---

## Glossary

- **GSE**: Ground Support Equipment (pushback tractors, fuel trucks, etc.)
- **Zone Incursion**: Unauthorized entry into restricted zone
- **Trail**: Sequential record of asset positions over time
- **Discrepancy**: Deviation from expected asset behavior or location
- **Dwell Time**: Duration an asset remains stationary in a location
- **Haversine**: Formula for calculating distance between GPS coordinates
- **Hot Storage**: Database storage for frequently accessed data
- **Cold Storage**: Archive storage (S3/Glacier) for infrequently accessed data

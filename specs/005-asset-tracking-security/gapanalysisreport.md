# Gap Analysis Report — New Reports vs Existing TAM Report Infrastructure

**Date:** 2026-02-21  
**Feature:** 005-asset-tracking-security  
**Purpose:** Evaluate the requested new reports against the current TAM reporting stack to identify overlaps, partial coverage, and genuine gaps requiring new development.

---

## 1. Current Report Inventory

### 1.1 SQL Views (`09-superset-report-views.sql` — 20 views)

| # | View | Domain | Purpose |
|---|------|--------|---------|
| 1 | `v_ops_overview_daily` | Operations | Daily flights, vehicles, alerts, violations per tenant |
| 2 | `v_flight_movements_hourly` | Operations | Hourly flight position counts |
| 3 | `v_vehicle_activity_summary_daily` | Operations | Vehicle telemetry by type (points, avg speed) |
| 4 | `v_stand_gate_occupancy` | Turnaround | Stand session count + avg turnaround duration |
| 5 | `v_turnaround_sla_compliance` | Turnaround | Task-type SLA on-time ratio + avg delay |
| 6 | `v_delay_root_causes` | Turnaround | Pareto of delay minutes by task type |
| 7 | `v_speed_violations_by_zone` | Safety | Violation count by zone × severity |
| 8 | `v_restricted_zone_breach_dwell` | Safety | Avg/max dwell time in restricted zones |
| 9 | `v_discrepancy_trends_daily` | Safety | Daily movement discrepancy counts |
| 10 | `v_asset_utilization_status_counts` | Assets | Latest asset status distribution |
| 11 | `v_maintenance_downtime_by_type` | Assets | Assets in Maintenance/Out-of-Service |
| 12 | `v_dwell_proxy_by_zone_hourly` | Assets | Movement density per zone per hour |
| 13 | `v_alerts_summary_type_hour` | Safety | Hourly sensor alert counts by type |
| 14 | `v_repeat_offenders_assets` | Safety | Top assets by violation count (6 months) |
| 15 | `v_throughput_ops_volume_today` | Operations | Today's flights, tasks done, alerts closed |
| 16 | `v_pipeline_health_events_per_minute` | Pipeline | Movement trail ingestion rate |
| 17 | `v_activity_heatmap_latest` | Spatial | 24h activity heatmap (materialized view) |
| 18 | `v_violation_heatmap_latest` | Spatial | 7d violation heatmap (materialized view) |
| 19 | _(dwell heatmap placeholder)_ | Spatial | Deferred — uses dwell proxy |
| 20 | `v_stand_conflicts` | Turnaround | Overlapping turnaround sessions per stand |

### 1.2 Superset Dashboards & Charts (`create-all-reports.sh` — 6 dashboards, 28+ charts)

| Dashboard | Slug | Charts |
|-----------|------|--------|
| TAM Ops Overview | `tam_ops_full` | Ops Overview Daily, Flight Movements Hourly, Vehicle Activity Daily, Throughput Today, Flight Movements (Line) |
| TAM Safety & Security | `tam_safety` | Violations by Zone, Breach Dwell Stats, Discrepancy Trends, Repeat Offenders, Violation Heatmap (deck.gl), Alerts by Type (Line) |
| TAM Turnaround | `tam_turnaround` | Stand Occupancy, SLA Compliance by Task, Delay Root Causes, Stand Conflicts |
| TAM Assets | `tam_assets` | Asset Utilization Status, Maintenance Downtime, Dwell by Zone Hourly, Activity Heatmap (deck.gl) |
| TAM Pipeline | `tam_pipeline` | Pipeline Events/min |
| TAM Predictive | `tam_predictive` | Turnaround Risk, Congestion Forecast, Zone Breach Probability, Asset Violation Risk, Violations Forecast |

### 1.3 Grafana Dashboards (`AnalyticsHubPage.tsx` — 8 dashboards)

| Dashboard | Slug | Focus |
|-----------|------|-------|
| Executive Operations | `tam-exec-ops` | High-level KPIs |
| Flight Operations | `tam-flight-ops` | Real-time flight tracking |
| Turnaround Performance | `tam-turnaround-perf` | Turnaround activities |
| Ground Vehicle Tracking | `tam-vehicle-tracking` | Vehicle monitoring |
| Data Pipeline Health | `tam-pipeline-health` | Pipeline metrics |
| Infrastructure Metrics | `tam-infra-metrics` | System health |
| Anomaly Detection | `tam-anomaly-detection` | Outlier detection |
| **Stand Utilization** | `tam-stand-utilization` | **Stand occupancy & planning** |

### 1.4 Dedicated Frontend Report Pages

| Page | Route | Purpose |
|------|-------|---------|
| `RestrictedZoneReportPage.tsx` | `/tracking/violations` | Interactive zone violations report with filters, table, acknowledge flow, WebSocket live updates |
| `MovementDiscrepancyReportPage.tsx` | `/tracking/discrepancies` | Movement discrepancy report with table/map views, acknowledgement, real-time notifications |
| `MovementTrailPage.tsx` | `/tracking/trail` | Asset movement trail replay |
| `AnalyticsHubPage.tsx` | `/reports` | Unified analytics hub embedding all Superset + Grafana dashboards |

---

## 2. Requested New Reports

### Report A: GSE Maintenance Report
**Purpose:** Track health & availability of ground vehicle fleet.  
**Fields:** GHA / Service Provider, Vehicle Number / Type / Name, Maintenance Status (red flag for "Under-Maintenance").

### Report B: Stand Utilization Report (Heat-Map)
**Purpose:** 24-hour heat-map view of parking stand occupancy.  
**Fields:** Stand (with flight count), Hourly time slots (00:00–23:00), Occupancy value (0 = vacant, 1 = occupied, 2 = conflict/double-booked).

### Report C: Gate Utilization Report
**Purpose:** Monitor boarding gate usage.  
**Fields:** Gate selector (filterable), Timeline bar (0:00–23:00), Color-coded activity blocks.

### Report D: Maintenance Reports (Suite)
- **D1:** Predictive maintenance alerts — fault predictions from vibration/temperature trends
- **D2:** Utilization logs — engine hours, service intervals due, downtime analysis
- **D3:** Asset lifecycle reports — total usage, remaining life estimates

### Report E: Safety/Compliance Reports (Suite)
- **E1:** Incident reconstructions — collision/near-miss videos with telematics overlay (speed, path)
- **E2:** Driver behavior analytics — speeding, harsh maneuvers, fatigue patterns
- **E3:** Compliance logs — automated safety records for audits (Annex 19 SPIs)

### Report F: Operational Efficiency Reports (Suite)
- **F1:** Fleet utilization dashboards — idle time, allocation optimization, turnaround impact
- **F2:** Fuel/emissions tracking — consumption trends, sustainability metrics
- **F3:** Geofence breach summaries — unauthorized areas, ramp intrusions

---

## 3. Gap Analysis Matrix

| Requested Report | Overlap Level | Existing Coverage | Gap / What's Missing |
|-----------------|---------------|-------------------|---------------------|
| **A. GSE Maintenance** | 🟡 Partial | `v_maintenance_downtime_by_type` shows Maintenance/OOS asset counts; `v_asset_utilization_status_counts` shows status distribution | **Missing:** GHA/service-provider field, vehicle number/type detail view, red-flag maintenance indicator per vehicle. Schema needs `gha`/`service_provider` on vehicles/assets table. |
| **B. Stand Utilization Heat-Map** | 🟡 Partial | `v_stand_gate_occupancy` has session counts per stand; `v_stand_conflicts` detects overlaps; Grafana `tam-stand-utilization` dashboard exists | **Missing:** Hourly pivot/matrix view (stand × hour → 0/1/2). Data exists in `turnaround_sessions` but no SQL view produces the heat-map format. |
| **C. Gate Utilization** | 🔴 No Coverage | No gate-level data model exists. Stands (tarmac-side) ≠ Gates (terminal-side). | **Missing:** `gates` table, gate–flight association data, gate timeline generation. Requires new data model + ingestion pipeline. |
| **D1. Predictive Maintenance Alerts** | 🔴 No Coverage | Existing predictive models cover turnaround risk & violation risk, not maintenance faults. | **Missing:** Sensor telemetry (vibration, temperature, hydraulic pressure), fault prediction ML model, `pred_maintenance` table. |
| **D2. Utilization Logs** | 🔴 No Coverage | `v_vehicle_activity_summary_daily` tracks telemetry points/avg speed but not engine hours or service intervals. | **Missing:** Engine-hour telemetry, service schedule table, downtime tracking with reason codes. |
| **D3. Asset Lifecycle** | 🔴 No Coverage | Assets have basic status but no lifecycle metadata. | **Missing:** Acquisition date, expected lifespan, depreciation model, usage accumulator, remaining-life estimation logic. |
| **E1. Incident Reconstructions** | 🔴 No Coverage | No video or incident data model exists. | **Missing:** Incident/event table, video storage integration, telematics path replay, collision detection algorithm. |
| **E2. Driver Behavior** | 🔴 No Coverage | Vehicle telemetry has speed but no driver association or maneuver classification. | **Missing:** Driver–vehicle assignment, harsh-acceleration/braking detection, fatigue scoring, driver profile table. |
| **E3. Compliance Logs** | 🔴 No Coverage | Zone violations and discrepancies are tracked but not formatted as compliance/audit records. | **Missing:** Compliance record schema aligned to ICAO Annex 19 SPIs, audit trail generation, sign-off workflow. |
| **F1. Fleet Utilization** | 🟡 Partial | `v_asset_utilization_status_counts` + `v_vehicle_activity_summary_daily` provide basic fleet data. | **Missing:** Idle-time calculation, allocation optimization analytics, turnaround-impact correlation analysis. |
| **F2. Fuel/Emissions** | 🔴 No Coverage | No fuel or emissions data model exists. | **Missing:** Fuel consumption telemetry, emissions calculation model, fuel-type metadata, sustainability KPI definitions. |
| **F3. Geofence Breach Summaries** | 🟢 Well Covered | `v_speed_violations_by_zone`, `v_restricted_zone_breach_dwell`, `RestrictedZoneReportPage.tsx` (interactive, real-time, with acknowledge workflow) | **No gap** — fully functional today. |

---

## 4. Coverage Summary

```
┌──────────────────────────────────┐
│  🟢  Well Covered:     1 report  │  F3. Geofence Breach Summaries
│  🟡  Partially Covered: 3 reports│  A. GSE Maintenance, B. Stand Utilization, F1. Fleet Utilization
│  🔴  No Coverage:       8 reports│  C, D1, D2, D3, E1, E2, E3, F2
└──────────────────────────────────┘
```

---

## 5. Implementation Tiers

### Tier 1 — Buildable Now (existing data, schema enhancements only)

| Report | Effort | What to Build |
|--------|--------|---------------|
| **A. GSE Maintenance Report** | Medium | Add `gha`/`service_provider` to vehicles/assets, new SQL view `v_gse_maintenance_report`, new Superset chart |
| **B. Stand Utilization Heat-Map** | Medium | New SQL view `v_stand_utilization_hourly` pivoting `turnaround_sessions` into hourly matrix, new Superset chart |
| **F1. Fleet Utilization (enhanced)** | Medium | New SQL view `v_fleet_utilization_detail` computing idle time from movement trail gaps, new Superset chart |

### Tier 2 — Requires New Data Models (moderate schema + backend work)

| Report | Effort | Prerequisites |
|--------|--------|---------------|
| **C. Gate Utilization** | High | New `gates` table, gate–flight associations, gate timeline SQL view |
| **D2. Utilization Logs** | High | Engine-hour telemetry ingestion, service schedule table |
| **D3. Asset Lifecycle** | Medium | Lifecycle metadata columns on assets, depreciation/usage logic |
| **E3. Compliance Logs** | Medium | Compliance record schema, audit generation from existing violation data |

### Tier 3 — Requires Significant New Infrastructure (new pipelines, ML, integrations)

| Report | Effort | Prerequisites |
|--------|--------|---------------|
| **D1. Predictive Maintenance** | Very High | New sensor telemetry pipeline (vibration, temp, hydraulic), ML fault-prediction model |
| **E1. Incident Reconstructions** | Very High | Video storage, collision-detection, telematics path replay engine |
| **E2. Driver Behavior** | High | Driver–vehicle assignment model, maneuver-classification algorithm, fatigue scoring |
| **F2. Fuel/Emissions** | High | Fuel sensor telemetry, emissions calculation, sustainability reporting framework |

---

## 6. Data Model Gaps (New Tables Required)

For reports beyond Tier 1, the following new tables/columns would need to be designed:

```
┌─────────────────────────────┬──────────────────────────────────────────────┐
│ Table/Column                │ Reports Served                               │
├─────────────────────────────┼──────────────────────────────────────────────┤
│ vehicles.gha               │ A (GSE Maintenance)                          │
│ vehicles.maintenance_status │ A (GSE Maintenance)                          │
│ gates                       │ C (Gate Utilization)                         │
│ gate_flight_assignments     │ C (Gate Utilization)                         │
│ engine_telemetry            │ D1 (Predictive Maint), D2 (Utilization Logs) │
│ service_schedules           │ D2 (Utilization Logs)                        │
│ asset_lifecycle_metadata    │ D3 (Asset Lifecycle)                         │
│ incidents                   │ E1 (Incident Reconstruction)                 │
│ driver_profiles             │ E2 (Driver Behavior)                         │
│ driver_vehicle_assignments  │ E2 (Driver Behavior)                         │
│ compliance_records          │ E3 (Compliance Logs)                         │
│ fuel_consumption            │ F2 (Fuel/Emissions)                          │
│ emission_metrics            │ F2 (Fuel/Emissions)                          │
└─────────────────────────────┴──────────────────────────────────────────────┘
```

---

## 7. Recommendations

1. **Immediate (Tier 1):** Proceed with GSE Maintenance Report and Stand Utilization Heat-Map — these require the least effort and fill the most visible gaps.

2. **Short-term (Tier 2):** Design schemas for Gate Utilization and Compliance Logs — these are high-value for airport operations and can reuse existing data patterns.

3. **Medium-term (Tier 3):** Plan infrastructure for Driver Behavior and Fuel/Emissions — these require new telemetry pipelines but are critical for operational efficiency.

4. **Skip for now:** Incident Reconstruction and Predictive Maintenance — these require video infrastructure and specialized ML models that represent a separate major initiative.

5. **No action needed:** Geofence Breach Summaries (F3) — already fully covered by `RestrictedZoneReportPage.tsx`.

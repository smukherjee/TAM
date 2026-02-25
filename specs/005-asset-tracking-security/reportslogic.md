```markdown
# Reports Logic — Superset & Real-time Reports

Path: specs/005-asset-tracking-security/reportslogic.md

Purpose: detailed functional documentation and logic for each Superset report and real-time report, mapped to the relevant ICAO references noted in research.md and the gap analysis in gapanalysisreport.md.

---

## Summary
- Contains functional design for all Superset dashboard charts, SQL view logic, real-time WebSocket events, alerting rules, retention and performance notes.
- Each report maps to the ICAO documents referenced in `research.md` (notably ICAO Annex 19 — Safety Management and ICAO Doc 9137 — Airport Services Manual Part 9) and where applicable notes gaps from `gapanalysisreport.md`.

## Conventions used in this document
- `asset_positions` (hypertable): time-series of incoming position messages (vehicle_id, geom, speed, heading, status, recorded_at, tenant_code)
- `restricted_zones` (table): polygons for restricted areas (zone_id, name, geom, severity)
- `turnaround_sessions` (table): stand/gate occupancy sessions
- `zone_violations` (hypertable): generated violation events (violation_id, asset_id, zone_id, severity, start_ts, end_ts, tenant_code)
- `movement_discrepancies` (hypertable): discrepancy events
- `vehicles` (table): asset registry (id, asset_tag, type, gha, service_provider, category, max_speed)

If a view name or table is referenced that doesn't exist in the current schema, see `gapanalysisreport.md` for missing schema items and tier guidance.

---

## Report: TAM Ops Overview (Dashboard: `tam_ops_full`)

- Purpose: high-level KPIs for operations per tenant (flights, vehicles active, alerts, violations).
- Data sources / views:
  - `v_ops_overview_daily` (materialized): aggregates daily counts from `flights`, `asset_positions`, `zone_violations`, `alerts`.
- Core logic (SQL sketch):

  SELECT tenant_code,
         date_trunc('day', d)::date AS day,
         count(distinct flight_id) as flights,
         count(distinct asset_id) filter (where last_seen >= now()-interval '24 hours') as active_vehicles,
         sum(violations) as violations_count
  FROM generate_series(start_date, end_date, '1 day'::interval) d
  LEFT JOIN ... -- join continuous aggregates

- Superset chart types: KPI tiles, time series line for flights, stacked bar for alerts by type.
- Refresh: daily materialized refresh + nightly refresh job; real-time tile uses a lightweight REST endpoint `/api/ops/overview/live` returning last 5 minutes snapshot.
- Filters: `tenant_code`, `start_date`, `end_date`, `asset_category`.
- ICAO mapping: ICAO Doc 9137 (Airport Services) — operational performance metrics; Annex 19 (Safety Management) for safety KPI visibility.

---

## Report: TAM Safety & Security (Dashboard: `tam_safety`)

- Purpose: show violations, discrepancies, repeat offenders, and heatmaps to support security operations and compliance.
- Data sources / views:
  - `v_speed_violations_by_zone` — counts grouped by zone × severity
  - `v_restricted_zone_breach_dwell` — dwell distributions
  - `v_discrepancy_trends_daily` — daily counts of movement discrepancies
  - `v_repeat_offenders_assets` — top assets by violation count
  - `v_violation_heatmap_latest` — materialized (7d) for visualization

- Core SQL logic (speed violation example):

  CREATE VIEW v_speed_violations_by_zone AS
  SELECT z.zone_id, z.name AS zone_name, v.tenant_code,
         date_trunc('hour', a.recorded_at) AS hour,
         count(*) AS violation_count,
         avg(a.speed) AS avg_speed
  FROM asset_positions a
  JOIN restricted_zones z ON ST_DWithin(a.geom, z.geom, z.buffer_meters)
  JOIN vehicles vmeta ON a.asset_id = vmeta.id
  WHERE a.recorded_at >= now() - interval '30 days'
    AND a.speed > vmeta.max_speed * 1.2
  GROUP BY z.zone_id, z.name, v.tenant_code, hour;

- Violation creation pipeline: materialized or event-driven process produces rows in `zone_violations` using ST_DWithin(geom, geom, 50) and confirmation logic (2 consecutive readings within zone within 10s window) — see `ingestion` NiFi/Kafka → backend worker logic in `violations-worker`.
- Superset charts: bar/line combos, top-n tables, deck.gl hex/heatmap layers for `v_violation_heatmap_latest` (7d materialized view refreshed every 15 minutes).
- Real-time component: WebSocket topic `/topic/violations/{tenantCode}` emitting violation events (see Real-time section below).
- Alerts: severity mapping to ICAO Annex 19 SPIs. Events with severity CRITICAL must create compliance records and be retained per Annex 19 retention policy (90 days hot, archive after) as noted in `research.md`.
- ICAO mapping: Annex 19 — Safety Management (incident logging, SPIs), Doc 9137 — procedures for restricted area enforcement and security.

---

## Report: Stand Utilization & Heat-Map (Dashboard: `tam_turnaround`, View: `v_stand_gate_occupancy`, new view `v_stand_utilization_hourly`)

- Purpose: matrix heatmap (stand × hour) for occupancy, conflicts and usage trends.
- Data sources:
  - `turnaround_sessions` (flight_id, stand_id, start_ts, end_ts)
  - materialized pivot view `v_stand_utilization_hourly` that produces rows: (stand_id, hour_of_day, occupancy_state)

- Core logic (hourly pivot sketch):

  WITH hours AS (
    SELECT generate_series(date_trunc('day', now()) - interval '30 days', now(), '1 hour') AS bucket
  )
  SELECT s.stand_id,
         extract(hour from h.bucket) AS hour_of_day,
         CASE
           WHEN count(session_id) = 0 THEN 0
           WHEN count(session_id) = 1 THEN 1
           ELSE 2
         END AS occupancy_state
  FROM hours h
  CROSS JOIN stands s
  LEFT JOIN turnaround_sessions t ON t.stand_id = s.stand_id
    AND t.start_ts <= h.bucket + interval '1 hour' AND t.end_ts >= h.bucket
  GROUP BY s.stand_id, hour_of_day;

- Superset chart: pivot table -> heatmap (stand rows × hour columns). Support drill-through to `MovementTrailPage` for a given stand-hour cell.
- Refresh: materialized view refresh every 60 minutes; ad-hoc SQL for historical exports.
- ICAO mapping: Doc 9137 (Stand/gate handling efficiency and turnaround performance metrics).

---

## Report: Asset Utilization & Maintenance (Dashboard: `tam_assets`, Views: `v_asset_utilization_status_counts`, `v_maintenance_downtime_by_type`)

- Purpose: GSE maintenance, downtime, utilization percentages and predictive maintenance indicators (predictive is Tier 3 — not currently available).
- Data sources:
  - `vehicles` table (requires `gha`/`service_provider` fields per gap analysis)
  - `maintenance_events` (start_ts, end_ts, reason_code, asset_id)
  - `asset_movement_trail` hypertable for idle/active calculations

- Core logic (idle time example):

  SELECT v.id AS asset_id, v.asset_tag, sum(inactive_seconds) as idle_seconds
  FROM (
    SELECT asset_id,
           lead(recorded_at) OVER (PARTITION BY asset_id ORDER BY recorded_at) as next_ts,
           recorded_at,
           CASE WHEN speed < 0.5 THEN extract(epoch from lead(recorded_at) OVER (PARTITION BY asset_id ORDER BY recorded_at) - recorded_at) ELSE 0 END as inactive_seconds
    FROM asset_positions
    WHERE recorded_at >= now() - interval '30 days'
  ) t
  JOIN vehicles v ON v.id = t.asset_id
  GROUP BY v.id, v.asset_tag;

- Superset charts: bar charts for downtime by type, table for maintenance events, line for utilization % over time.
- Filters: `gha`, `service_provider`, `asset_type`, `start_date`, `end_date`.
- ICAO mapping: Doc 9137 — vehicle servicing/availability; Annex 19 indirectly for operational safety impact of maintenance lapses.

---

## Report: Movement Discrepancy (Page & Dashboard: `MovementDiscrepancyReportPage.tsx`, view `v_discrepancy_trends_daily`)

- Purpose: list and trend movement discrepancies (duplicate signals, location mismatch, missing tracking).
- Data sources:
  - `movement_discrepancies` hypertable (type, asset_id, details JSON, occurred_at, severity)

- Detection logic (server-side worker):
  - DUPLICATE_SIGNAL: detect two or more distinct position events with same asset_id and timestamps within 5s but locations > 500m apart.
  - LOCATION_MISMATCH: last known registry_location for asset vs current GPS distance > 100m.
  - MISSING_TRACKING: no position update for > 10 minutes when status = 'In Use'.

- Event enrichment: attach nearest `zone_id`, last_known_driver (if available), last_maintenance_event.
- Superset charts: table with filters, trend line, map pins for recent events.
- Real-time: WebSocket topic `/topic/discrepancies/{tenantCode}`.
- ICAO mapping: Annex 19 — data integrity and safety incident detection; Doc 9137 — operational records.

---

## Report: Heatmaps (Activity & Violation) — `v_activity_heatmap_latest`, `v_violation_heatmap_latest`

- Purpose: spatial aggregation for operational and safety hotspots.
- Aggregation logic:
  - Pre-aggregate into grid cells using `ST_SnapToGrid(asset_positions.geom, resolution)` where resolution ∈ {10,25,50,100} (meters converted to degrees as appropriate), then count unique assets or violation counts per cell per timeframe.
  - Maintain materialized view per resolution (10m materialized; runtime resample to coarser resolution using grouped aggregation).

- SQL sketch:

  CREATE MATERIALIZED VIEW mv_activity_10m AS
  SELECT ST_SnapToGrid(geom, grid_size) AS grid_geom,
         tenant_code, date_trunc('hour', recorded_at) AS hour_bucket,
         count(*) AS hits
  FROM asset_positions
  WHERE recorded_at >= now() - interval '24 hours'
  GROUP BY grid_geom, tenant_code, hour_bucket;

- Visualization: deck.gl hex/heatmap layers in Superset or frontend; allow time-slider (24h) and resolution selector.
- Performance: compress / tune materialized view refresh cadence (10m grid: 15-minute refresh; coarser: 60-minute refresh). Use TimescaleDB compression for storage.
- ICAO mapping: supports operational footprint analyses from Doc 9137 and safety hotspot identification for Annex 19 reporting.

---

## Real-time reports & alerts (websocket + REST contract)

This section defines the WebSocket topics, event payloads, throttling, and client behavior used across real-time pages: `RestrictedZoneReportPage.tsx`, `MovementDiscrepancyReportPage.tsx`, and live map overlays.

Topics:
- `/topic/assets/live/{tenantCode}` — asset position deltas (live feed)
- `/topic/violations/{tenantCode}` — zone violation events
- `/topic/discrepancies/{tenantCode}` — movement discrepancy events
- `/queue/acknowledge/{tenantCode}` — client → server acknowledgement actions (STOMP send)

Message formats (JSON):

- Asset position (published by ingestion worker, throttled):

  {
    "event_type": "asset.position",
    "asset_id": "uuid",
    "asset_tag": "GSE-123",
    "geom": {"type":"Point","coordinates":[lon,lat]},
    "speed": 12.2,
    "heading": 180,
    "status": "IN_USE",
    "recorded_at": "2026-02-21T12:34:56Z",
    "tenant_code": "VIDP"
  }

- Violation event:

  {
    "event_type": "zone.violation",
    "violation_id": "uuid",
    "asset_id": "uuid",
    "zone_id": "zone-123",
    "severity": "HIGH",
    "start_ts": "2026-02-21T12:33:50Z",
    "end_ts": null,
    "geom": {"type":"Point","coordinates":[lon,lat]},
    "details": {"confirmed_by": null, "confirm_count": 2},
    "tenant_code": "VIDP"
  }

- Discrepancy event:

  {
    "event_type": "discrepancy.detected",
    "discrepancy_id": "uuid",
    "type": "DUPLICATE_SIGNAL",
    "asset_id": "uuid",
    "details": {"distances_m": 620, "timestamps": ["...","..."]},
    "severity": "CRITICAL",
    "occurred_at": "2026-02-21T12:34:00Z",
    "tenant_code": "VIDP"
  }

Throttling & dedup:
- Server enforces max 1 asset.position message per asset per 5 seconds.
- Violation events are de-duplicated on `violation_id`; send updates when severity escalates or `end_ts` set.

Client behavior (frontend):
- Subscribe to tenant-specific topics after login and tenant selection.
- Map receives asset.position updates and updates marker position with interpolation for smoothness.
- Violation events show pinned alerts in UI with direct link to the `RestrictedZoneReportPage` detail table.
- Acknowledge flow: UI sends STOMP message to `/queue/acknowledge/{tenantCode}` with payload {violation_id, user_id, action: 'ack'|'dismiss', note}. Server records ack in `violation_acks` table and emits ack confirmation to `/topic/violations/{tenantCode}`.

Security & tenancy:
- Messages scoped by `tenant_code`. WebSocket subscription authorization validated on connect (JWT). Messages filtered server-side to ensure tenant isolation.

ICAO mapping (real-time): Annex 19 — incident detection, timely notification, and evidence capture for safety management.

---

## Superset: chart-level logic and user interactions

- All Superset charts must:
  - Accept `tenant_code`, `start_date`, `end_date` filters.
  - Use parameterized SQL in dataset for safe queries.
  - Configure caching TTL aligned with the materialized view refresh cadence; ex: 15 minutes for live safety charts, 60 minutes for heatmaps.

- Drill-throughs:
  - From violation tile → `RestrictedZoneReportPage` or `MovementDiscrepancyReportPage` depending on event type.
  - From heatmap cell → Movement trail replay for that grid cell/time-window.

---

## Retention, storage and archival rules

- Short-term (hot): 90 days for `asset_movement_trail`, `zone_violations`, `movement_discrepancies` (per `research.md` requirement; Annex 19 guidance).
- Compression: apply TimescaleDB compression after 7 days (see `research.md` SQL snippets).
- Long-term: S3 archive for >90 days; retention policy and restore process documented in infra runbooks.

---

## Alerts, severity mapping and SLA

- Severity mapping:
  - CRITICAL: immediate operator notification + create compliance record (Annex 19) + retain evidence (movement trail + associated camera video id if present)
  - HIGH: create ticket, notify duty manager
  - MEDIUM: create log entry, email digest
  - LOW: aggregated into daily digest

- SLA examples:
  - Critical violation: notify within 30 seconds via WebSocket & push notification
  - Acknowledge action persisted within 2 minutes of UI ack action

---

## Mapping requested reports (from gapanalysisreport.md) to ICAO references

- A. GSE Maintenance Report
  - ICAO: Doc 9137 (Airport Services Manual — ground support equipment servicing and availability). Annex 19: safety implications of maintenance state.

- B. Stand Utilization Heat-Map
  - ICAO: Doc 9137 (stand/gate operations, turnaround planning)

- C. Gate Utilization Report
  - ICAO: Doc 9137 (gate handling and scheduling). Note: gap analysis marks new `gates` model required.

- D1/D2/D3 Maintenance Suite
  - ICAO: Doc 9137 (maintenance & servicing). Predictive maintenance is beyond ICAO but supports Annex 19 reporting by reducing incidents.

- E1/E2/E3 Safety/Compliance Suite
  - ICAO: Annex 19 (Safety Management — SPI/incident reporting), Doc 9137 for operational procedures and incident handling.

- F1/F2/F3 Operational Efficiency & Sustainability
  - ICAO: Doc 9137 (operations). Emissions tracking references ICAO environmental guidance (ICAO Environmental Protection Annex 16 — note: not in research.md but relevant for emissions reporting; treat as future mapping request).

---

## Gaps & recommended implementation order (aligned with `gapanalysisreport.md`)

1. Tier 1 (implement first):
   - `v_gse_maintenance_report` (add `gha`/`service_provider` to `vehicles`), `v_stand_utilization_hourly`, `v_fleet_utilization_detail`.
2. Tier 2 (schema + backend):
   - `gates`, `gate_flight_assignments`, `service_schedules`, `compliance_records`.
3. Tier 3 (heavy infra):
   - Engine telemetry, predictive maintenance pipelines, video incident reconstruction.

---

## Appendix: example SQL snippets and worker pseudocode

- Violation confirmation (pseudocode worker):

  For each incoming position message for asset A:
    recent = fetch positions for asset A in last 15s
    if >=2 positions within zone Z (ST_DWithin with buffer 50m):
      if no active violation row for (asset A, Z):
         create zone_violation start_ts = first_confirm_time
      else:
         update end_ts when asset leaves zone (no positions in zone for 2 consecutive readings)

- Materialized view refresh cron examples (pgagent / cronjob):

  # refresh violation heatmap every 15 minutes
  REFRESH MATERIALIZED VIEW CONCURRENTLY mv_violation_heatmap_10m;

  # refresh activity 10m grid every 15 minutes
  REFRESH MATERIALIZED VIEW CONCURRENTLY mv_activity_10m;

---

## Next steps
- Implement Tier 1 views and schema changes noted in `gapanalysisreport.md`.
- Wire Superset datasets to new views and create dashboards per `tam_*` slugs.
- Implement WebSocket event contracts in backend `violations-worker` and `positions-ingest` services.

---

For implementation questions or to add precise ICAO clause citations, confirm which Annex/docs you want exact clause-level mapping to (Annex numbers and Doc numbers are referenced in `research.md`).

```

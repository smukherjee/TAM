# Research: Entity Data Generators

**Feature**: 006-entity-data-generators | **Date**: 2026-02-03

## Research Summary

This document consolidates all research findings for implementing comprehensive data generators for the TAM application.

---

## R1: Airport Boundary Data

### Decision
Use GeoJSON polygons for VIDP (Mumbai) and YBBN (Brisbane) airport perimeters stored as PostGIS geometry.

### Rationale
- PostGIS natively supports GeoJSON import/export
- Airport boundaries are publicly available from OpenStreetMap
- GeoJSON is the standard format for geospatial data exchange
- Matches existing zone storage pattern in the application

### Sources
- VIDP (Mumbai): OSM relation #3879817 - approximate boundary coordinates
- YBBN (Brisbane): OSM relation #5571893 - approximate boundary coordinates

### Data Structure
```json
{
  "type": "Feature",
  "properties": {
    "airport": "VIDP",
    "name": "Indira Gandhi International Airport"
  },
  "geometry": {
    "type": "Polygon",
    "coordinates": [[[77.0854, 28.5565], [77.1236, 28.5565], [77.1236, 28.5856], [77.0854, 28.5856], [77.0854, 28.5565]]]
  }
}
```

### Alternatives Considered
- **KML format**: Rejected - less common in modern web apps, PostGIS prefers GeoJSON
- **WKT (Well-Known Text)**: Rejected - less human-readable, harder to edit manually

---

## R2: Stand/Depot Coordinates

### Decision
Define realistic stand positions based on terminal layouts and GSE depot locations based on operational proximity.

### Rationale
- Stands must be positioned along terminal aprons
- Depots should be located near high-traffic areas but outside movement paths
- Existing airport diagrams from DGCA (India) and CASA (Australia) provide reference

### VIDP Stand Layout (48 stands)
| Terminal | Stand Range | Approximate Location |
|----------|-------------|---------------------|
| T1 Domestic | 1-12 | 77.0920°E, 28.5620°N |
| T2 Domestic | 13-24 | 77.1050°E, 28.5680°N |
| T3 International | 25-48 | 77.1000°E, 28.5750°N |

### YBBN Stand Layout (52 stands)
| Terminal | Stand Range | Approximate Location |
|----------|-------------|---------------------|
| Domestic | 1-26 | 153.1140°E, -27.3840°N |
| International | 27-52 | 153.1220°E, -27.3900°N |

### Depot Locations
- **Baggage Depot**: Near terminal buildings (primary carts, tractors)
- **Fuel Farm**: Perimeter location (fuel bowsers, tankers)
- **Maintenance Hangar**: Edge of airfield (tugs, ground power)
- **Catering Depot**: Near cargo area (catering trucks)
- **Cargo Depot**: Near cargo terminals (loaders, transporters)

### Alternatives Considered
- **Random generation**: Rejected - unrealistic, vehicles would spawn in invalid locations
- **Single depot**: Rejected - doesn't reflect real GSE distribution patterns

---

## R3: Turnaround Sub-Processes

### Decision
Implement 34 sub-processes based on Deep Turnaround methodology from SITA/ARINC specifications.

### Rationale
- Deep Turnaround provides industry-standard activity definitions
- Covers both narrow-body (25-45 min) and wide-body (75-120 min) turnarounds
- Each process has defined dependencies and can run in parallel/sequence

### Complete Process List (34 activities)

| # | Activity | Typical Duration | Dependencies |
|---|----------|------------------|--------------|
| 1 | Aircraft Arrival | 0 min | - |
| 2 | Chocks On | 1 min | 1 |
| 3 | Engine Shutdown | 2 min | 2 |
| 4 | Beacon Off | 0.5 min | 3 |
| 5 | Jetbridge Connect | 2 min | 4 |
| 6 | Door Open (L1) | 0.5 min | 5 |
| 7 | Passenger Deplane | 5-15 min | 6 |
| 8 | Crew Deplane | 1 min | 7 |
| 9 | Cabin Crew Change | 5 min | 8 |
| 10 | Ground Power Connect | 2 min | 4 |
| 11 | Air Start Disconnect | 1 min | 4 |
| 12 | Preconditioned Air Connect | 3 min | 4 |
| 13 | Cargo Door Open (Fwd) | 1 min | 4 |
| 14 | Cargo Door Open (Aft) | 1 min | 4 |
| 15 | Baggage Offload (Fwd) | 8-15 min | 13 |
| 16 | Baggage Offload (Aft) | 8-15 min | 14 |
| 17 | Cargo Offload | 10-20 min | 13, 14 |
| 18 | Cabin Cleaning | 10-25 min | 7 |
| 19 | Toilet Service | 5-10 min | 4 |
| 20 | Water Service | 5-8 min | 4 |
| 21 | Catering Offload | 5 min | 6 |
| 22 | Catering Upload | 10-20 min | 21 |
| 23 | Fuel Quantity Check | 2 min | 4 |
| 24 | Fueling Start | 1 min | 23 |
| 25 | Fueling Complete | 15-30 min | 24 |
| 26 | Baggage Load (Fwd) | 8-15 min | 15 |
| 27 | Baggage Load (Aft) | 8-15 min | 16 |
| 28 | Cargo Load | 10-20 min | 17 |
| 29 | Passenger Board | 10-25 min | 18, 22 |
| 30 | Cabin Door Close (L1) | 0.5 min | 29 |
| 31 | Cargo Door Close | 1 min | 26, 27, 28 |
| 32 | Jetbridge Disconnect | 2 min | 30 |
| 33 | Pushback Start | 3 min | 31, 32 |
| 34 | Pushback Complete | 3 min | 33 |

### Alternatives Considered
- **Simplified 10-process model**: Rejected - insufficient for demonstrating optimization value
- **60+ detailed model**: Rejected - over-engineering for simulation purposes

---

## R4: GSE Fleet Quantities

### Decision
Implement 15 GSE vehicle types with realistic fleet sizes based on typical airport operations.

### Rationale
- Fleet sizes based on IATA Ground Handling Manual recommendations
- Quantities scaled for medium-large airports (VIDP class)
- Allows demonstration of resource contention and optimization

### Fleet Definition

| # | Vehicle Type | Code | Quantity | Speed (km/h) |
|---|--------------|------|----------|--------------|
| 1 | Baggage Tractor | BGT | 25 | 15-25 |
| 2 | Baggage Cart | BGC | 80 | 15-25 |
| 3 | Belt Loader | BLT | 15 | 10-15 |
| 4 | Pushback Tractor | PBT | 12 | 5-15 |
| 5 | Fuel Bowser | FBW | 10 | 20-30 |
| 6 | Fuel Tanker | FTK | 6 | 20-30 |
| 7 | Catering Truck | CAT | 12 | 15-25 |
| 8 | Water Service | WTR | 8 | 15-25 |
| 9 | Toilet Service | LAV | 8 | 15-25 |
| 10 | Ground Power Unit | GPU | 10 | 5-10 |
| 11 | Air Start Unit | ASU | 4 | 5-10 |
| 12 | Preconditioned Air | PCA | 6 | 5-10 |
| 13 | Passenger Steps | PST | 8 | 10-15 |
| 14 | Container Loader | ULD | 8 | 10-15 |
| 15 | Deicing Truck | DIC | 4 | 15-25 |

**Total Fleet**: 216 vehicles

### Alternatives Considered
- **Smaller fleet (50 vehicles)**: Rejected - insufficient for realistic operations
- **Larger fleet (500 vehicles)**: Rejected - excessive for demonstration purposes

---

## R5: Alert Financial Rates

### Decision
Use $100-150/min delay cost with escalating slot values based on Deep Turnaround research.

### Rationale
- $100-150/min represents industry consensus for delay costs
- Slot values escalate during peak hours (2-3x base rate)
- Cascade multipliers for network impact on hub operations

### Financial Parameters

| Parameter | Value | Source |
|-----------|-------|--------|
| Base delay cost | $100/min | SITA Turnaround research |
| Peak delay cost | $150/min | Peak hour adjustment |
| Off-peak slot value | $500/slot | Base booking cost |
| Peak slot value | $1,500/slot | 3x peak multiplier |
| Cascade multiplier | 1.5x per connected flight | Hub network effect |
| Annual savings target | $11M | 50-airport deployment |

### Alert Financial Impact by Type

| Alert Type | Impact Calculation |
|------------|-------------------|
| Turnaround Delay | delay_minutes × $100-150 |
| Vehicle Delay | task_delay × $75 |
| Missed Task | standard_duration × $100 + cascade_impact |
| Zone Violation | $500 fixed + investigation_time × $50 |
| Equipment Failure | replacement_time × $100 + cascade_impact |
| Capacity Breach | overflow_count × $200 |

### Alternatives Considered
- **Lower rates ($50/min)**: Rejected - undervalues optimization impact
- **Higher rates ($300/min)**: Rejected - too aggressive for typical carrier costs

---

## R6: Leaflet Drawing Plugins

### Decision
Use **leaflet-geoman** for polygon/path drawing in both path editor and zone editor.

### Rationale
- leaflet-geoman is actively maintained (2024 releases)
- Supports polygons, polylines, rectangles, circles
- Built-in snap-to-vertex and grid features
- TypeScript definitions available
- Better touch/mobile support than leaflet-draw

### Feature Comparison

| Feature | leaflet-geoman | leaflet-draw |
|---------|----------------|--------------|
| Last release | 2024 | 2022 |
| TypeScript | ✅ Native | ⚠️ DefinitelyTyped |
| Touch support | ✅ Full | ⚠️ Limited |
| Snapping | ✅ Advanced | ⚠️ Basic |
| Hole support | ✅ Yes | ⚠️ Limited |
| Bundle size | 85KB | 120KB |
| React wrapper | @geoman-io/leaflet-geoman-free | react-leaflet-draw |

### Installation
```bash
npm install @geoman-io/leaflet-geoman-free
```

### Usage Pattern
```typescript
import "@geoman-io/leaflet-geoman-free";
import "@geoman-io/leaflet-geoman-free/dist/leaflet-geoman.css";

// Enable drawing controls
map.pm.addControls({
  position: 'topleft',
  drawPolygon: true,
  drawPolyline: true,
  editMode: true,
  dragMode: true,
  cutPolygon: true,
  removalMode: true,
});

// Handle draw events
map.on('pm:create', (e) => {
  const layer = e.layer;
  const geoJson = layer.toGeoJSON();
  // Save to API
});
```

### Alternatives Considered
- **leaflet-draw**: Rejected - maintenance concerns, less active development
- **mapbox-gl-draw**: Rejected - requires MapBox GL, different tech stack
- **Custom implementation**: Rejected - significant development effort

---

## Summary of Decisions

| Research Area | Decision | Key Factor |
|---------------|----------|------------|
| R1: Boundaries | GeoJSON + PostGIS | Standard format, native support |
| R2: Stands/Depots | Terminal-based layout | Operational realism |
| R3: Sub-processes | 34 Deep Turnaround activities | Industry standard |
| R4: GSE Fleet | 15 types, 216 vehicles | Demonstration scale |
| R5: Financials | $100-150/min, slot escalation | Industry research |
| R6: Drawing | leaflet-geoman | Active maintenance, TypeScript |

All NEEDS CLARIFICATION items from Technical Context have been resolved through this research.

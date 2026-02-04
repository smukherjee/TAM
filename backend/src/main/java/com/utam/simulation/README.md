# Data Generators Module

**Location**: `backend/src/main/java/com/utam/simulation/`

This module contains comprehensive data generators for populating the TAM (Turnaround Asset Management) application with realistic simulation data.

## Overview

The data generators create realistic airport simulation data for:
- **24 entity types** across flights, vehicles, assets, turnarounds, alerts, and security
- **2 tenants** (VIDP - Delhi, YBBN - Brisbane)
- **15 GSE vehicle types** with realistic fleet sizes
- **Real-time position updates** at 500ms intervals
- **Historical data backfill** for analytics

## Module Structure

```
simulation/
├── config/
│   ├── SimulationConfig.java       # Central configuration
│   └── SimulationMetrics.java      # Prometheus metrics
├── core/
│   ├── BaseDataGenerator.java      # Abstract base class
│   └── GeneratorOrchestrator.java  # Coordinates all generators
├── flight/
│   ├── FlightDataGenerator.java    # Arrivals/departures
│   └── FlightTrajectoryCalculator.java
├── vehicle/
│   ├── VehicleDataGenerator.java   # GSE fleet
│   ├── VehiclePathPlayer.java      # Path following
│   ├── VehicleAssignmentGenerator.java
│   ├── VehicleRouteCalculator.java
│   └── ProximityAssignmentService.java
├── asset/
│   └── AssetDataGenerator.java     # Equipment/tools
├── turnaround/
│   ├── TurnaroundDataGenerator.java # Sessions/tasks
│   ├── TurnaroundMilestoneGenerator.java
│   └── TurnaroundCorrelator.java
├── alert/
│   ├── AlertDataGenerator.java     # 12 alert types
│   ├── AlertEnricher.java
│   ├── CascadeAnalyzer.java
│   ├── FinancialImpactCalculator.java
│   └── FinancialMetricGenerator.java
├── security/
│   ├── ZoneDataGenerator.java      # Restricted zones
│   ├── ViolationDataGenerator.java
│   ├── MovementDiscrepancyGenerator.java
│   └── BoundaryValidator.java
├── tracking/
│   ├── MovementTrailGenerator.java # Position trails
│   └── LocationRegisterUpdater.java # Real-time register
├── reference/
│   ├── StandDataGenerator.java
│   ├── DepotDataGenerator.java
│   ├── VehicleTypeDataGenerator.java
│   └── BoundaryDataGenerator.java
├── financial/
│   └── FinancialDataGenerator.java
├── dispatch/
│   └── DispatchDataGenerator.java
└── retention/
    └── DataRetentionService.java   # 90-day cleanup
```

## Quick Start

### 1. Start Batch Population

Populate the database with initial data:

```bash
curl -X POST http://localhost:8080/api/admin/generators/batch/start \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"tenantCodes": ["VIDP", "YBBN"]}'
```

### 2. Enable Continuous Simulation

Start real-time position updates:

```bash
# Start all generators
curl -X POST http://localhost:8080/api/admin/generators/continuous/start \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 3. Check Status

```bash
# Generator status
curl http://localhost:8080/api/admin/generators/status \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Health check
curl http://localhost:8080/actuator/health/simulation

# Prometheus metrics
curl http://localhost:8080/actuator/prometheus | grep simulation
```

## Configuration

Key settings in `application.yml`:

```yaml
simulation:
  enabled: true
  retention-days: 90
  position-update-interval-ms: 500
  trail-retention-minutes: 5
  max-trail-points-per-entity: 600
  
  tenants:
    VIDP:
      center: [28.5665, 77.1031]
      timezone: Asia/Kolkata
      fleet-size: 150
    YBBN:
      center: [-27.3942, 153.1218]
      timezone: Australia/Brisbane
      fleet-size: 100
```

## Entity Types (24 Total)

### Reference Data
1. Stand - Aircraft parking positions
2. Depot - Vehicle storage areas
3. VehicleType - GSE type definitions
4. AirportBoundary - Perimeter polygons

### Core Entities
5. Flight - Arrivals and departures
6. Vehicle - GSE fleet vehicles
7. Asset - Equipment and tools
8. TurnaroundSession - Ground handling sessions
9. TurnaroundTask - Individual operations
10. TurnaroundMilestone - Key timing events

### Tracking
11. VehiclePosition - Time-series positions
12. FlightPosition - Flight trajectories
13. MovementTrail - Rolling position history
14. LocationRegister - Live positions

### Operations
15. VehicleAssignment - Vehicle-to-flight links
16. VehiclePath - Defined movement routes
17. Dispatch - Work orders

### Alerts & Financial
18. Alert - Predictive warnings
19. FinancialMetric - Cost/savings data

### Security
20. RestrictedZone - GeoJSON polygons
21. Violation - Zone breaches
22. MovementDiscrepancy - Anomalies

### Reports
23. TrailStatistics - Trail metrics
24. RegisterStatistics - Register metrics

## Metrics Exposed

All metrics are available at `/actuator/prometheus`:

```
simulation.records.total{generator="FlightDataGenerator",entity_type="flight"}
simulation.generators.active
simulation.generators.status{generator="VehicleDataGenerator"}
simulation.errors.total{generator="AlertDataGenerator"}
```

## API Endpoints

All endpoints require `ADMIN` role.

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/admin/generators/status` | GET | Get all generator statuses |
| `/api/admin/generators/batch/start` | POST | Start batch population |
| `/api/admin/generators/continuous/start` | POST | Start continuous mode |
| `/api/admin/generators/continuous/stop` | POST | Stop continuous mode |
| `/api/admin/generators/{type}/start` | POST | Start specific generator |
| `/api/admin/generators/{type}/stop` | POST | Stop specific generator |
| `/api/admin/generators/historical` | POST | Generate historical data |
| `/api/admin/generators/trails/stats` | GET | Get trail statistics |
| `/api/admin/generators/register/stats` | GET | Get register statistics |

## Frontend Admin UI

Access the admin dashboard at `/admin/generators`:

- **Overview Tab**: Generator status, quick actions
- **Controls Tab**: Per-generator start/stop, configuration
- **Metrics Tab**: Real-time throughput and performance
- **Security Tab**: ADMIN role status, audit logs

## Architecture

### Generation Modes

1. **Batch Mode**: One-time population with configurable sizes
2. **Continuous Mode**: Real-time updates at 500ms intervals
3. **Historical Mode**: Backfill data for analytics (7-day lookback)

### Data Flow

```
GeneratorOrchestrator
    ├── Batch Population
    │   ├── Reference generators (stands, depots, etc.)
    │   ├── Core generators (flights, vehicles, etc.)
    │   └── Correlation generators (assignments, alerts)
    │
    └── Continuous Updates (@Scheduled 500ms)
        ├── VehicleDataGenerator.generatePositionUpdate()
        ├── FlightDataGenerator.generatePositionUpdate()
        ├── MovementTrailGenerator.captureTrails()
        └── LocationRegisterUpdater.updateRegister()
```

### Security

- All admin endpoints require `ADMIN` role
- Boundary validation prevents out-of-bounds positions
- Zone violation detection for security monitoring
- Audit logging for all admin actions

## Testing

Run generator tests:

```bash
./mvnw test -Dtest="*Generator*"
```

## Troubleshooting

### No data appearing?
1. Check generator status: `GET /api/admin/generators/status`
2. Verify tenant exists in configuration
3. Check logs for errors: `docker logs tam-backend`

### Performance issues?
1. Reduce position update interval
2. Decrease trail retention period
3. Check database connection pool

### Out-of-bounds positions?
1. Verify airport boundary data is seeded
2. Check BoundaryValidator is enabled
3. Review boundary polygon coordinates

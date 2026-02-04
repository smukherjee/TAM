# Quickstart: Entity Data Generators

**Feature**: 006-entity-data-generators | **Date**: 2026-02-03

## Prerequisites

- Docker and Docker Compose installed
- TAM infrastructure running (PostgreSQL, Kafka, etc.)
- Admin user credentials

## Quick Setup

### 1. Start Infrastructure

```bash
cd /Users/sujoymukherjee/code/TAM
docker-compose up -d
```

Wait for all services to be healthy:

```bash
docker-compose ps
# All services should show "healthy" status
```

### 2. Run Initial Data Population

Populate the database with initial realistic data:

```bash
# Start batch population (creates ~1000 records in 2 minutes)
curl -X POST http://localhost:8080/api/admin/generators/batch/start \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantCodes": ["VIDP", "YBBN"],
    "populationSize": {
      "flights": 100,
      "vehicles": 150,
      "turnarounds": 20,
      "assets": 50
    }
  }'
```

Check batch status:

```bash
curl http://localhost:8080/api/admin/generators/batch/batch-$(date +%Y-%m-%d)-001 \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 3. Enable Continuous Simulation

Start real-time data generation:

```bash
# Start all generators
curl -X POST http://localhost:8080/api/admin/generators/ALL/start \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Or start individual generators:

```bash
# Start specific generators
curl -X POST http://localhost:8080/api/admin/generators/FLIGHT/start \
  -H "Authorization: Bearer $ADMIN_TOKEN"

curl -X POST http://localhost:8080/api/admin/generators/VEHICLE/start \
  -H "Authorization: Bearer $ADMIN_TOKEN"

curl -X POST http://localhost:8080/api/admin/generators/TURNAROUND/start \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 4. Access Admin Tools

Open the admin interface in your browser:

```
http://localhost:3000/admin
```

Login with admin credentials and access:

- **Generator Dashboard**: Monitor generator status, start/stop controls
- **Path Editor**: Draw and manage vehicle paths
- **Zone Editor**: Create and import restricted zones

### 5. Monitor Metrics

Check simulation health:

```bash
curl http://localhost:8080/actuator/health/simulation
```

View Prometheus metrics:

```bash
curl http://localhost:8080/actuator/prometheus | grep simulator
```

Expected metrics:
```
simulator_records_generated_total{type="flight"} 1250
simulator_records_generated_total{type="vehicle"} 45000
simulator_generation_rate{type="flight"} 5.0
```

## Verify Data Generation

### Check Generated Flights

```sql
SELECT COUNT(*), DATE(scheduled_time) as date
FROM flights
WHERE tenant_code IN ('VIDP', 'YBBN')
GROUP BY DATE(scheduled_time)
ORDER BY date DESC
LIMIT 7;
```

### Check Generated Vehicles

```sql
SELECT vt.name as vehicle_type, COUNT(*) as count
FROM vehicles v
JOIN vehicle_types vt ON v.vehicle_type_code = vt.code
WHERE v.tenant_code = 'VIDP'
GROUP BY vt.name
ORDER BY count DESC;
```

### Check Active Turnarounds

```sql
SELECT 
  ts.status,
  COUNT(*) as count,
  AVG(EXTRACT(EPOCH FROM (ts.end_time - ts.start_time))/60) as avg_duration_min
FROM turnaround_sessions ts
WHERE ts.tenant_code = 'VIDP'
  AND ts.start_time > NOW() - INTERVAL '24 hours'
GROUP BY ts.status;
```

### Check Alert Distribution

```sql
SELECT 
  alert_type,
  COUNT(*) as count,
  AVG(financial_impact) as avg_impact
FROM alerts
WHERE tenant_code = 'VIDP'
  AND created_at > NOW() - INTERVAL '24 hours'
GROUP BY alert_type
ORDER BY count DESC;
```

## Admin Tool Usage

### Create a Vehicle Path

1. Navigate to **Admin > Path Editor**
2. Select tenant (VIDP or YBBN)
3. Select vehicle type (e.g., "Baggage Tractor")
4. Click "Draw Path" and click on map to add waypoints
5. Use **Ctrl+Z** to undo, **Ctrl+Y** to redo
6. Configure schedule (start time, end time, interval)
7. Click "Save Path"
8. Use "Preview" to see animated vehicle movement

### Create a Restricted Zone

1. Navigate to **Admin > Zone Editor**
2. Select tenant
3. Click "Draw Polygon" tool
4. Click on map to create vertices
5. Double-click to complete polygon
6. Enter zone properties (name, type, max speed)
7. Click "Save Zone"

### Import GeoJSON Zones

1. Navigate to **Admin > Zone Editor**
2. Click "Import GeoJSON"
3. Select .geojson file
4. Choose zone type for imported features
5. Click "Import"
6. Review imported zones on map

### Export Zones as GeoJSON

1. Navigate to **Admin > Zone Editor**
2. Click "Export GeoJSON"
3. File downloads with all zones for current tenant

## Troubleshooting

### Generator Not Starting

Check logs:
```bash
docker logs tam-backend | grep -i generator
```

Common issues:
- Database connection failed → Check PostgreSQL is running
- Kafka unavailable → Check Kafka container status
- Configuration missing → Verify `application.yml` has simulation settings

### No Data Appearing

1. Verify generators are running:
```bash
curl http://localhost:8080/api/admin/generators
```

2. Check for errors:
```bash
curl http://localhost:8080/actuator/health/simulation
```

3. Check database connectivity:
```bash
docker exec -it tam-postgres psql -U utam -d utam -c "SELECT 1"
```

### Path Editor Not Saving

1. Verify ADMIN role:
```bash
curl http://localhost:8080/api/admin/paths \
  -H "Authorization: Bearer $ADMIN_TOKEN"
# Should return 200, not 403
```

2. Check browser console for JavaScript errors

3. Verify GeoJSON is valid (minimum 2 waypoints)

## Configuration

Key configuration in `application.yml`:

```yaml
simulation:
  enabled: true
  retention:
    days: 90
    cleanup-cron: "0 0 3 * * *"  # 3 AM daily
  rates:
    flight-updates-per-sec: 5
    vehicle-updates-per-sec: 5
    alert-generation-interval: 60  # seconds
  initial-population:
    flights: 100
    vehicles: 150
    turnarounds: 20
    assets: 50
  tenants:
    - VIDP
    - YBBN
```

## Next Steps

1. **Customize reference data**: Edit stand/depot positions in `data/vidp.json`
2. **Add custom paths**: Create frequently-used vehicle routes
3. **Define zone rules**: Import your operational zone definitions
4. **Monitor performance**: Set up Grafana dashboard with simulator metrics
5. **Scale testing**: Increase population sizes for load testing

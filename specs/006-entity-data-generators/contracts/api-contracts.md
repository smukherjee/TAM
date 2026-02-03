# API Contracts: Entity Data Generators

**Feature**: 006-entity-data-generators | **Date**: 2026-02-03

## Base URL

```
/api/admin
```

All endpoints require authentication with ADMIN role.

---

## Path Editor API

### List Vehicle Paths

```http
GET /paths?tenantCode={tenantCode}&vehicleType={vehicleType}&active={active}
```

**Query Parameters**:
| Name | Type | Required | Description |
|------|------|----------|-------------|
| tenantCode | string | Yes | Airport code (VIDP, YBBN) |
| vehicleType | string | No | Filter by vehicle type code |
| active | boolean | No | Filter by active status |

**Response**: `200 OK`
```json
{
  "paths": [
    {
      "id": 1,
      "tenantCode": "VIDP",
      "name": "T3 Baggage Route",
      "description": "Main baggage route from depot to T3 stands",
      "vehicleType": {
        "code": "BGT",
        "name": "Baggage Tractor"
      },
      "waypointsGeoJson": {
        "type": "LineString",
        "coordinates": [[77.1000, 28.5700], [77.1020, 28.5720], [77.1050, 28.5750]]
      },
      "schedule": {
        "startTime": "06:00",
        "endTime": "23:00",
        "intervalSeconds": 300
      },
      "active": true,
      "loop": true,
      "createdAt": "2026-02-01T10:00:00Z",
      "createdBy": "admin@utam.com"
    }
  ],
  "total": 15
}
```

---

### Create Vehicle Path

```http
POST /paths
Content-Type: application/json
```

**Request Body**:
```json
{
  "tenantCode": "VIDP",
  "name": "T3 Baggage Route",
  "description": "Main baggage route from depot to T3 stands",
  "vehicleTypeCode": "BGT",
  "waypointsGeoJson": {
    "type": "LineString",
    "coordinates": [[77.1000, 28.5700], [77.1020, 28.5720], [77.1050, 28.5750]]
  },
  "schedule": {
    "startTime": "06:00",
    "endTime": "23:00",
    "intervalSeconds": 300
  },
  "active": true,
  "loop": true
}
```

**Response**: `201 Created`
```json
{
  "id": 1,
  "tenantCode": "VIDP",
  "name": "T3 Baggage Route",
  ...
}
```

**Errors**:
- `400 Bad Request`: Invalid GeoJSON or missing required fields
- `409 Conflict`: Path with same name exists for tenant

---

### Get Vehicle Path

```http
GET /paths/{id}
```

**Response**: `200 OK` (same structure as list item)

---

### Update Vehicle Path

```http
PUT /paths/{id}
Content-Type: application/json
```

**Request Body**: Same as create (all fields optional except id)

**Response**: `200 OK` (updated path)

---

### Delete Vehicle Path

```http
DELETE /paths/{id}
```

**Response**: `204 No Content`

---

### Preview Path Animation

```http
GET /paths/{id}/preview?speed={speed}
```

**Query Parameters**:
| Name | Type | Required | Description |
|------|------|----------|-------------|
| speed | number | No | Animation speed multiplier (default: 1.0) |

**Response**: `200 OK`
```json
{
  "pathId": 1,
  "frames": [
    {
      "timestamp": 0,
      "position": [77.1000, 28.5700],
      "heading": 45.0
    },
    {
      "timestamp": 1000,
      "position": [77.1005, 28.5705],
      "heading": 45.0
    }
  ],
  "totalDurationMs": 60000,
  "distanceMeters": 1500
}
```

---

## Zone Editor API

### List Restricted Zones

```http
GET /zones?tenantCode={tenantCode}&zoneType={zoneType}
```

**Response**: `200 OK`
```json
{
  "zones": [
    {
      "id": 1,
      "tenantCode": "VIDP",
      "name": "Runway 28L Safety Zone",
      "description": "Critical safety zone during runway operations",
      "zoneType": "SAFETY",
      "geometryGeoJson": {
        "type": "Polygon",
        "coordinates": [[[77.08, 28.56], [77.12, 28.56], [77.12, 28.58], [77.08, 28.58], [77.08, 28.56]]]
      },
      "properties": {
        "maxSpeed": 10,
        "requiredClearance": "AIRSIDE"
      },
      "active": true,
      "createdAt": "2026-02-01T10:00:00Z"
    }
  ],
  "total": 25
}
```

---

### Create Restricted Zone

```http
POST /zones
Content-Type: application/json
```

**Request Body**:
```json
{
  "tenantCode": "VIDP",
  "name": "Runway 28L Safety Zone",
  "description": "Critical safety zone during runway operations",
  "zoneType": "SAFETY",
  "geometryGeoJson": {
    "type": "Polygon",
    "coordinates": [[[77.08, 28.56], [77.12, 28.56], [77.12, 28.58], [77.08, 28.58], [77.08, 28.56]]]
  },
  "properties": {
    "maxSpeed": 10,
    "requiredClearance": "AIRSIDE"
  },
  "active": true
}
```

**Response**: `201 Created`

**Validation Rules**:
- Polygon must have minimum 3 vertices
- Polygon must be closed (first = last coordinate)
- Polygon must not self-intersect
- Total area must not exceed airport boundary

---

### Get Zone

```http
GET /zones/{id}
```

---

### Update Zone

```http
PUT /zones/{id}
```

---

### Delete Zone

```http
DELETE /zones/{id}
```

**Response**: `204 No Content`

---

### Import GeoJSON

```http
POST /zones/import
Content-Type: multipart/form-data
```

**Form Data**:
| Name | Type | Description |
|------|------|-------------|
| file | file | GeoJSON file (.json or .geojson) |
| tenantCode | string | Target airport code |
| zoneType | string | Default zone type for imported features |
| overwrite | boolean | Replace existing zones with same name |

**Response**: `200 OK`
```json
{
  "imported": 12,
  "skipped": 2,
  "errors": [
    {
      "featureIndex": 5,
      "message": "Invalid polygon: self-intersecting geometry"
    }
  ]
}
```

---

### Export GeoJSON

```http
GET /zones/export?tenantCode={tenantCode}&format={format}
```

**Query Parameters**:
| Name | Type | Required | Description |
|------|------|----------|-------------|
| tenantCode | string | Yes | Airport code |
| format | string | No | "geojson" (default) or "geojson-seq" |

**Response**: `200 OK`
```json
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "properties": {
        "id": 1,
        "name": "Runway 28L Safety Zone",
        "zoneType": "SAFETY"
      },
      "geometry": {
        "type": "Polygon",
        "coordinates": [[[77.08, 28.56], ...]]
      }
    }
  ]
}
```

---

## Generator Control API

### Get Generator Status

```http
GET /generators
```

**Response**: `200 OK`
```json
{
  "generators": [
    {
      "type": "FLIGHT",
      "status": "RUNNING",
      "lastRunAt": "2026-02-03T14:30:00Z",
      "recordsGenerated": 1250,
      "rate": "5/sec",
      "errors": 0
    },
    {
      "type": "VEHICLE",
      "status": "RUNNING",
      "lastRunAt": "2026-02-03T14:30:00Z",
      "recordsGenerated": 45000,
      "rate": "5/sec",
      "errors": 0
    },
    {
      "type": "TURNAROUND",
      "status": "STOPPED",
      "lastRunAt": "2026-02-03T14:00:00Z",
      "recordsGenerated": 500,
      "rate": "0/sec",
      "errors": 0
    }
  ],
  "overallStatus": "PARTIAL",
  "uptime": "2h 30m"
}
```

---

### Start Generator

```http
POST /generators/{type}/start
```

**Path Parameters**:
| Name | Type | Description |
|------|------|-------------|
| type | string | Generator type: FLIGHT, VEHICLE, TURNAROUND, ALERT, ZONE, ALL |

**Response**: `200 OK`
```json
{
  "type": "FLIGHT",
  "status": "RUNNING",
  "message": "Generator started successfully"
}
```

---

### Stop Generator

```http
POST /generators/{type}/stop
```

**Response**: `200 OK`
```json
{
  "type": "FLIGHT",
  "status": "STOPPED",
  "message": "Generator stopped"
}
```

---

### Batch Start (Initial Population)

```http
POST /generators/batch/start
Content-Type: application/json
```

**Request Body** (optional):
```json
{
  "tenantCodes": ["VIDP", "YBBN"],
  "populationSize": {
    "flights": 100,
    "vehicles": 150,
    "turnarounds": 20,
    "assets": 50
  }
}
```

**Response**: `202 Accepted`
```json
{
  "batchId": "batch-2026-02-03-001",
  "status": "IN_PROGRESS",
  "estimatedCompletionSeconds": 120
}
```

---

### Get Batch Status

```http
GET /generators/batch/{batchId}
```

**Response**: `200 OK`
```json
{
  "batchId": "batch-2026-02-03-001",
  "status": "COMPLETE",
  "startedAt": "2026-02-03T14:00:00Z",
  "completedAt": "2026-02-03T14:02:00Z",
  "results": {
    "flights": {"generated": 100, "errors": 0},
    "vehicles": {"generated": 150, "errors": 0},
    "turnarounds": {"generated": 20, "errors": 0},
    "assets": {"generated": 50, "errors": 0}
  }
}
```

---

## Health & Metrics

### Simulation Health Endpoint

```http
GET /actuator/health/simulation
```

**Response**: `200 OK`
```json
{
  "status": "UP",
  "details": {
    "flightGenerator": "UP",
    "vehicleGenerator": "UP",
    "turnaroundGenerator": "UP",
    "alertGenerator": "UP",
    "dataRetention": "UP",
    "lastCleanup": "2026-02-03T03:00:00Z",
    "recordsInSystem": 125000
  }
}
```

### Prometheus Metrics

```http
GET /actuator/prometheus
```

Exposed metrics:
```
# TYPE simulator_records_generated_total counter
simulator_records_generated_total{type="flight"} 1250
simulator_records_generated_total{type="vehicle"} 45000

# TYPE simulator_generation_rate gauge
simulator_generation_rate{type="flight"} 5.0
simulator_generation_rate{type="vehicle"} 5.0

# TYPE simulator_errors_total counter
simulator_errors_total{type="flight"} 0

# TYPE simulator_retention_cleanup_total counter
simulator_retention_cleanup_total{table="vehicle_assignments"} 15000
```

---

## Error Responses

All endpoints return standard error format:

```json
{
  "error": "VALIDATION_ERROR",
  "message": "Invalid polygon: geometry must have at least 3 vertices",
  "details": {
    "field": "geometryGeoJson",
    "value": "..."
  },
  "timestamp": "2026-02-03T14:30:00Z",
  "path": "/api/admin/zones"
}
```

**Error Codes**:
| Code | HTTP Status | Description |
|------|-------------|-------------|
| VALIDATION_ERROR | 400 | Invalid request data |
| UNAUTHORIZED | 401 | Missing or invalid auth token |
| FORBIDDEN | 403 | User lacks ADMIN role |
| NOT_FOUND | 404 | Resource not found |
| CONFLICT | 409 | Resource already exists |
| INTERNAL_ERROR | 500 | Server error |

# NiFi Flow Configuration Guide

This document describes how to set up NiFi flows for TAM data ingestion.

## Access NiFi

- **URL**: http://localhost:8091/nifi
- **Default Credentials**: admin / admin123456789

## Flow 1: ADSB Flight Data Ingestion

### Processors

```
[ListenHTTP] → [UpdateAttribute] → [PublishKafka]
    :8092           Add routing       flight-raw-json
    /adsb-ingest    attributes
```

### ListenHTTP Configuration
| Property | Value |
|----------|-------|
| Listening Port | 8092 |
| Base Path | /adsb-ingest |
| HTTP Method | POST |

### UpdateAttribute Configuration
| Property | Value |
|----------|-------|
| kafka.topic | flight-raw-json |
| content.type | application/json |

### PublishKafka Configuration
| Property | Value |
|----------|-------|
| Kafka Brokers | redpanda:9092 |
| Topic Name | ${kafka.topic} |
| Key Attribute | CallSign |
| Use Transactions | false |

---

## Flow 2: Vehicle Data Ingestion

### Processors

```
[ListenHTTP] → [UpdateAttribute] → [PublishKafka]
    :8093           Add routing       vehicle-raw-json
    /vehicle-ingest attributes
```

### ListenHTTP Configuration
| Property | Value |
|----------|-------|
| Listening Port | 8093 |
| Base Path | /vehicle-ingest |
| HTTP Method | POST |

### UpdateAttribute Configuration
| Property | Value |
|----------|-------|
| kafka.topic | vehicle-raw-json |
| content.type | application/json |

### PublishKafka Configuration
| Property | Value |
|----------|-------|
| Kafka Brokers | redpanda:9092 |
| Topic Name | ${kafka.topic} |
| Key Attribute | vehicle_no |

---

## Testing the Flows

### Send test flight data:
```bash
curl -X POST http://localhost:8092/adsb-ingest \
  -H "Content-Type: application/json" \
  -d '[{
    "LivePlotId": "test-123",
    "Time": 1734567890.123,
    "CallSign": "TEST1",
    "Lat": 28.5562,
    "Lon": 77.1000,
    "Speed": 450.5,
    "Heading": 90.0,
    "Altitude": 35000.0,
    "Status": "AIRBORNE"
  }]'
```

### Send test vehicle data:
```bash
curl -X POST http://localhost:8093/vehicle-ingest \
  -H "Content-Type: application/json" \
  -d '[{
    "vehicle_no": "TEST001",
    "vehicletype": "BUS",
    "latitude": "28.5562",
    "longitude": "77.1000",
    "speed": "45.5",
    "status": "RUNNING"
  }]'
```

### Verify in Redpanda Console:
1. Open http://localhost:8090
2. Check Topics → flight-raw-json
3. New messages should appear

---

## Notes

- NiFi container must be running: `docker-compose -f docker-compose.dev.yml up -d nifi`
- First login may take 1-2 minutes while NiFi initializes
- Flows can be exported/imported via Templates menu

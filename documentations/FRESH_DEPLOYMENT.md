# TAM Platform - Fresh Deployment Guide

This guide ensures that when you bring down Docker containers and start fresh, everything works seamlessly.

---

## 🏗️ What's Already Persisted

### ✅ Code & Configuration Files
All these are in your Git repository and will survive container restarts:

- **Database Init Scripts:** `infrastructure/db/init/*.sql`
  - Tables, indexes, seed data (tenants, users)
  - Automatically run on TimescaleDB first start
  
- **Backend Mock Generators:** `backend/src/main/java/com/utam/simulation/`
  - Auto-start when backend starts
  - Send data to NiFi HTTP endpoints
  
- **Application Config:** `backend/src/main/resources/application.yml`
  - NiFi URLs: `http://nifi:8092/adsb-ingest` (and 8093, 8094)
  - Kafka, MinIO, Redis connections
  
- **Docker Volumes:** Defined in `docker-compose.dev.yml`
  - `tam_timescaledb_data` - Database persistence
  - `tam_nifi_data`, `tam_nifi_conf` - NiFi flows
  - `tam_redpanda_data` - Kafka topics
  - `tam_minio_data` - Object storage
  - `tam_redis_data` - Cache

---

## 🚀 Fresh Start Procedure

### Step 1: Stop Everything
```bash
cd /Users/sujoymukherjee/code/TAM
docker-compose -f docker-compose.dev.yml down
```

### Step 2: (Optional) Clean Slate - Remove Volumes
**⚠️ WARNING: This deletes all data!**

Only do this if you want a completely fresh start:
```bash
docker volume rm tam_timescaledb_data tam_nifi_data tam_nifi_conf \
  tam_nifi_logs tam_nifi_provenance tam_redpanda_data \
  tam_minio_data tam_redis_data
```

### Step 3: Start All Services
```bash
docker-compose -f docker-compose.dev.yml up -d
```

### Step 4: Wait for Health Checks
```bash
# Watch status (wait for all to be "healthy")
watch -n 5 'docker-compose -f docker-compose.dev.yml ps'

# Or check once
docker-compose -f docker-compose.dev.yml ps
```

Expected status:
```
NAME                 STATUS
tam-backend-1        Up (healthy)
tam-minio-1          Up (healthy)
tam-nifi-1           Up (healthy)     # May take 60-90 seconds
tam-redpanda-1       Up (healthy)
tam-redis-1          Up (healthy)
tam-timescaledb-1    Up (healthy)
```

### Step 5: Setup NiFi Flows
**⚠️ REQUIRED if volumes were removed or first-time setup**

```bash
cd infrastructure/nifi
./setup-nifi.sh
```

This script will:
- Create ListenHTTP processors on ports 8092, 8093, 8094
- Create PublishKafka processors for topics
- Configure HttpContextMap controller service
- Connect all processors
- Start all flows

**Expected output:**
```
🔧 Starting TAM NiFi Setup...
✅ NiFi is ready
✅ Created HttpContextMap controller service
✅ Created ADSB Ingestion flow
✅ Created Vehicle Ingestion flow
✅ Created CV Event Ingestion flow
✅ All flows started successfully
```

### Step 6: Verify Deployment
```bash
cd /Users/sujoymukherjee/code/TAM
./verify-deployment.sh
```

This will check:
- ✅ All services running
- ✅ Ports accessible
- ✅ NiFi ListenHTTP ports (8092-8094) bound
- ✅ NiFi HTTP endpoint returns 200 (not 503)
- ✅ Database has flight data
- ✅ Backend API responding

### Step 7: Access Frontend
```bash
# Frontend should be running on port 3000
open http://localhost:3000
```

Login with:
- **YBBN (Brisbane):** `admin_ybbn` / `admin123`
- **LIRN (Naples):** `admin_lirn` / `admin123`
- **VIDP (Delhi):** `admin_vidp` / `admin123`

---

## ✅ Verification Checklist

### 1. Check Services Running
```bash
docker ps
```
Should see 9+ containers running.

### 2. Check NiFi ListenHTTP Ports
```bash
netstat -an | grep -E "8092|8093|8094"
```
Should show:
```
tcp46  *:8092  LISTEN
tcp46  *:8093  LISTEN
tcp46  *:8094  LISTEN
```

If ports NOT listening → Run `infrastructure/nifi/setup-nifi.sh`

### 3. Test NiFi Endpoint
```bash
curl -X POST http://localhost:8092/adsb-ingest \
  -H "Content-Type: application/json" \
  -d '[{"test":"data"}]' \
  -w "\nHTTP: %{http_code}\n"
```
Expected: `HTTP: 200`  
If `HTTP: 503` → NiFi flows not started, run setup script

### 4. Check Database Has Data
```bash
# Wait 30 seconds after fresh start for mock generators to run
sleep 30

docker exec tam-timescaledb-1 psql -U postgres -d utam -c \
  "SELECT tenant_code, COUNT(*) as flights FROM flights GROUP BY tenant_code;"
```
Expected:
```
 tenant_code | flights 
-------------+---------
 LIRN        |      15
 VIDP        |      15
 YBBN        |      15
```

### 5. Check Backend API
```bash
curl -H "X-User-ICAO: YBBN" http://localhost:8080/api/flights | jq '.data | length'
```
Expected: Number (e.g., `10`)

### 6. Check Kafka Topics
Open Redpanda Console: http://localhost:8090

Should see topics:
- `flight-raw-json`
- `vehicle-raw-json`
- `cv-event-raw-json`

With active messages.

---

## 🔧 Troubleshooting

### Problem: Flights Not Appearing on Map

**Check 1: Are mock generators running?**
```bash
docker logs tam-backend-1 --tail 50 | grep "MockAdsb"
```
Should see: `Generated flight data: Flight{...}`

**Check 2: Can generators reach NiFi?**
```bash
docker logs tam-backend-1 --tail 50 | grep "Failed to send"
```
Should NOT see errors. If you see "503 Service Unavailable" → NiFi flows not started.

**Check 3: Is NiFi receiving data?**
```bash
curl -X POST http://localhost:8092/adsb-ingest \
  -H "Content-Type: application/json" \
  -d '[{"test":"data"}]'
```
Should return HTTP 200.

**Check 4: Is Kafka receiving messages?**
- Open http://localhost:8090
- Check `flight-raw-json` topic
- Should see messages arriving every 2 seconds

**Check 5: Is backend consuming from Kafka?**
```bash
docker logs tam-backend-1 --tail 50 | grep "FlightConsumer"
```
Should see: `Received N flights from Kafka`

**Check 6: Is data persisting to DB?**
```bash
docker exec tam-timescaledb-1 psql -U postgres -d utam -c \
  "SELECT COUNT(*) FROM flights WHERE timestamp > NOW() - INTERVAL '1 minute';"
```
Should show recent flights.

---

### Problem: NiFi Ports Not Listening

**Solution:** Run NiFi setup script
```bash
cd infrastructure/nifi
./setup-nifi.sh
```

If script fails, check NiFi logs:
```bash
docker logs tam-nifi-1 --tail 100
```

---

### Problem: Database Empty After Fresh Start

**Cause:** Database init scripts only run on FIRST container start.

**Solution 1:** Wait 30 seconds for mock generators to populate data.

**Solution 2:** If still empty, check init scripts ran:
```bash
docker exec tam-timescaledb-1 psql -U postgres -d utam -c "\dt"
```
Should show tables: `flights`, `vehicles`, `tenants`, etc.

If no tables, manually run init:
```bash
docker exec -i tam-timescaledb-1 psql -U postgres -d utam < infrastructure/db/init/01-core-platform.sql
docker exec -i tam-timescaledb-1 psql -U postgres -d utam < infrastructure/db/init/02-aviation-domain.sql
# ... etc
```

---

### Problem: Backend Not Starting

**Check logs:**
```bash
docker logs tam-backend-1
```

Common issues:
- **Database not ready:** Wait for timescaledb health check
- **Kafka not ready:** Wait for redpanda health check
- **Port conflict:** Check if 8080 is already in use

**Force rebuild:**
```bash
docker-compose -f docker-compose.dev.yml up -d --build backend
```

---

## 📊 Expected Data Flow

After fresh start (30 seconds):

```
MockAdsbGenerator_YBBN (every 2s)
    ↓ HTTP POST :8092
NiFi ListenHTTP
    ↓ ProcessJSON
NiFi PublishKafka
    ↓ Topic: flight-raw-json
FlightConsumer.java
    ↓ Split
    ├─→ TimescaleDB.flights table
    └─→ WebSocket /topic/flights/YBBN
         ↓
Frontend FlightLayer.tsx
    ├─→ REST API load (getActiveFlights)
    └─→ WebSocket updates (real-time)
```

Same flow for LIRN and VIDP airports.

---

## 🎯 Key URLs

| Service | URL | Credentials |
|---------|-----|-------------|
| Frontend | http://localhost:3000 | admin_ybbn / admin123 |
| Backend API | http://localhost:8080 | - |
| NiFi UI | http://localhost:8091/nifi | admin / admin123456789 |
| Redpanda Console | http://localhost:8090 | - |
| MinIO Console | http://localhost:9001 | minioadmin / minioadmin |
| TimescaleDB | localhost:5432 | postgres / password |

---

## 📝 Files to Keep in Sync

When making changes, ensure these are committed to Git:

### Critical Config Files
- `docker-compose.dev.yml` - Service definitions, volumes, ports
- `backend/src/main/resources/application.yml` - Spring Boot config
- `infrastructure/db/init/*.sql` - Database schema
- `infrastructure/nifi/setup-nifi.sh` - NiFi flow automation

### Backend Code
- `backend/src/main/java/com/utam/simulation/MockAdsb*.java` - Data generators
- `backend/src/main/java/com/utam/service/*Consumer.java` - Kafka consumers
- `backend/src/main/java/com/utam/controller/*.java` - API endpoints

### Frontend Code
- `frontend/src/components/Map/*.tsx` - Map layers
- `frontend/src/components/MapIcons.tsx` - SVG icons
- `frontend/src/pages/*.tsx` - Page components

---

## 🔒 Important Notes

1. **Database Init Scripts:** Only run on first container start
   - If you remove `tam_timescaledb_data` volume, they run again
   - If container restarts with existing volume, they DON'T run

2. **NiFi Flows:** Persist in `tam_nifi_data` volume
   - If you remove volume, must run `setup-nifi.sh` again
   - If volume exists, flows survive restart

3. **Mock Generators:** Auto-start with backend
   - No manual trigger needed
   - Send data every 2 seconds

4. **NiFi Version:** Currently 1.18.0 (downgraded from 1.25.0)
   - DO NOT change back to 1.25.0 - ListenHTTP broken
   - 1.18.0 is stable LTS version

---

## 📞 Support

For issues, check:
1. This guide's troubleshooting section
2. `SESSION_SUMMARY_20260123.md` - Full session details
3. `SETUP_GUIDE.md` - Original setup documentation
4. Docker logs: `docker logs <container-name>`
5. Backend logs: `docker logs tam-backend-1 --tail 100`

---

**Last Updated:** January 23, 2026  
**NiFi Version:** 1.18.0  
**Architecture:** NiFi → Kafka → Spring Boot → TimescaleDB + WebSocket → React

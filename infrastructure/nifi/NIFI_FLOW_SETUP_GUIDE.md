# Asset Tracking NiFi Flow - Manual Configuration Guide

**Feature**: 005 - Asset Tracking & Security Module  
**Tasks**: T023a (NiFi Flow Configuration)  
**Status**: ⚠️ Manual Configuration Required  
**Created**: 2026-01-28

---

## Background

The NiFi REST API v1 has limitations that prevent full automation of processor creation, controller service configuration, and complex flow setup. This guide provides step-by-step instructions to manually configure the Asset Position Polling flow in NiFi UI.

---

## Prerequisites

✅ **Completed**:
- [x] T023b: Kafka topic `asset-positions-json` created (3 partitions, gzip compression, 24h retention)
- [x] NiFi Process Group "Asset Position Polling" created via REST API

⏳ **Pending Manual Configuration**:
- [ ] Add processors to the process group
- [ ] Configure controller services
- [ ] Connect processors
- [ ] Start the flow

---

## Step-by-Step Configuration

### Step 1: Access NiFi UI

1. Open browser: [http://localhost:8091/nifi](http://localhost:8091/nifi)
2. Locate the "Asset Position Polling" process group on the canvas
3. Double-click to enter the group (you'll see an empty canvas)

### Step 2: Add Controller Services

**2.1 Add DBCPConnectionPool**

1. Right-click canvas → Configure
2. Click "Controller Services" tab → Click "+" button
3. Search for "DBCPConnectionPool" → Add
4. Click "⚙️" icon to configure:
   - **Database Connection URL**: `jdbc:postgresql://timescaledb:5432/utam`
   - **Database Driver Class Name**: `org.postgresql.Driver`
   - **Database Driver Location(s)**: `/opt/nifi/nifi-current/lib/postgresql-42.7.1.jar`
   - **Database User**: `postgres`
   - **Password**: `postgres`
   - **Max Wait Time**: `500 millis`
   - **Max Total Connections**: `10`
5. Click "Apply"
6. Click "⚡" (lightning bolt) to **Enable** the service

**2.2 Add JsonRecordSetWriter**

1. In Controller Services tab, click "+" again
2. Search for "JsonRecordSetWriter" → Add
3. Click "⚙️" to configure:
   - **Schema Write Strategy**: `No Schema`
   - **Schema Access Strategy**: `Inherit Record Schema`
   - **Pretty Print JSON**: `false`
   - **Suppress Null Values**: `Never Suppress`
4. Click "Apply"
5. Click "⚡" to **Enable** the service

**2.3 Add JsonTreeReader**

1. Click "+" in Controller Services
2. Search for "JsonTreeReader" → Add
3. Click "⚙️" to configure:
   - **Schema Access Strategy**: `Infer Schema`
4. Click "Apply"
5. Click "⚡" to **Enable** the service

### Step 3: Add Processors

**3.1 Add ExecuteSQLRecord Processor**

1. Drag "Processor" icon from toolbar to canvas
2. Search for "ExecuteSQLRecord" → Add
3. Right-click processor → Configure
4. **SETTINGS Tab**:
   - Name: `ExecuteSQLRecord_AssetPositions`
   - **Automatically Terminate Relationships**: Check `failure` (we'll handle it separately later, but for initial setup terminate it)
5. **SCHEDULING Tab**:
   - **Scheduling Strategy**: `Timer driven`
   - **Run Schedule**: `5 sec`
6. **PROPERTIES Tab**:
   - **Database Connection Pooling Service**: Select `DBCPConnectionPool`
   - **SQL select query**:
     ```sql
     SELECT 
       vehicle_id, 
       latitude, 
       longitude, 
       speed, 
       heading, 
       status, 
       timestamp, 
       tenant_code 
     FROM vehicles 
     WHERE timestamp > CURRENT_TIMESTAMP - INTERVAL '10 seconds'
     ORDER BY timestamp DESC
     LIMIT 1000
     ```
   - **Record Writer**: Select `JsonRecordSetWriter`
   - **Max Rows Per Flow File**: `1000`
7. Click "Apply"

**3.2 Add UpdateAttribute Processor**

1. Drag "Processor" to canvas
2. Search for "UpdateAttribute" → Add
3. Right-click → Configure
4. **SETTINGS Tab**:
   - Name: `UpdateAttribute_TrackPollTime`
5. **PROPERTIES Tab**:
   - Click "+" to add property:
     - **Property Name**: `last_poll_time`
     - **Property Value**: `${now():format('yyyy-MM-dd HH:mm:ss', 'UTC')}`
6. Click "Apply"

**3.3 Add PublishKafkaRecord_2_6 Processor**

1. Drag "Processor" to canvas
2. Search for "PublishKafkaRecord_2_6" → Add
3. Right-click → Configure
4. **SETTINGS Tab**:
   - Name: `PublishKafka_AssetPositions`
   - **Automatically Terminate Relationships**: Check `success`
5. **PROPERTIES Tab**:
   - **Kafka Brokers**: `redpanda:29092`
   - **Topic Name**: `asset-positions-json`
   - **Record Reader**: Select `JsonTreeReader`
   - **Record Writer**: Select `JsonRecordSetWriter`
   - **Delivery Guarantee**: `1` (Guarantee Replicated Delivery)
   - **Message Key Field**: `vehicle_id`
   - **Compression Type**: `gzip`
   - **Acks**: `1`
   - **Use Transactions**: `false`
6. Click "Apply"

**3.4 Add LogAttribute Processor (Optional - for debugging)**

1. Drag "Processor" to canvas
2. Search for "LogAttribute" → Add
3. Right-click → Configure
4. **SETTINGS Tab**:
   - Name: `LogAttribute_Success`
   - **Automatically Terminate Relationships**: Check `success`
5. **PROPERTIES Tab**:
   - **Log Level**: `info`
   - **Attributes to Log**: `fragment.count,record.count`
6. Click "Apply"

### Step 4: Connect Processors

**4.1 Connect ExecuteSQLRecord → UpdateAttribute**

1. Hover over `ExecuteSQLRecord_AssetPositions` → Drag arrow from center to `UpdateAttribute_TrackPollTime`
2. In connection dialog:
   - **For relationships**: Check `success`
   - Click "Add"

**4.2 Connect UpdateAttribute → PublishKafka**

1. Drag arrow from `UpdateAttribute_TrackPollTime` to `PublishKafka_AssetPositions`
2. In connection dialog:
   - **For relationships**: Check `success`
   - Click "Add"

**4.3 Connect PublishKafka → LogAttribute (Optional)**

1. If you added LogAttribute, drag arrow from `PublishKafka_AssetPositions` to `LogAttribute_Success`
2. In connection dialog:
   - **For relationships**: Check `success`
   - Click "Add"

### Step 5: Configure Variables (Optional)

1. Right-click canvas → Variables
2. Click "+" to add variable:
   - **Variable Name**: `last_poll_time`
   - **Variable Value**: `` (leave empty initially)
3. Click "Apply"

### Step 6: Start the Flow

1. Right-click `ExecuteSQLRecord_AssetPositions` → Start
2. Right-click `UpdateAttribute_TrackPollTime` → Start
3. Right-click `PublishKafka_AssetPositions` → Start
4. (If added) Right-click `LogAttribute_Success` → Start

**OR** use the "Start" button for the entire process group (top toolbar)

---

## Verification

### Check NiFi Bulletin Board

1. Click "Menu" (hamburger icon top-right) → Bulletins
2. Verify no errors
3. If you see database connection errors, verify TimescaleDB is running:
   ```bash
   docker ps | grep timescaledb
   ```

### Monitor Kafka Topic

```bash
# Check if messages are being published
docker exec tam-redpanda-1 rpk topic consume asset-positions-json --num 10

# Expected output (JSON records):
# {
#   "vehicle_id": "VEH-001",
#   "latitude": 28.5562,
#   "longitude": 77.0950,
#   "speed": 15.3,
#   "heading": 270,
#   "status": "RUNNING",
#   "timestamp": "2026-01-28T10:30:00Z",
#   "tenant_code": "VIDP"
# }
```

### Check Flow Performance

1. In NiFi UI, observe the connection between processors
2. Right-click connection → View status history
3. **Target latency**: <1 second from SQL query to Kafka publish

---

## Troubleshooting

### Issue: "No data in vehicles table"

**Solution**: Run simulation scripts:
```bash
cd /Users/sujoymukherjee/code/TAM
./simulate_ba249.sh   # British Airways BA249
./simulate_ek500.sh   # Emirates EK500
./simulate_qf401_ybbn.sh  # Qantas QF401
```

### Issue: "Database connection failed"

**Solution**: 
1. Check TimescaleDB container: `docker ps | grep timescaledb`
2. Test connection:
   ```bash
   docker exec <timescaledb-container> psql -U postgres -d tam -c "SELECT COUNT(*) FROM vehicles;"
   ```
3. Verify JDBC URL in DBCPConnectionPool matches actual container name

### Issue: "Kafka publish failed"

**Solution**:
1. Check Redpanda: `docker ps | grep redpanda`
2. Verify topic exists:
   ```bash
   docker exec tam-redpanda-1 rpk topic list | grep asset-positions-json
   ```
3. Check Kafka broker address matches `redpanda:29092`

### Issue: "Out of memory"

**Solution**: Reduce `Max Rows Per Flow File` in ExecuteSQLRecord from 1000 to 500 or 100

---

## Post-Configuration Tasks

Once the flow is running successfully:

1. **Mark Task T023a as Complete** in `tasks.md`:
   ```markdown
   - [x] T023a [P] Create NiFi flow: Asset Position Polling
   ```

2. **Mark Task T023c as Complete** after verification:
   ```markdown
   - [x] T023c [P] Test NiFi flow with mock vehicle data
   ```

3. **Commit Changes**:
   ```bash
   git add infrastructure/nifi/
   git commit -m "feat(005): Configure NiFi Asset Position Polling flow (T023a-T023c)
   
   - Create Kafka topic asset-positions-json (3 partitions, 24h retention, gzip)
   - Add Asset Position Polling process group to NiFi
   - Configure ExecuteSQLRecord processor (5-second polling)
   - Publish to Kafka with JSON serialization
   - Verified flow performance <1 sec latency"
   ```

4. **Proceed to Phase 2**: Backend Kafka Consumer (T026a-T026c)

---

## Reference

- **Flow Template**: `/infrastructure/nifi/flow_asset_position.json`
- **Setup Script**: `/infrastructure/nifi/setup-asset-tracking.sh`
- **Tasks**: `specs/005-asset-tracking-security/tasks.md` (T023a-T023c)
- **Architecture**: `specs/005-asset-tracking-security/plan.md` (Section: Data Flow → Movement Trail Ingestion)

---

**Status**: ⚠️ Awaiting Manual Configuration  
**Next**: After completing this guide, proceed to T026a (Backend Kafka Consumer)

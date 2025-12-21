# Research: Platform Phase 1

**Feature**: `003-nifi-ksqldb-integration` | **Date**: 2025-12-21

## 1. Redis Caching Strategy

### Problem
Database is hit every time a user polls for "Active Flights" (every 2-3s per user). This scales poorly.

### Options
1. **Full Object Cache**: Store the entire `List<Flight>` as a single JSON string.
   - *Pros*: Simple.
   - *Cons*: Hard to update individual flights.
2. **Individual Key Cache**: Store each flight as `flight:{id}` and maintain a Set of active IDs.
   - *Pros*: Granular updates.
   - *Cons*: Multiple round-trips to fetch all.
3. **RedisJSON**: Use RedisJSON module.
   - *Pros*: Best of both worlds.
   - *Cons*: Requires RedisJSON module (might not be in standard redis image).

### Decision
**Option 2 (Individual Key + Set)**.
- **Key**: `flight:{icao}:{callsign}` -> JSON String.
- **Set**: `active_flights:{icao}` -> List of callsigns.
- **TTL**: 5 minutes (auto-expire stale flights).

## 2. WebSocket Topic Design (Multi-Tenancy)

### Problem
We need to push updates to clients, but ensure LIRN users don't see VIDP flights.

### Options
1. **Single Topic + Client Filter**: `/topic/flights` sends everything; client filters.
   - *Pros*: Simple backend.
   - *Cons*: Security risk (data leakage).
2. **Dynamic Topics**: `/topic/flights/{icao}`.
   - *Pros*: Secure, efficient.
   - *Cons*: Client needs to know which topic to subscribe to.

### Decision
**Option 2 (Dynamic Topics)**.
- **Global Admin**: Subscribes to `/topic/flights/all`.
- **Tenant User**: Subscribes to `/topic/flights/{icao}` (e.g., `/topic/flights/VIDP`).

## 3. MinIO Storage Structure

### Problem
Where to store generated reports and raw archives?

### Decision
- **Bucket**: `tam-data`
- **Structure**:
  - `reports/{icao}/{yyyy}/{mm}/{dd}/{report_id}.pdf`
  - `archives/raw/{icao}/{yyyy}/{mm}/{dd}/{hour}.json`

## 4. KSQLDB Integration (Future)

### Context
The folder name implies KSQLDB.

### Decision
For Phase 1, we will **NOT** implement KSQLDB logic yet, as the user request focused on Redis/MinIO/WebSockets. We will set up the infrastructure (which exists) but defer the stream processing logic to Phase 2.

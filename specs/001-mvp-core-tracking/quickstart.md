# Quickstart: Core MVP Tracking & Alerting

**Feature**: `001-mvp-core-tracking`

## Prerequisites

- Docker & Docker Compose
- Java 17+ (for local dev, optional if using Docker)
- Node.js 18+ (for local dev, optional if using Docker)

## Setup & Run

1. **Clone the repository**
   ```bash
   git clone <repo-url>
   cd TAM
   ```

2. **Start Infrastructure (NiFi, Kafka, DB)**
   ```bash
   docker-compose up -d
   ```
   *Wait for containers to be healthy. NiFi takes a minute to start.*

3. **Configure NiFi (First Time Only)**
   - Open NiFi UI: `http://localhost:8091/nifi`
   - Import the `utam-ingestion-flow.json` (if provided) or create the flow manually:
     - **Flight Flow**: `ListenHTTP` (Port 8081, Path `/api/adsblivedata`) -> `PublishKafka` (Topic `flight-raw-json`)
     - **Vehicle Flow**: `ListenHTTP` (Port 8081, Path `/veh_live_data_con`) -> `PublishKafka` (Topic `vehicle-raw-json`)
     - **Turnaround Flow**: `ListenHTTP` (Port 8081, Path `/api/turnaround/events`) -> `PublishKafka` (Topic `turnaround-raw-json`)
   - Start the Processors.

4. **Run Backend (Spring Boot)**
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```
   *Or run via Docker if configured.*

5. **Run Frontend (React)**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
   *Access Dashboard at `http://localhost:3000`*

## Verification

1. **Check Map**: Open `http://localhost:3000`. You should see the map centered on IGIA.
2. **Check Data**:
   - Ensure Mock Generators are running (they are part of the Backend in this MVP).
   - Verify aircraft and vehicle icons appearing on the map.
   - Verify Gantt chart updating.
3. **Check NiFi**:
   - Check NiFi UI counters to see data flowing through `ListenHTTP` processors.

## Troubleshooting

- **NiFi not reachable**: Ensure port 8091 is mapped and container is running.
- **No Data on Map**: Check Kafka topics (`docker exec -it tam-kafka-1 kafka-console-consumer ...`) to see if NiFi is publishing.
- **CORS Errors**: Ensure Backend allows requests from `localhost:3000`.

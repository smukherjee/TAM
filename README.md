# UTAM - Unified Tracking and Alerting Module

## Overview

UTAM is a real-time tracking system for flights and ground vehicles, featuring speed violation alerts and turnaround management. The platform supports multi-tenant deployments for Delhi (VIDP), Naples (LIRN), and Brisbane (YBBN). It uses a modern tech stack with Apache NiFi for data ingestion, Kafka for streaming, Spring Boot for backend processing, and React for the frontend dashboard.

## Tech Stack

- **Backend**: Java 21, Spring Boot 3.4.12, Spring Kafka, Spring Data JPA
- **Frontend**: React 18, TypeScript, Vite, Leaflet, Tailwind CSS
- **Database**: PostgreSQL / TimescaleDB
- **Ingestion & Streaming**: Apache NiFi, Apache Kafka
- **Simulation**: Java (Integrated in Backend)

## Prerequisites

- **Java**: JDK 21
- **Node.js**: v18+
- **Docker**: Docker Desktop or Docker Engine & Docker Compose

## Setup & Run

For comprehensive deployment instructions, maintenance procedures, and troubleshooting, please refer to:
👉 **[DEPLOYMENT.md](DEPLOYMENT.md)**

### Quick Start (Internal Orchestrator)

The platform includes a unified orchestrator that sets up everything from database to dashboards:

```bash
./infrastructure/setup-infrastructure.sh
```

2. **Configure NiFi** (Wait ~60s for NiFi to start first):

   ```bash
   make setup-nifi
   ```

3. **Access Services**:
    - **Frontend**: [http://localhost:3000](http://localhost:3000) (Login: `admin_vidp / admin`, `admin_lirn / admin`, or `admin_ybbn / admin`)
    - **Backend API**: [http://localhost:8080](http://localhost:8080)
    - **NiFi UI**: [http://localhost:8091/nifi](http://localhost:8091/nifi)

### Option 1: Full Docker Deployment

Run the entire stack (Infrastructure, Backend, Frontend) in Docker containers.

1. **Build and Start**:

   ```bash
   docker-compose up --build -d
   ```

2. **Access Services**:
    - **Frontend**: [http://localhost:3000](http://localhost:3000)
    - **Backend API**: [http://localhost:8080](http://localhost:8080)
    - **NiFi UI**: [http://localhost:8091/nifi](http://localhost:8091/nifi)

### Option 2: Local Development

Run infrastructure in Docker, but run Backend and Frontend locally for development.

#### 1. Start Infrastructure Only

Start the required services (Kafka, Zookeeper, NiFi, TimescaleDB):

```bash
docker-compose up -d zookeeper kafka nifi timescaledb
```

- **NiFi UI**: [http://localhost:8091/nifi](http://localhost:8091/nifi)
- **TimescaleDB**: Port 5432
- **Kafka**: Port 9092

#### 2. Run Backend

Navigate to the backend directory and run the Spring Boot application:

```bash
cd backend
mvn spring-boot:run
```

Alternatively, build and run the JAR:

```bash
mvn clean package
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

- **API Base URL**: [http://localhost:8080](http://localhost:8080)

#### 3. Run Frontend

Navigate to the frontend directory, install dependencies, and start the development server:

```bash
cd frontend
npm install
npm run dev
```

- **Dashboard**: [http://localhost:3000](http://localhost:3000)

### 4. Run Simulation Data Generators

The simulation data generators are integrated into the Spring Boot Backend as scheduled components. They automatically start generating mock data (Flights, Vehicles, CV Events) when the backend application starts.

- **Flight Data (ADSB)**: Generated every 2 seconds.
- **Vehicle Data (Telit)**: Generated every 2 seconds.
- **Computer Vision Events**: Generated every 5 seconds.

To configure the simulation URLs (e.g., if NiFi is running on a different host/port), update `backend/src/main/resources/application.yml`.

## Data Generator Administration

The comprehensive data generator system supports **24 entity types** with realistic airport simulation data.

### Admin Dashboard

Access the generator admin dashboard at [http://localhost:3000/admin/generators](http://localhost:3000/admin/generators) (requires ADMIN role).

Features:

- **Overview**: Generator status, quick actions
- **Controls**: Per-generator start/stop, batch/continuous modes
- **Metrics**: Real-time throughput and performance
- **Security**: Role verification, audit logging

### Quick Commands

```bash
# Start batch population
curl -X POST http://localhost:8080/api/admin/generators/batch/start \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Enable continuous real-time simulation
curl -X POST http://localhost:8080/api/admin/generators/continuous/start \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Check status
curl http://localhost:8080/api/admin/generators/status
```

### Entity Types Generated

| Category   | Entities                                              |
| ---------- | ----------------------------------------------------- |
| Reference  | Stands, Depots, VehicleTypes, AirportBoundaries       |
| Core       | Flights, Vehicles (15 GSE types), Assets, Turnarounds |
| Tracking   | VehiclePositions, FlightPositions, MovementTrails     |
| Operations | VehicleAssignments, VehiclePaths, Dispatches          |
| Alerts     | Alerts (12 types), FinancialMetrics                   |
| Security   | RestrictedZones, Violations, MovementDiscrepancies    |

### Supported Tenants

- **VIDP** (Delhi) - Center: 28.5665, 77.1031, Timezone: Asia/Kolkata
- **YBBN** (Brisbane) - Center: -27.3942, 153.1218, Timezone: Australia/Brisbane

For detailed documentation, see [backend/src/main/java/com/utam/simulation/README.md](backend/src/main/java/com/utam/simulation/README.md).

## Architecture Overview

1. **Data Ingestion**: Java components in the backend simulate data sources and push JSON payloads to **Apache NiFi** endpoints.
2. **Streaming**: NiFi processes the data and publishes it to **Apache Kafka** topics.
3. **Processing**: The **Spring Boot Backend** consumes messages from Kafka, processes them (e.g., speed checks), and persists data to **TimescaleDB**.
4. **Visualization**: The **React Frontend** polls the backend API to display real-time positions and alerts on a map.

## API Endpoints

- `GET /api/flights`: Retrieve active flights.
- `GET /api/vehicles`: Retrieve active ground vehicles.
- `GET /api/alerts`: Retrieve generated alerts (e.g., speed violations).
- `GET /api/cv/events`: Retrieve computer vision events.

## Testing and Verification

For comprehensive manual testing and verification of all system components, refer to [sanitytest.md](sanitytest.md).

This includes detailed checklists for each service, integration tests, performance verification, and troubleshooting steps.

## Troubleshooting

- **NiFi Connection Refused**: Ensure the Docker container is running and port 8091 is accessible.
- **Database Connection**: Ensure TimescaleDB is up (`docker-compose ps`) and the credentials in `application.properties` match `docker-compose.yml`.
- **Kafka Issues**: If the backend fails to connect to Kafka, ensure the `SPRING_KAFKA_BOOTSTRAP_SERVERS` matches the exposed port or internal docker network alias depending on where you run the backend (host vs container).

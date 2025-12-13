# UTAM - Unified Tracking and Alerting Module

## Overview

UTAM is a real-time tracking system for flights and ground vehicles, featuring speed violation alerts and turnaround management. It uses a modern tech stack with Apache NiFi for data ingestion, Kafka for streaming, Spring Boot for backend processing, and React for the frontend dashboard.

## Tech Stack

- **Backend**: Java 21, Spring Boot 3.4.12, Spring Kafka, Spring Data JPA
- **Frontend**: React 18, TypeScript, Vite, Leaflet, Tailwind CSS
- **Database**: PostgreSQL / TimescaleDB
- **Ingestion & Streaming**: Apache NiFi, Apache Kafka
- **Simulation**: Python 3

## Prerequisites

- **Java**: JDK 21
- **Node.js**: v18+
- **Docker**: Docker Desktop or Docker Engine & Docker Compose
- **Python**: Python 3.8+ (for simulation scripts)

## Setup & Run

You can run the application in two ways: **Full Docker Deployment** (easiest) or **Local Development**.

### Option 1: Full Docker Deployment

Run the entire stack (Infrastructure, Backend, Frontend) in Docker containers.

1.  **Build and Start**:

    ```bash
    docker-compose up --build -d
    ```

2.  **Access Services**:
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

To simulate real-time traffic (Flights, Vehicles, CV Events), use the provided Python scripts. These scripts generate mock data and send it to the NiFi ingestion layer.

**Install Python Dependencies:**

```bash
pip install -r backend/src/main/resources/simulation/requirements.txt
```

**Run Generators:**

Open separate terminals for each generator you want to run:

- **Flight Data (ADSB)**:

  ```bash
  python3 backend/src/main/resources/simulation/adsb-generator.py
  ```

- **Vehicle Data (Telit)**:

  ```bash
  python3 backend/src/main/resources/simulation/telit-generator.py
  ```

- **Computer Vision Events**:

  ```bash
  python3 backend/src/main/resources/simulation/cv-generator.py
  ```

## Architecture Overview

1. **Data Ingestion**: Python scripts simulate data sources and push JSON payloads to **Apache NiFi** endpoints.
2. **Streaming**: NiFi processes the data and publishes it to **Apache Kafka** topics.
3. **Processing**: The **Spring Boot Backend** consumes messages from Kafka, processes them (e.g., speed checks), and persists data to **TimescaleDB**.
4. **Visualization**: The **React Frontend** polls the backend API to display real-time positions and alerts on a map.

## API Endpoints

- `GET /api/flights`: Retrieve active flights.
- `GET /api/vehicles`: Retrieve active ground vehicles.
- `GET /api/alerts`: Retrieve generated alerts (e.g., speed violations).
- `GET /api/cv/events`: Retrieve computer vision events.

## Troubleshooting

- **NiFi Connection Refused**: Ensure the Docker container is running and port 8091 is accessible.
- **Database Connection**: Ensure TimescaleDB is up (`docker-compose ps`) and the credentials in `application.properties` match `docker-compose.yml`.
- **Kafka Issues**: If the backend fails to connect to Kafka, ensure the `SPRING_KAFKA_BOOTSTRAP_SERVERS` matches the exposed port or internal docker network alias depending on where you run the backend (host vs container).

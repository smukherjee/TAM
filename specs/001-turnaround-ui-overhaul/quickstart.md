# Quickstart: Turnaround UI Overhaul

## Prerequisites
- Docker & Docker Compose
- Java 17+ (for local dev)
- Node.js 18+ (for local dev)

## Running the Mock Environment

1. **Start Infrastructure**:
   ```bash
   docker-compose up -d postgres kafka zookeeper
   ```

2. **Start Backend (with Mock Generator)**:
   The backend is configured to run the `MockDataGenerator` when the `dev` profile is active.
   ```bash
   cd backend
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```
   *This will start generating random flight schedules and publishing them to Kafka.*

3. **Start Frontend**:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
   Access the dashboard at `http://localhost:3000`.

## Verifying the Setup

1. **Check Grid View**: You should see ~10-20 cards appearing on the dashboard within 30 seconds of starting the backend.
2. **Check Kafka**:
   ```bash
   docker exec -it kafka kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic flight-raw-json --from-beginning
   ```
   You should see JSON messages flowing.
3. **Check Database**:
   Connect to Postgres (`jdbc:postgresql://localhost:5432/tam_db`) and query:
   ```sql
   SELECT * FROM turnaround_sessions;
   ```

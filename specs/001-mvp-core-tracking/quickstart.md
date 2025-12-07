# Quickstart: Core MVP Tracking

## Prerequisites

- Docker & Docker Compose
- Java 17+ (for local build)
- Node.js 18+ (for local build)

## Running the System

1. **Clone the repository**

   ```bash
   git clone <repo-url>
   cd tam-mvp
   ```

2. **Start the Environment**

   Run the full stack (Backend, Frontend, Kafka, DB) using Docker Compose:

   ```bash
   docker-compose up --build
   ```

3. **Access the Dashboard**
   Open your browser to: [http://localhost:3000](http://localhost:3000)

   **Credentials**:
   - Username: `admin`
   - Password: `admin`

4. **Verify Data Flow**
   - The system will automatically start generating mock data.
   - You should see aircraft and vehicles moving on the map.
   - Check the "Alerts" panel for any speed violations.

## Development Commands

### Backend (Spring Boot)

```bash
cd backend
./mvnw spring-boot:run
```

### Frontend (React)

```bash
cd frontend
npm install
npm start
```

## Troubleshooting

- **Kafka Connection Issues**: Ensure `docker-compose` is running and Kafka is healthy (`docker ps`).
- **No Data on Map**: Check backend logs for ingestion errors. Ensure mock generators are active.

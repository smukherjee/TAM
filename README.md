# UTAM - Unified Tracking and Alerting Module

## Overview

UTAM is a real-time tracking system for flights and ground vehicles, featuring speed violation alerts.

## Tech Stack

- **Backend**: Java 25, Spring Boot 3.4.0, Kafka, PostgreSQL/TimescaleDB
- **Frontend**: React 18, TypeScript, Vite, Leaflet
- **Infrastructure**: Docker Compose

## Prerequisites

- Java 25
- Node.js 18+
- Docker & Docker Compose

## Setup & Run

1. **Start Infrastructure**:

   ```bash
   docker-compose up -d
   ```

2. **Run Backend**:

   ```bash
   cd backend
   mvn spring-boot:run
   ```

   Or build and run jar:

   ```bash
   mvn clean package
   java -jar target/backend-0.0.1-SNAPSHOT.jar
   ```

3. **Run Frontend**:

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

## Access the Application

- **Frontend (Dashboard)**: [http://localhost:3000](http://localhost:3000)
- **Backend API**: [http://localhost:8080](http://localhost:8080)

## Features

- **Live Flight Tracking**: Real-time aircraft positions on map.
- **Live Vehicle Tracking**: Real-time ground vehicle positions.
- **Speed Alerts**: Automatic alerts for vehicles exceeding speed limits.

## API Endpoints

- `GET /api/flights`: Get active flights
- `GET /api/vehicles`: Get active vehicles
- `GET /api/alerts`: Get recent alerts

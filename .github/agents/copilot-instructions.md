# TAM Development Guidelines

Auto-generated from all feature plans. Last updated: 2025-12-07

## Active Technologies
- Java 17+ (Spring Boot 3.x), TypeScript (React 18+) + Apache NiFi (Ingestion), Spring Boot Web, Spring Kafka, React, Leaflet, react-leafle (001-mvp-core-tracking)
- PostgreSQL 16+ with TimescaleDB extension (001-mvp-core-tracking)
- Java 17 (Spring Boot 3.x), TypeScript 5.x (React 18) (003-nifi-ksqldb-integration)
- Java 17+ (Backend), TypeScript 5.x (Frontend) + Spring Boot 3.x, React 18+, TailwindCSS (001-turnaround-ui-overhaul)
- PostgreSQL 16+ (TimescaleDB) (001-turnaround-ui-overhaul)
- Java 21 (Virtual Threads enabled) + Spring Boot 3.x, Resilience4j (Circuit Breaker), Micrometer (Context Propagation) (004-platform-services-layer)
- PostgreSQL 16+ (TimescaleDB), Redis (Cache), MinIO (Object Storage) (004-platform-services-layer)
- Java 21 (Virtual Threads enabled) + Spring Boot 3.x, Resilience4j (Circuit Breaker), Micrometer (Context Propagation + Tracing), Spring Tx (Synchronization) (004-platform-services-layer)
- Java 21 (Required for Virtual Threads/ScopedValue per FR-006) + Spring Boot 3.2+, Micrometer, Resilience4j, Kafka Clients, Redis Clients (Lettuce), MinIO Java SDK (004-platform-services-layer)
- PostgreSQL 16 (TimescaleDB), Redis (Cache), MinIO (Object Storage) (004-platform-services-layer)
- Java 21 + Spring Boot 3.x, Parquet Hadoop libraries, MinIO SDK (001-refine-platform-spec)
- MinIO with tenant-isolated buckets (tenant-{tenantId}-archive) (001-refine-platform-spec)

- Java 17+ (Spring Boot 3.x), TypeScript (React 18+) + Spring Boot Web, Spring Kafka, React, Leaflet, react-leafle (001-mvp-core-tracking)

## Project Structure

```text
src/
tests/
```

## Commands

npm test && npm run lint

## Code Style

Java 17+ (Spring Boot 3.x), TypeScript (React 18+): Follow standard conventions

## Recent Changes
- 001-refine-platform-spec: Added Java 21 + Spring Boot 3.x, Parquet Hadoop libraries, MinIO SDK
- 004-platform-services-layer: Added Java 21 (Required for Virtual Threads/ScopedValue per FR-006) + Spring Boot 3.2+, Micrometer, Resilience4j, Kafka Clients, Redis Clients (Lettuce), MinIO Java SDK
- 004-platform-services-layer: Added Java 21 (Virtual Threads enabled) + Spring Boot 3.x, Resilience4j (Circuit Breaker), Micrometer (Context Propagation + Tracing), Spring Tx (Synchronization)


<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->

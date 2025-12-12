# Research & Decisions: Core MVP Tracking & Alerting

**Feature**: `001-mvp-core-tracking` | **Date**: 2025-12-12

## Unknowns & Clarifications

| Unknown | Resolution | Source |
|---------|------------|--------|
| Ingestion Architecture | Use Apache NiFi as Ingestion Gateway | Constitution v1.2.0 |
| Map Library | Leaflet (react-leaflet) | Spec Clarifications |
| Tile Provider | OpenStreetMap (OSM) | Spec Clarifications |
| Frontend Updates | HTTP Polling (2-3s) | Spec Clarifications |

## Technology Decisions

### 1. Ingestion: Apache NiFi
- **Decision**: Use Apache NiFi running in a Docker container.
- **Rationale**: Provides a flexible, visual way to handle data ingestion from various sources (HTTP, MQTT, etc.) without writing custom Java adapters. Decouples ingestion from processing.
- **Alternatives**: Spring Boot Controllers (Rejected: Tightly coupled, requires code changes for new sources).

### 2. Message Broker: Apache Kafka
- **Decision**: Single-node Kafka instance.
- **Rationale**: Standard for event-driven architectures, handles high throughput, decouples producers (NiFi) from consumers (Spring Boot).
- **Alternatives**: RabbitMQ (Rejected: Kafka is better suited for stream processing and high-throughput event logs).

### 3. Backend: Spring Boot 3.x
- **Decision**: Java 17+ with Spring Boot 3.
- **Rationale**: Robust, enterprise-grade, excellent Kafka integration (Spring Kafka), familiar to team.
- **Alternatives**: Node.js (Rejected: Team expertise in Java, better multi-threading for complex processing).

### 4. Database: TimescaleDB
- **Decision**: PostgreSQL 16 with TimescaleDB extension.
- **Rationale**: Optimized for time-series data (tracking history), SQL interface, relational features for metadata.
- **Alternatives**: MongoDB (Rejected: Need efficient time-series queries and relational integrity).

### 5. Frontend: React + Leaflet
- **Decision**: React 18 with `react-leaflet`.
- **Rationale**: React is the standard frontend library. Leaflet is lightweight and sufficient for 2D maps.
- **Alternatives**: OpenLayers (Rejected: Too complex for MVP), Mapbox (Rejected: Requires API key/cost).

## Best Practices

- **NiFi**: Use Process Groups to organize flows. Use `HandleHttpRequest` -> `PublishKafka` pattern.
- **Kafka**: Use JSON serialization for messages. Topic naming convention: `domain.entity.type` (e.g., `utam.flight.raw`).
- **Spring Boot**: Use `@KafkaListener` for consumption. Use `Repository` pattern for DB access.
- **React**: Use Context API for state management if needed, or simple props for MVP. Componentize Map Layers.

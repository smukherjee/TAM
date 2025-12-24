# Phase 0: Research & Architecture Decisions

**Feature**: Refine Platform Spec  
**Date**: 2025-12-24  

## 1. Parquet Implementation for Event Archival

### Decision
Use **parquet-avro** library with Avro union schemas for polymorphic DomainEvent payloads. Integrate with Spring Boot via custom configuration beans.

### Rationale
* **Library Choice**: parquet-avro provides excellent support for polymorphic payloads via Avro unions, enabling type-safe schema evolution. It's mature and integrates well with Java ecosystems.
* **Schema Design**: Union approach (Approach 1 from research) balances type safety, evolution, and queryability. Allows adding new event types without breaking existing data.
* **Spring Integration**: Use @Configuration beans for ParquetWriter and Hadoop Configuration. Leverage S3AFileSystem for MinIO/S3 compatibility.
* **Performance**: SNAPPY compression for archival balance of speed and ratio. Batch writing (1000 records) for I/O efficiency.
* **Compatibility**: Bridge Jackson JSON serialization with Avro via custom converters.

### Alternatives Considered
* **parquet-hadoop**: Rejected for higher boilerplate and manual polymorphism handling.
* **Embedded JSON**: Rejected for poor columnar benefits and queryability.

## 2. Tenant-Isolated Object Storage Buckets

### Decision
Use separate buckets per tenant with naming convention `tenant-{tenantId}-archive`. Enforce access via IAM policies and bucket policies.

### Rationale
* **Naming Convention**: Prefix ensures grouping and easy policy application. Includes tenant ID for isolation.
* **Access Control**: IAM roles per tenant with least-privilege policies. Bucket policies deny cross-tenant access.
* **Security**: SSE-KMS encryption for data at rest. HTTPS-only and IP restrictions.
* **Cost Optimization**: Lifecycle rules to transition to cheaper storage (e.g., Glacier after 30 days). Auto-delete after 6 months.
* **Operations**: Monitor via CloudWatch/MinIO metrics. Automate cleanup and scaling.

### Alternatives Considered
* **Shared Buckets with Prefixes**: Rejected for weaker isolation guarantees.
* **Single Bucket per Environment**: Rejected for scaling and cost issues.

## 3. Parquet Schema Design for Polymorphic DomainEvent

### Decision
Implement Union Schema approach with Avro unions for payload polymorphism. Use schema versioning for evolution.

### Rationale
* **Union Types**: Provides type safety and backward compatibility for adding new event types.
* **Evolution**: Append new unions without breaking readers. Use schema registry for versioning.
* **Performance**: Good compression for dense data, excellent queryability via columnar access.
* **Java Compatibility**: Integrate with Jackson via AvroMapper and custom deserializers.
* **Null Handling**: Avro unions with null branches handle optional fields naturally.

### Alternatives Considered
* **Flat Optional Fields**: Rejected for lack of type safety.
* **Embedded JSON**: Rejected for poor performance and queryability.
* **Versioned Schemas**: Considered but unions provide simpler evolution.

## Consolidated Findings

- **Parquet Library**: parquet-avro with Avro unions.
- **Bucket Strategy**: Per-tenant buckets with IAM isolation.
- **Schema Approach**: Union-based with versioning.
- **Compression**: SNAPPY for archival.
- **Retention**: 6 months with lifecycle rules.
- **Security**: SSE-KMS encryption, policy-based access.
- **Operations**: Automated lifecycle, monitoring, and cleanup.</content>
<parameter name="filePath">/Users/sujoymukherjee/code/TAM/specs/001-refine-platform-spec/research.md
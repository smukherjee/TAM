# Feature Specification: Refine Platform Spec

**Feature Branch**: `001-refine-platform-spec`  
**Created**: 2025-12-24  
**Status**: Ready for Implementation  
**Input**: Plan requirements for platform refinement

## User Scenarios & Testing

### User Story 1 - Update Java Version to 21 (Priority: P1)

Platform services must run on Java 21+ to leverage modern JVM features like virtual threads.

**Why this priority**: Java 21 provides performance and scalability improvements critical for platform services.

**Independent Test**: Application starts successfully with Java 21 runtime and passes all existing tests.

**Acceptance Scenarios**:

1. **Given** the platform is configured for Java 21, **When** the application starts, **Then** it initializes without JVM compatibility errors
2. **Given** existing functionality, **When** running on Java 21, **Then** performance meets or exceeds current benchmarks

---

### User Story 2 - Standardize Tenant Identifier (Priority: P1)

All tenant references must use consistent "tenantId" naming instead of "icaoCode".

**Why this priority**: Inconsistent naming causes confusion and integration issues across services.

**Independent Test**: All code references use "tenantId" consistently, no "icaoCode" references remain.

**Acceptance Scenarios**:

1. **Given** tenant context operations, **When** inspecting code, **Then** all use "tenantId" field/variable names
2. **Given** multi-tenant operations, **When** tenant isolation is tested, **Then** uses standardized tenantId values

---

### User Story 3 - Implement Parquet Archival (Priority: P1)

Domain events must be archived to Parquet format in tenant-isolated MinIO buckets with 6-month retention.

**Why this priority**: Enables efficient long-term storage and analytics of event data.

**Independent Test**: Events are successfully archived to tenant-specific Parquet files with proper lifecycle management.

**Acceptance Scenarios**:

1. **Given** a domain event, **When** archived, **Then** it's stored in tenant-specific bucket as Parquet file
2. **Given** archived data, **When** lifecycle rules apply, **Then** files transition to cheaper storage after 30 days and delete after 180 days
3. **Given** Parquet files, **When** queried, **Then** data is accessible and properly typed

## Requirements

### Functional Requirements

- **FR-001**: System MUST run on Java 21+ JVM
- **FR-002**: System MUST use "tenantId" consistently for tenant identification
- **FR-003**: System MUST archive domain events to Parquet format
- **FR-004**: System MUST use tenant-isolated buckets (tenant-{tenantId}-archive)
- **FR-005**: System MUST apply 6-month retention lifecycle to archived data

### Key Entities

- **DomainEvent**: Core event structure with tenantId, eventType, payload
- **TenantBucket**: Bucket configuration with naming convention and lifecycle rules
- **ArchivalConfig**: Configuration for Parquet compression, batch size, retention

## Success Criteria

### Measurable Outcomes

- **SC-001**: Application starts and runs on Java 21 without errors
- **SC-002**: All tenant references use tenantId consistently (0 icaoCode references)
- **SC-003**: Events are archived to Parquet format with <5% compression overhead
- **SC-004**: Tenant isolation prevents cross-tenant data access
- **SC-005**: Archived data follows lifecycle rules (transition after 30 days, deletion after 180 days)

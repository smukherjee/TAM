# Implementation Plan: Refine Platform Spec

**Branch**: `001-refine-platform-spec` | **Date**: 2025-12-24 | **Spec**: [link]
**Input**: Feature specification from `/specs/001-refine-platform-spec/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Refine the platform services layer specification to require Java 21+, standardize tenant identifier naming to tenantId, and specify RawDataArchiver to use Parquet files with tenant-isolated buckets and 6-month retention. Update implementation to align with refined requirements.

## Technical Context

**Language/Version**: Java 21  
**Primary Dependencies**: Spring Boot 3.x, Parquet Hadoop libraries, MinIO SDK  
**Storage**: MinIO with tenant-isolated buckets (tenant-{tenantId}-archive)  
**Testing**: JUnit 5, TestContainers  
**Target Platform**: JVM 21+  
**Project Type**: Library refinement  
**Performance Goals**: Maintain existing performance, add Parquet compression benefits  
**Constraints**: Java 21+ required, tenant isolation mandatory, no failure recovery for archival  
**Scale/Scope**: Update existing platform-core module, add Parquet support

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

✅ **IV. Tech Stack Compliance**: Java 21+ aligns with updated constitution requirement.  
✅ **I. Simplicity First**: Refinement maintains MVP focus.  
✅ **II. Containerization**: No changes to containerization.  
✅ **III. Simulation Driven**: No impact.  
✅ **V. Documentation**: Specification updates improve clarity.

## Project Structure

### Documentation (this feature)

```text
specs/001-refine-platform-spec/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

Refinement of existing backend/platform-core module:
- Update pom.xml with Parquet dependencies
- Modify RawDataArchiver to use Parquet format and tenant-isolated buckets
- Update TenantContextFilter and related classes for tenantId consistency
- Ensure Java 21 compatibility

**Structure Decision**: Refinement project targeting existing platform-core library with minimal structural changes.

## Phases

### Phase 0: Outline & Research

1. **Extract unknowns from Technical Context**
   - Research Parquet file format implementation in Java for DomainEvent archival
   - Evaluate Parquet schema options for DomainEvent structure
   - Assess performance impact of Parquet vs JSON for archival use case
   - Investigate tenant-isolated bucket management in MinIO

2. **Generate and dispatch research agents**
   - Task: "Research Parquet implementation for event archival in Java Spring Boot"
   - Task: "Find best practices for tenant-isolated object storage buckets"
   - Task: "Evaluate Parquet schema design for polymorphic DomainEvent types"

3. **Consolidate findings** in `research.md` with decisions on Parquet schema, implementation approach, and bucket naming strategy.

**Output**: research.md with all unknowns resolved

### Phase 1: Design & Contracts

**Prerequisites:** `research.md` complete

1. **Extract entities from feature spec** → `data-model.md`:
   - Define Parquet schema for DomainEvent archival
   - Specify tenant bucket naming convention
   - Document retention policy implementation

2. **Generate API contracts** from functional requirements:
   - Update ObjectStorageService interface for tenant-aware operations
   - Define RawDataArchiver contract with Parquet format
   - Output updated contracts to `/contracts/`

3. **Agent context update**:
   - Update agent context with Parquet dependencies and Java 21 features

**Output**: data-model.md, /contracts/*, quickstart.md, updated agent context

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |

# TAM Simulation Module Functional Overview

## Location
`backend/src/main/java/com/utam/simulation/`

## Purpose
This module generates realistic airport simulation data for testing, analytics, and development. It covers flights, vehicles, assets, turnarounds, alerts, security, and more.

## Main Packages and Roles

- **config/**
  - `SimulationConfig.java`: Central configuration for simulation parameters.
  - `SimulationMetrics.java`: Exposes Prometheus metrics for simulation.

- **core/**
  - `BaseDataGenerator.java`: Abstract base class for all data generators.
  - `GeneratorOrchestrator.java`: Coordinates and manages all generators.

- **flight/**
  - `FlightDataGenerator.java`: Generates flight arrivals and departures.
  - `FlightTrajectoryCalculator.java`: Calculates flight paths.

- **vehicle/**
  - `VehicleDataGenerator.java`: Generates GSE fleet vehicles.
  - `VehiclePathPlayer.java`: Simulates vehicle movement along paths.
  - `VehicleAssignmentGenerator.java`: Assigns vehicles to flights.
  - `VehicleRouteCalculator.java`: Calculates vehicle routes.
  - `ProximityAssignmentService.java`: Assigns vehicles based on proximity.

- **asset/**
  - `AssetDataGenerator.java`: Generates equipment and tools.

- **turnaround/**
  - `TurnaroundDataGenerator.java`: Simulates ground handling sessions and tasks.
  - `TurnaroundMilestoneGenerator.java`: Generates key timing events.
  - `TurnaroundCorrelator.java`: Correlates turnaround events.

- **alert/**
  - `AlertDataGenerator.java`: Generates alerts for delays and SLA breaches.
  - `AlertEnricher.java`: Adds context and severity to alerts.
  - `CascadeAnalyzer.java`: Analyzes network cascade impacts.
  - `FinancialImpactCalculator.java`: Calculates financial impact of delays.
  - `FinancialMetricGenerator.java`: Generates cost/savings metrics.

- **security/**
  - `ZoneDataGenerator.java`: Generates restricted zones.
  - `ViolationDataGenerator.java`: Simulates zone breaches.
  - `MovementDiscrepancyGenerator.java`: Tracks movement anomalies.
  - `BoundaryValidator.java`: Validates positions against airport boundaries.

- **tracking/**
  - `MovementTrailGenerator.java`: Generates rolling position history for assets.
  - `LocationRegisterUpdater.java`: Maintains live asset positions.

- **reference/**
  - `StandDataGenerator.java`: Generates aircraft parking positions.
  - `DepotDataGenerator.java`: Generates vehicle storage areas.
  - `VehicleTypeDataGenerator.java`: Defines GSE vehicle types.
  - `BoundaryDataGenerator.java`: Generates airport perimeter polygons.

- **financial/**
  - `FinancialDataGenerator.java`: Generates financial metrics.

- **dispatch/**
  - `DispatchDataGenerator.java`: Generates work orders for vehicles.

- **retention/**
  - `DataRetentionService.java`: Cleans up old simulation data.

## API Endpoints

- `/api/admin/generators/status`: Get generator statuses.
- `/api/admin/generators/batch/start`: Start batch population.
- `/api/admin/generators/continuous/start`: Start real-time simulation.
- `/api/admin/generators/continuous/stop`: Stop real-time simulation.
- `/api/admin/generators/{type}/start`: Start a specific generator.
- `/api/admin/generators/{type}/stop`: Stop a specific generator.
- `/api/admin/generators/historical`: Generate historical data.
- `/api/admin/generators/trails/stats`: Get trail statistics.
- `/api/admin/generators/register/stats`: Get register statistics.

## Metrics

Metrics are exposed at `/actuator/prometheus` for monitoring and alerting.

---

This document provides a high-level overview of the simulation module, its structure, and the role of each package. For more details, see the README in the simulation folder or request specific class descriptions.

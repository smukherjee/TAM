# TAM Platform Deployment Guide

This guide provides comprehensive instructions for deploying and managing the TAM (Unified Tracking and Alerting Module) infrastructure.

## 🚀 One-Click Setup

The fastest way to get the entire platform (Infrastructure, Backend, and Frontend) running is using the unified setup script:

```bash
# From the project root
./infrastructure/setup-infrastructure.sh
```

This orchestrator handles:
1.  **Prerequisites Validation**: Checks for Docker, jq, curl, etc.
2.  **Service Launch**: Starts all containers via `docker-compose`.
3.  **Health Monitoring**: Waits for DB, Kafka, and MinIO availability.
4.  **Database Initialization**: Runs the full 01-15 SQL sequence.
5.  **NiFi Configuration**: Sets up ingestion, asset tracking, and monitoring flows.
6.  **Superset Provisioning**: Creates datasets, charts, and dashboards automatically.
7.  **Demo Data Seeding**: Populates the platform with initial simulate data.

---

## 🏗️ Technical Architecture

The TAM platform is composed of several integrated components:

-   **Frontend**: React-based dashboard ([http://localhost:3000](http://localhost:3000))
-   **Backend**: Spring Boot 3 Engine ([http://localhost:8080](http://localhost:8080))
-   **Database**: TimescaleDB (PostgreSQL) for time-series geospatial data.
-   **Ingestion**: Apache NiFi for multi-source data ingestion.
-   **Streaming**: Redpanda (Kafka) for real-time event distribution.
-   **Storage**: MinIO for raw data archival (Datalake).

---

## 🛠️ Management & Maintenance

A suite of consolidated scripts is available in the `./infrastructure` directory for ongoing management:

### 🔍 Verification & Testing
-   `./infrastructure/verify-deployment.sh`: Comprehensive health check of all components.
-   `./infrastructure/sanity-test.sh`: End-to-end data flow validation (Ingestion → Kafka → DB).

### 🔄 Data Management
-   `./infrastructure/seed-demo-data.sh`: CLI wrapper to generate simulation data for specific tenants.
-   `./infrastructure/provision-tenant.sh`: Dynamics onboarding for new airports (ICAO, Lat/Long).
-   `./infrastructure/backup-data.sh`: High-level backup of DB and MinIO files.
-   `./infrastructure/restore-data.sh`: Restore platform state from a backup package.

### 🩹 Repair & Reset
-   `./infrastructure/repair-infra.sh`: Automated fixes for common NiFi/Superset/DB issues.
-   `./infrastructure/reset-infra.sh`: Clean wipe of flows and simulation data for a fresh start.

---

## 👤 Credentials & Access

| Service | URL | Credentials (Default) |
| :--- | :--- | :--- |
| **Frontend** | [http://localhost:3000](http://localhost:3000) | `admin_vidp` / `admin` |
| **NiFi UI** | [http://localhost:8091/nifi](http://localhost:8091/nifi) | No Auth (Dev Mode) |
| **Superset** | [http://localhost:8089](http://localhost:8089) | `admin` / `admin` |
| **MinIO Admin** | [http://localhost:9001](http://localhost:9001) | `minioadmin` / `minioadmin` |
| **Redpanda Console** | [http://localhost:8090](http://localhost:8090) | No Auth |

---

## ❓ Troubleshooting

-   **NiFi Errors**: If flows are missing, run `./infrastructure/nifi/configure-nifi.sh`.
-   **Empty Charts**: If Superset charts are blank, ensure the backend is running and run `./infrastructure/seed-demo-data.sh --tenant all --sync`.
-   **Database Connectivity**: Check `docker-compose ps` to ensure the `timescaledb` container is healthy.

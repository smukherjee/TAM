# Complete TAM Platform Setup Guide

Follow these steps to build and start the TAM platform locally from scratch.

## Prerequisites
- Docker and Docker Compose installed
- Java 21+ and Maven
- Node.js (v18+) and npm

## Setup & Run

### Method 1: The Automated Pipeline (Recommended)
You can build the applications, boot up the infrastructure, seed the database, and configure all dashboards in a single automated step:

```bash
make rebuild-all
```
*(This will wipe any existing data, rebuild the applications, and spin up a fresh environment).*

### Method 2: Manual Pipeline
If you prefer running the steps individually, or just want to start existing services without deleting data:

1. **Build the Applications**
```bash
make build-all
```
2. **Start Infrastructure**
```bash
make start
```
3. **Configure Streaming & Reports** (Run after services are healthy)
```bash
make setup-nifi
make superset-provision
```

## Useful Commands
- `make status` - View the running Docker containers
- `make logs` - Tail logs from all platform services
- `make stop` - Turn off services
- `make reset` - ⚠️ Completely destroy all data and stop services

# TAM Platform - Development Makefile
# Commands to manage the development infrastructure

.PHONY: help dev-up dev-up-only dev-down dev-logs dev-ps backend-build frontend-build clean reset

# Default target
help:
	@echo "TAM Platform - Development Commands"
	@echo "===================================="
	@echo ""
	@echo "Infrastructure:"
	@echo "  make dev-up       - Start all dev services (Redpanda, NiFi, TimescaleDB, etc.)"
	@echo "  make setup-nifi   - Configure NiFi flows (Run after NiFi is ready)"
	@echo "  make dev-down     - Stop all services"
	@echo "  make dev-logs     - Tail logs from all services"
	@echo "  make dev-ps       - Show running containers"
	@echo ""
	@echo "Build:"
	@echo "  make backend-build   - Build backend JAR"
	@echo "  make frontend-build  - Build frontend dist"
	@echo "  make build-all       - Build everything"
	@echo ""
	@echo "Utils:"
	@echo "  make clean        - Remove build artifacts"
	@echo "  make reset        - Stop services and delete all volumes"
	@echo ""
	@echo "Service URLs:"
	@echo "  Frontend:         http://localhost:3000"
	@echo "  Backend API:      http://localhost:8080"
	@echo "  NiFi:             http://localhost:8091"
	@echo "  Redpanda Console: http://localhost:8090"
	@echo "  Superset:         http://localhost:8089"
	@echo "  Grafana:          http://localhost:3001"
	@echo "  Prometheus:       http://localhost:9090"
	@echo "  MinIO Console:    http://localhost:9001"

# ============================================
# Infrastructure Commands
# ============================================

dev-up:
	@bash infrastructure/dev-up-all.sh

dev-up-only:
	docker-compose -f docker-compose.dev.yml up -d
	@echo ""
	@echo "Services starting... Check with 'make dev-ps'"
	@echo "NiFi will take ~60 seconds to be ready"

dev-up-build:
	docker-compose -f docker-compose.dev.yml up -d --build

setup-nifi:
	@echo "Configuring NiFi flows..."
	@bash infrastructure/nifi/setup-nifi.sh

dev-down:
	docker-compose -f docker-compose.dev.yml down

dev-logs:
	docker-compose -f docker-compose.dev.yml logs -f

dev-logs-backend:
	docker-compose -f docker-compose.dev.yml logs -f backend

dev-logs-nifi:
	docker-compose -f docker-compose.dev.yml logs -f nifi

dev-ps:
	docker-compose -f docker-compose.dev.yml ps

# Superset provisioning
superset-provision:
	bash infrastructure/superset/create-all-reports.sh

superset-bootstrap:
	bash infrastructure/superset/bootstrap-superset.sh

superset-purge:
	bash infrastructure/superset/purge-all-reports.sh

superset-reprovision:
	$(MAKE) superset-purge && $(MAKE) superset-provision

superset-dedupe:
	bash infrastructure/superset/dedupe-charts.sh

# Start only infrastructure (no app build)
infra-up:
	docker-compose -f docker-compose.dev.yml up -d redpanda redpanda-console nifi timescaledb minio minio-init redis prometheus grafana loki

# ============================================
# Build Commands
# ============================================

backend-build:
	cd backend && mvn clean package -DskipTests

frontend-build:
	cd frontend && npm install  --legacy-peer-deps && npm run build

build-all: backend-build frontend-build
	@echo "Build complete!"

# ============================================
# Utility Commands
# ============================================

clean:
	cd backend && mvn clean
	cd frontend && rm -rf node_modules dist

reset:
	docker-compose -f docker-compose.dev.yml down -v
	@echo "All containers stopped and volumes deleted"

# Reset only the database (preserves other services)
db-reset:
	docker-compose -f docker-compose.dev.yml stop timescaledb
	docker-compose -f docker-compose.dev.yml rm -f -v timescaledb
	docker volume rm tam-core-refactor_timescaledb_data || docker volume rm tam_timescaledb_data || true
	docker-compose -f docker-compose.dev.yml up -d timescaledb
	@echo "Database reset and re-initializing..."

# Run SchemaSpy to generate DB documentation
schema-audit:
	@echo "Generating Database Schema Documentation..."
	mkdir -p documentations/schemaspy
	docker-compose -f docker-compose.dev.yml --profile tools up schemaspy
	@echo "Documentation generated at: documentations/schemaspy/index.html"

# Show Kafka topics
topics:
	docker-compose -f docker-compose.dev.yml exec redpanda rpk topic list

# Create a test message
test-flight:
	docker-compose -f docker-compose.dev.yml exec redpanda rpk topic produce flight-raw-avro <<< '[{"flightNumber":"AI123","latitude":28.5,"longitude":77.1,"altitude":35000,"speed":450}]'

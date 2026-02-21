# TAM Platform - Development Makefile
# Commands to manage the development infrastructure

.PHONY: help rebuild-all setup-all reset start stop logs status clean

# Default target
help:
	@echo "TAM Platform - Development Commands"
	@echo "===================================="
	@echo ""
	@echo "Complete Pipelines:"
	@echo "  make rebuild-all   - [START HERE] Full reset, build applications, and seed databases"
	@echo "  make setup-all     - Build applications and initialize services (without deleting data)"
	@echo ""
	@echo "Lifecycle:"
	@echo "  make start         - Start all services (must have run build/setup first)"
	@echo "  make stop          - Stop all services"
	@echo "  make reset         - WARNING: Stops services and DELETES all local databases/volumes"
	@echo ""
	@echo "Monitoring:"
	@echo "  make status        - Show running containers"
	@echo "  make logs          - Tail logs from all services"
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
# Complete Pipelines
# ============================================

rebuild-all: reset setup-all
	@echo "Full rebuild pipeline complete!"

setup-all: build-all start
	@echo "Wait for Infrastructure initialization (60 seconds)..."
	@sleep 60
	@bash infrastructure/nifi/setup-nifi.sh
	@echo "Wait for Reporting BI initialization (15 seconds)..."
	@sleep 15
	@bash infrastructure/superset/create-all-reports.sh
	@echo "Platform setup complete! Navigate to http://localhost:3000"

build-all: 
	cd backend && mvn clean package -DskipTests
	cd frontend && npm install --legacy-peer-deps && npm run build
	@echo "Build complete!"

# ============================================
# Lifecycle Commands
# ============================================

start:
	docker-compose -f docker-compose.dev.yml up -d
	@echo "Services starting... Check with 'make status'"

stop:
	docker-compose -f docker-compose.dev.yml stop

reset:
	docker-compose -f docker-compose.dev.yml down -v
	@echo "All containers stopped and volumes deleted (database reset)"

# ============================================
# Monitoring
# ============================================

logs:
	docker-compose -f docker-compose.dev.yml logs -f

status:
	docker-compose -f docker-compose.dev.yml ps

clean:
	cd backend && mvn clean
	cd frontend && rm -rf node_modules dist
	@echo "Local workspaces cleaned."

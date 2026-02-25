# TAM Platform - Development Makefile
# Commands to manage the development infrastructure

.PHONY: help rebuild-all setup-all reset start stop logs status clean deploy-frontend deploy-backend wait-backend wait-superset wait-nifi setup-nifi-flows provision-superset generate-fresh-data

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
	@echo "Data/BI Utilities:"
	@echo "  make provision-superset - Create/update Superset datasets, charts, dashboards"
	@echo "  make generate-fresh-data - Run generator-backed fresh data population for VIDP/LIRN/YBBN"
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
	@echo "Waiting for core services to become ready..."
	@$(MAKE) wait-backend
	@$(MAKE) wait-superset
	@$(MAKE) setup-nifi-flows
	@$(MAKE) provision-superset
	@$(MAKE) generate-fresh-data
	@echo "Platform setup complete! Navigate to http://localhost:3000"

build-all: 
	cd backend && mvn clean package -DskipTests
	cd frontend && npm install --legacy-peer-deps && npm run build
	@echo "Build complete!"

deploy-frontend:
	cd frontend && npm install --legacy-peer-deps && npm run build
	docker-compose -f docker-compose.dev.yml up -d frontend
	@echo "Frontend deployed!"

deploy-backend:
	cd backend && mvn clean package -DskipTests
	docker-compose -f docker-compose.dev.yml up -d backend
	@echo "Backend deployed!"

# ============================================
# Lifecycle Commands
# ============================================

start:
	docker-compose -f docker-compose.dev.yml build backend frontend
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

wait-backend:
	@echo "Waiting for Backend API at http://127.0.0.1:8080/api/actuator/health ..."
	@attempts=90; \
	missing_limit=30; \
	for i in $$(seq 1 $$attempts); do \
		cid=$$(docker-compose -f docker-compose.dev.yml ps -q backend 2>/dev/null || true); \
		if [ -z "$$cid" ]; then \
			if [ $$i -ge $$missing_limit ]; then \
				echo "Backend container was not created within $$((missing_limit*2)) seconds."; \
				docker-compose -f docker-compose.dev.yml ps backend || true; \
				exit 1; \
			fi; \
			if [ $$((i % 5)) -eq 0 ]; then \
				echo "Waiting for backend container to be created ($$i/$$missing_limit)..."; \
			fi; \
			sleep 2; \
			continue; \
		fi; \
		state=$$(docker inspect -f '{{.State.Status}}' "$$cid" 2>/dev/null || echo "unknown"); \
		health=$$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$$cid" 2>/dev/null || echo "unknown"); \
		if [ "$$state" = "exited" ] || [ "$$state" = "dead" ]; then \
			echo "Backend container is $$state. Failing fast."; \
			docker-compose -f docker-compose.dev.yml ps backend || true; \
			docker-compose -f docker-compose.dev.yml logs --tail=50 backend || true; \
			exit 1; \
		fi; \
		if [ "$$health" = "unhealthy" ]; then \
			echo "Backend container is unhealthy. Failing fast."; \
			docker-compose -f docker-compose.dev.yml ps backend || true; \
			docker-compose -f docker-compose.dev.yml logs --tail=50 backend || true; \
			exit 1; \
		fi; \
		resp=$$(curl --silent --show-error --fail --connect-timeout 1 --max-time 2 http://127.0.0.1:8080/api/actuator/health 2>/dev/null || true); \
		if echo "$$resp" | grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"'; then \
			echo "Backend API is ready."; \
			exit 0; \
		fi; \
		if [ $$((i % 10)) -eq 0 ]; then \
			echo "Still waiting for backend ($$i/$$attempts)..."; \
		fi; \
		sleep 2; \
	done; \
	echo "Backend API not ready after 180 seconds."; \
	docker-compose -f docker-compose.dev.yml ps backend || true; \
	exit 1

wait-superset:
	@echo "Waiting for Superset at http://127.0.0.1:8089/health ..."
	@attempts=90; \
	missing_limit=30; \
	for i in $$(seq 1 $$attempts); do \
		cid=$$(docker-compose -f docker-compose.dev.yml ps -q superset 2>/dev/null || true); \
		if [ -z "$$cid" ]; then \
			if [ $$i -ge $$missing_limit ]; then \
				echo "Superset container was not created within $$((missing_limit*2)) seconds."; \
				docker-compose -f docker-compose.dev.yml ps superset || true; \
				exit 1; \
			fi; \
			if [ $$((i % 5)) -eq 0 ]; then \
				echo "Waiting for superset container to be created ($$i/$$missing_limit)..."; \
			fi; \
			sleep 2; \
			continue; \
		fi; \
		state=$$(docker inspect -f '{{.State.Status}}' "$$cid" 2>/dev/null || echo "unknown"); \
		health=$$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$$cid" 2>/dev/null || echo "unknown"); \
		if [ "$$state" = "exited" ] || [ "$$state" = "dead" ]; then \
			echo "Superset container is $$state. Failing fast."; \
			docker-compose -f docker-compose.dev.yml ps superset || true; \
			docker-compose -f docker-compose.dev.yml logs --tail=50 superset || true; \
			exit 1; \
		fi; \
		if [ "$$health" = "unhealthy" ]; then \
			echo "Superset container is unhealthy. Failing fast."; \
			docker-compose -f docker-compose.dev.yml ps superset || true; \
			docker-compose -f docker-compose.dev.yml logs --tail=50 superset || true; \
			exit 1; \
		fi; \
		resp=$$(curl --silent --show-error --fail --connect-timeout 1 --max-time 2 http://127.0.0.1:8089/health 2>/dev/null || true); \
		if echo "$$resp" | grep -Eiq "healthy|ok"; then \
			echo "Superset is ready."; \
			exit 0; \
		fi; \
		if [ $$((i % 10)) -eq 0 ]; then \
			echo "Still waiting for superset ($$i/$$attempts)..."; \
		fi; \
		sleep 2; \
	done; \
	echo "Superset not ready after 180 seconds."; \
	docker-compose -f docker-compose.dev.yml ps superset || true; \
	exit 1

wait-nifi:
	@echo "Waiting for NiFi API at http://127.0.0.1:8091/nifi-api/flow/about ..."
	@attempts=120; \
	missing_limit=30; \
	for i in $$(seq 1 $$attempts); do \
		cid=$$(docker-compose -f docker-compose.dev.yml ps -q nifi 2>/dev/null || true); \
		if [ -z "$$cid" ]; then \
			if [ $$i -ge $$missing_limit ]; then \
				echo "NiFi container was not created within $$((missing_limit*2)) seconds."; \
				docker-compose -f docker-compose.dev.yml ps nifi || true; \
				exit 1; \
			fi; \
			if [ $$((i % 5)) -eq 0 ]; then \
				echo "Waiting for NiFi container to be created ($$i/$$missing_limit)..."; \
			fi; \
			sleep 2; \
			continue; \
		fi; \
		state=$$(docker inspect -f '{{.State.Status}}' "$$cid" 2>/dev/null || echo "unknown"); \
		health=$$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$$cid" 2>/dev/null || echo "unknown"); \
		if [ "$$state" = "exited" ] || [ "$$state" = "dead" ]; then \
			echo "NiFi container is $$state. Failing fast."; \
			docker-compose -f docker-compose.dev.yml ps nifi || true; \
			docker-compose -f docker-compose.dev.yml logs --tail=80 nifi || true; \
			exit 1; \
		fi; \
		if [ "$$health" = "unhealthy" ]; then \
			echo "NiFi container is unhealthy. Failing fast."; \
			docker-compose -f docker-compose.dev.yml ps nifi || true; \
			docker-compose -f docker-compose.dev.yml logs --tail=80 nifi || true; \
			exit 1; \
		fi; \
		resp=$$(curl --silent --show-error --fail --connect-timeout 1 --max-time 3 http://127.0.0.1:8091/nifi-api/flow/about 2>/dev/null || true); \
		if echo "$$resp" | grep -q '"title":"NiFi"'; then \
			echo "NiFi API is ready."; \
			exit 0; \
		fi; \
		if [ $$((i % 10)) -eq 0 ]; then \
			echo "Still waiting for NiFi API ($$i/$$attempts)..."; \
		fi; \
		sleep 2; \
	done; \
	echo "NiFi API not ready after 240 seconds."; \
	docker-compose -f docker-compose.dev.yml ps nifi || true; \
	exit 1

setup-nifi-flows: wait-nifi
	@echo "Configuring NiFi flows..."
	@attempts=3; \
	for i in $$(seq 1 $$attempts); do \
		if bash infrastructure/nifi/setup-nifi.sh; then \
			echo "NiFi flow setup completed."; \
			exit 0; \
		fi; \
		echo "NiFi flow setup attempt $$i/$$attempts failed; retrying..."; \
		sleep 3; \
	done; \
	echo "NiFi flow setup failed after $$attempts attempts."; \
	exit 1

provision-superset: wait-superset
	@echo "Provisioning Superset reports..."
	@attempts=3; \
	for i in $$(seq 1 $$attempts); do \
		if bash infrastructure/superset/create-all-reports.sh; then \
			echo "Superset provisioning completed."; \
			exit 0; \
		fi; \
		echo "Superset provisioning attempt $$i/$$attempts failed; retrying..."; \
		sleep 3; \
	done; \
	echo "Superset provisioning failed after $$attempts attempts."; \
	exit 1

generate-fresh-data: wait-backend
	@echo "Generating fresh simulation/report data for VIDP, LIRN, YBBN..."
	@bash infrastructure/scripts/generate_fresh_data.sh VIDP LIRN YBBN --batch-size 100 --days 7

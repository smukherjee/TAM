#!/bin/bash
# TAM Unified Database Initialization Script
# Consolidates execution of all 01-15 SQL scripts

set -e

# Configuration
DB_CONTAINER="tam-timescaledb-1"
DB_NAME="utam"
DB_USER="postgres"
INIT_DIR="infrastructure/db/init"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}"
}

error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR: $1${NC}"
}

# 1. Pre-flight checks
if ! command -v docker &> /dev/null; then
    error "docker is not installed."
    exit 1
fi

# 2. Wait for TimescaleDB to be ready
log "Waiting for TimescaleDB to be ready..."
MAX_RETRIES=30
RETRY_COUNT=0
while ! docker exec "$DB_CONTAINER" pg_isready -U "$DB_USER" -d "$DB_NAME" &> /dev/null; do
    RETRY_COUNT=$((RETRY_COUNT + 1))
    if [ $RETRY_COUNT -ge $MAX_RETRIES ]; then
        error "Timed out waiting for database."
        exit 1
    fi
    sleep 2
done
log "Database is ready!"

# 3. Execute SQL scripts in order
log "Starting database initialization (01-15)..."

for sql_file in $(ls "$INIT_DIR"/*.sql | sort); do
    filename=$(basename "$sql_file")
    log "Executing $filename..."
    
    # Use docker exec to pipe the file content into psql
    if ! docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 < "$sql_file" > /dev/null; then
        error "Failed to execute $filename"
        exit 1
    fi
done

# 4. Final Validation
log "Verifying initialization..."
TABLE_COUNT=$(docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT count(*) FROM pg_tables WHERE schemaname = 'public';")
log "Total tables created: $TABLE_COUNT"

TENANT_COUNT=$(docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT count(*) FROM tenants;")
log "Default tenants seeded: $TENANT_COUNT"

log "Database initialization completed successfully! ✅"

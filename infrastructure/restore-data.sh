#!/bin/bash
# TAM Platform: Production Data Restore Utility
# Restores TimescaleDB and MinIO data from a backup package

set -e

# Configuration
DB_CONTAINER="tam-timescaledb-1"
DB_NAME="utam"
DB_USER="postgres"
RESTORE_TMP="./restore_tmp"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

log() { echo -e "${BLUE}[INF]${NC} $1"; }
success() { echo -e "${GREEN}[OK]${NC} $1"; }
error() { echo -e "${RED}[ERR]${NC} $1"; }

usage() {
    echo "Usage: $0 [backup_file.tar.gz]"
    exit 1
}

BACKUP_FILE=$1
if [ -z "$BACKUP_FILE" ]; then usage; fi
if [ ! -f "$BACKUP_FILE" ]; then error "Backup file not found: $BACKUP_FILE"; exit 1; fi

log "Starting restore from: $BACKUP_FILE"

# 1. Extract backup
mkdir -p "$RESTORE_TMP"
tar -xzf "$BACKUP_FILE" -C "$RESTORE_TMP"
# Find the actual data dir (might be nested)
DATA_DIR=$(find "$RESTORE_TMP" -maxdepth 2 -type d -name "tam_backup_*" | head -n 1)

if [ -z "$DATA_DIR" ]; then
    error "Invalid backup package format."
    rm -rf "$RESTORE_TMP"
    exit 1
fi

# 2. Restore Database
if [ -f "$DATA_DIR/db_dump.sql.gz" ]; then
    log "Restoring TimescaleDB..."
    # Terminate existing connections
    docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$DB_NAME' AND pid <> pg_backend_pid();" >/dev/null
    
    # Drop and recreate DB (standard psql way for fresh restore)
    # Note: Using piping to avoid interactive prompts
    gunzip -c "$DATA_DIR/db_dump.sql.gz" | docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" > /dev/null
    success "Database restore complete."
fi

# 3. Restore MinIO
if [ -d "$DATA_DIR/minio_data" ]; then
    log "Restoring MinIO data..."
    docker cp "$DATA_DIR/minio_data/." tam-minio-1:/data/
    success "MinIO restore complete."
fi

# Cleanup
rm -rf "$RESTORE_TMP"
success "Restore completed successfully! ✅"

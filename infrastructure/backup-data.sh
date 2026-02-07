#!/bin/bash
# TAM Platform: Production Data Backup Utility
# Backs up TimescaleDB and MinIO data

set -e

# Configuration
DB_CONTAINER="tam-timescaledb-1"
DB_NAME="utam"
DB_USER="postgres"
BACKUP_DIR="./backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_NAME="tam_backup_$TIMESTAMP"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

log() { echo -e "${BLUE}[INF]${NC} $1"; }
success() { echo -e "${GREEN}[OK]${NC} $1"; }

mkdir -p "$BACKUP_DIR/$BACKUP_NAME"

log "Starting backup: $BACKUP_NAME"

# 1. Database Backup (PostgreSQL)
log "Backing up TimescaleDB..."
docker exec "$DB_CONTAINER" pg_dump -U "$DB_USER" "$DB_NAME" | gzip > "$BACKUP_DIR/$BACKUP_NAME/db_dump.sql.gz"
success "Database backup complete."

# 2. MinIO Backup (Raw Data)
log "Backing up MinIO raw data..."
# Use docker cp to get data directory
if docker cp tam-minio-1:/data/. "$BACKUP_DIR/$BACKUP_NAME/minio_data/" 2>/dev/null; then
    success "MinIO backup complete."
else
    log "MinIO data directory not found or empty. Skipping."
fi

# 3. Compress the whole backup
log "Compressing backup package..."
tar -czf "$BACKUP_DIR/$BACKUP_NAME.tar.gz" -C "$BACKUP_DIR" "$BACKUP_NAME"
rm -rf "$BACKUP_DIR/$BACKUP_NAME"

success "Full backup complete: $BACKUP_DIR/$BACKUP_NAME.tar.gz ✅"

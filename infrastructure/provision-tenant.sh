#!/bin/bash
# TAM Dynamic Airport Provisioning Script
# Allows adding new tenants/airports with custom parameters

set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

# Default values
DB_CONTAINER="tam-timescaledb-1"
DB_NAME="utam"
DB_USER="postgres"

usage() {
    echo "Usage: $0 [options]"
    echo "Options:"
    echo "  --icao ICAO      4-letter airport code (required, e.g. KJFK)"
    echo "  --name NAME      Full airport name (required)"
    echo "  --lat LAT        Latitude of airport center (required)"
    echo "  --lon LON        Longitude of airport center (required)"
    echo "  --prefix PREFIX  Prefix for vehicle/asset IDs (optional, defaults to ICAO)"
    exit 1
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --icao) ICAO="$2"; shift 2 ;;
        --name) NAME="$2"; shift 2 ;;
        --lat) LAT="$2"; shift 2 ;;
        --lon) LON="$2"; shift 2 ;;
        --prefix) PREFIX="$2"; shift 2 ;;
        *) usage ;;
    esac
done

if [[ -z "$ICAO" || -z "$NAME" || -z "$LAT" || -z "$LON" ]]; then
    usage
fi

PREFIX=${PREFIX:-$ICAO}

log() { echo -e "${BLUE}[INF]${NC} $1"; }
success() { echo -e "${GREEN}[OK]${NC} $1"; }
error() { echo -e "${RED}[ERR]${NC} $1"; }

log "Provisioning new airport: $NAME ($ICAO)"
log "Coordinates: $LAT, $LON"
log "Prefix: $PREFIX"

# 1. Create SQL for the new tenant
SQL_TMP=$(mktemp)

cat <<EOF > "$SQL_TMP"
-- Provisioning for $ICAO
BEGIN;

-- 1. Tenant
INSERT INTO tenants (code, name, timezone) 
VALUES ('$ICAO', '$NAME', 'UTC')
ON CONFLICT (code) DO NOTHING;

-- 2. Default Users
INSERT INTO users (username, password, role, tenant_code) VALUES
('admin_${ICAO,,}', 'admin', 'ADMIN', '$ICAO'),
('user_${ICAO,,}', 'user', 'AIRPORT_USER', '$ICAO')
ON CONFLICT (username) DO NOTHING;

-- 3. Default Vehicle Types
INSERT INTO vehicle_types (tenant_code, code, name, category, max_speed, icon_name, icon_color) VALUES
('$ICAO', 'BAGGAGE', 'Baggage Tractor', 'BAGGAGE', 25, 'truck', '#4A90D9'),
('$ICAO', 'FUEL', 'Fuel Bowser', 'FUEL', 30, 'fuel', '#F5A623'),
('$ICAO', 'PUSHBACK', 'Pushback Tug', 'PUSHBACK', 20, 'pushback', '#E74C3C'),
('$ICAO', 'BUS', 'Passenger Bus', 'PASSENGER', 40, 'bus', '#3498DB')
ON CONFLICT (tenant_code, code) DO NOTHING;

-- 4. Sample Zones (around center)
INSERT INTO zones (tenant_code, code, name, type, color, opacity, active) VALUES
('$ICAO', 'APRON-1', 'Main Apron', 'APRON', '#3B82F6', 0.3, true),
('$ICAO', 'T1', 'Terminal 1', 'TERMINAL', '#10B981', 0.3, true)
ON CONFLICT (tenant_code, code) DO NOTHING;

-- 5. Airport Boundary (0.05 deg box approx 5km)
INSERT INTO airport_boundaries (tenant_code, min_latitude, max_latitude, min_longitude, max_longitude)
VALUES ('$ICAO', $LAT - 0.02, $LAT + 0.02, $LON - 0.02, $LON + 0.02)
ON CONFLICT (tenant_code) DO NOTHING;

-- 6. Sample Assets
INSERT INTO assets (asset_id, name, status, category, tenant_code, location) VALUES
('$PREFIX-AS-001', '$NAME Fire Truck', 'Available', 'Emergency', '$ICAO', 'Station 1'),
('$PREFIX-AS-002', '$NAME Fuel Truck', 'Available', 'Fueling', '$ICAO', 'Apron')
ON CONFLICT DO NOTHING;

COMMIT;
EOF

log "Executing provisioning SQL..."
if docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 < "$SQL_TMP" > /dev/null; then
    success "Airport $ICAO provisioned successfully! ✅"
    success "Admin user: admin_${ICAO,,} / admin"
else
    error "Provisioning failed."
    rm "$SQL_TMP"
    exit 1
fi

rm "$SQL_TMP"

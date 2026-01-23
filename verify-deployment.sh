#!/bin/bash
# TAM Platform - Fresh Start Verification Script
# Run this after: docker-compose -f docker-compose.dev.yml up -d

set -e

echo "🔍 TAM Platform - Post-Start Verification"
echo "=========================================="
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Helper functions
check_service() {
    local service=$1
    local port=$2
    
    if docker ps | grep -q "$service"; then
        echo -e "${GREEN}✓${NC} $service is running"
        return 0
    else
        echo -e "${RED}✗${NC} $service is NOT running"
        return 1
    fi
}

check_port() {
    local service=$1
    local port=$2
    
    if nc -z localhost $port 2>/dev/null; then
        echo -e "${GREEN}✓${NC} $service port $port is accessible"
        return 0
    else
        echo -e "${YELLOW}⚠${NC} $service port $port is not ready yet"
        return 1
    fi
}

check_db_data() {
    echo ""
    echo "📊 Database Statistics:"
    echo "----------------------"
    
    # Check if database has data
    FLIGHT_COUNT=$(docker exec tam-timescaledb-1 psql -U postgres -d utam -t -c \
        "SELECT COUNT(*) FROM flights;" 2>/dev/null | tr -d ' ')
    
    if [ -n "$FLIGHT_COUNT" ] && [ "$FLIGHT_COUNT" -gt 0 ]; then
        echo -e "${GREEN}✓${NC} Flights in database: $FLIGHT_COUNT"
        
        # Show breakdown by tenant
        docker exec tam-timescaledb-1 psql -U postgres -d utam -c \
            "SELECT tenant_code, COUNT(*) as count FROM flights GROUP BY tenant_code ORDER BY tenant_code;"
    else
        echo -e "${YELLOW}⚠${NC} No flights in database yet (wait 30 seconds after fresh start)"
    fi
}

check_nifi_ports() {
    echo ""
    echo "🔌 NiFi ListenHTTP Ports:"
    echo "------------------------"
    
    for port in 8092 8093 8094; do
        if netstat -an | grep -q "LISTEN.*:$port"; then
            echo -e "${GREEN}✓${NC} Port $port is listening"
        else
            echo -e "${RED}✗${NC} Port $port is NOT listening - run infrastructure/nifi/setup-nifi.sh"
        fi
    done
}

test_nifi_http() {
    echo ""
    echo "🧪 Testing NiFi HTTP Endpoints:"
    echo "-------------------------------"
    
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
        http://localhost:8092/adsb-ingest \
        -H "Content-Type: application/json" \
        -d '[{"test":"data"}]' 2>/dev/null)
    
    if [ "$HTTP_CODE" = "200" ]; then
        echo -e "${GREEN}✓${NC} NiFi HTTP endpoint returns 200"
    elif [ "$HTTP_CODE" = "503" ]; then
        echo -e "${RED}✗${NC} NiFi returns 503 - flows not started. Run: infrastructure/nifi/setup-nifi.sh"
    else
        echo -e "${YELLOW}⚠${NC} NiFi returns $HTTP_CODE"
    fi
}

test_backend_api() {
    echo ""
    echo "🌐 Testing Backend API:"
    echo "----------------------"
    
    FLIGHT_COUNT=$(curl -s -H "X-User-ICAO: YBBN" http://localhost:8080/api/flights 2>/dev/null | \
        jq '.data | length' 2>/dev/null)
    
    if [ -n "$FLIGHT_COUNT" ]; then
        echo -e "${GREEN}✓${NC} Backend API returns $FLIGHT_COUNT active flights"
    else
        echo -e "${YELLOW}⚠${NC} Backend API not responding yet"
    fi
}

# Main verification
echo "1️⃣  Checking Docker Services..."
echo "--------------------------------"
check_service "tam-timescaledb-1" "5432"
check_service "tam-redpanda-1" "9092"
check_service "tam-nifi-1" "8091"
check_service "tam-backend-1" "8080"
check_service "tam-redis-1" "6379"
check_service "tam-minio-1" "9000"

echo ""
echo "2️⃣  Checking Service Ports..."
echo "------------------------------"
check_port "TimescaleDB" "5432"
check_port "Redpanda" "9092"
check_port "NiFi Web UI" "8091"
check_port "Backend API" "8080"
check_port "Redpanda Console" "8090"

check_nifi_ports
test_nifi_http
check_db_data
test_backend_api

echo ""
echo "=========================================="
echo "📋 Summary:"
echo "=========================================="
echo ""
echo "Frontend:          http://localhost:3000"
echo "Backend API:       http://localhost:8080"
echo "NiFi UI:           http://localhost:8091 (admin/admin123456789)"
echo "Redpanda Console:  http://localhost:8090"
echo "MinIO Console:     http://localhost:9001 (minioadmin/minioadmin)"
echo ""
echo "Login Credentials:"
echo "  YBBN: admin_ybbn / admin123"
echo "  LIRN: admin_lirn / admin123"
echo "  VIDP: admin_vidp / admin123"
echo ""
echo -e "${YELLOW}NOTE:${NC} If NiFi ports are not listening, run:"
echo "  cd infrastructure/nifi && ./setup-nifi.sh"
echo ""

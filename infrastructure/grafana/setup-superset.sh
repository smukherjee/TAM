#!/bin/bash
# Setup Superset Database Connection

set -e
SUPERSET_URL="http://localhost:8089"

echo "📊 Configuring Superset..."

# 1. Login
echo "   Logging in..."
TOKEN=$(curl -s -X POST "$SUPERSET_URL/api/v1/security/login" \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin", "provider": "db"}' | jq -r '.access_token')

if [ -z "$TOKEN" ] || [ "$TOKEN" == "null" ]; then
    echo "   ❌ Failed to login to Superset. Check if it's running."
    exit 1
fi
echo "   ✅ Logged in"

# 2. Add Database (TimescaleDB)
echo "   Adding TimescaleDB connection..."
# Check if exists first? Hard via API to filter by name effortlessly, but POST will fail or return existing ID.
# Actually we can list databases.
DB_EXISTS=$(curl -s -X GET "$SUPERSET_URL/api/v1/database/" \
  -H "Authorization: Bearer $TOKEN" | jq -r '.result[] | select(.database_name == "TimescaleDB") | .id')

if [ -n "$DB_EXISTS" ]; then
    echo "   ✅ Database 'TimescaleDB' already exists (ID: $DB_EXISTS)"
else
    # Create
    RESP=$(curl -s -X POST "$SUPERSET_URL/api/v1/database/" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d '{
        "database_name": "TimescaleDB",
        "sqlalchemy_uri": "postgresql://postgres:password@tam-timescaledb:5432/utam",
        "expose_in_sqllab": true
      }')
      
    ID=$(echo "$RESP" | jq -r '.id')
    if [ -n "$ID" ] && [ "$ID" != "null" ]; then
        echo "   ✅ Added 'TimescaleDB' (ID: $ID)"
    else
        echo "   ⚠️ Failed to add database. Response: $RESP"
    fi
fi

echo "🎉 Superset configured!"

#!/bin/bash
# Fix Dashboard Slug for embedding

set -e
SUPERSET_URL="http://localhost:8089"

echo "🔧 Fixing Dashboard Slug..."

# 1. Login
TOKEN=$(curl -s -X POST "$SUPERSET_URL/api/v1/security/login" \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin", "provider": "db"}' | jq -r '.access_token')

if [ -z "$TOKEN" ] || [ "$TOKEN" == "null" ]; then
    echo "   ❌ Failed to login."
    exit 1
fi
AUTH_HEADER="Authorization: Bearer $TOKEN"

# 2. Find Dashboard
# Get list, filter by title
DASH_ID=$(curl -s -X GET "$SUPERSET_URL/api/v1/dashboard/?q=(filters:!((col:dashboard_title,opr:eq,value:'TAM%20Operations%20Dashboard')))" \
  -H "$AUTH_HEADER" | jq -r '.result[0].id')

if [ -z "$DASH_ID" ] || [ "$DASH_ID" == "null" ]; then
    echo "   ❌ Dashboard not found."
    exit 1
fi
echo "   ✅ Found Dashboard ID: $DASH_ID"

# 3. Update Slug
RESP=$(curl -s -X PUT "$SUPERSET_URL/api/v1/dashboard/$DASH_ID" \
    -H "$AUTH_HEADER" \
    -H "Content-Type: application/json" \
    -d '{
        "slug": "tam_ops",
        "published": true
    }')

MSG=$(echo "$RESP" | jq -r '.message // empty')
if [ -n "$MSG" ]; then
    # If slug already exists, maybe it belongs to THIS dashboard or another?
    # If it belongs to this one, update might succeed or complain.
    # If "Slug already exists", we are good if it's this one.
    echo "   ⚠️ Update Message: $MSG"
else
    echo "   ✅ Updated Slug to 'tam_ops'"
fi

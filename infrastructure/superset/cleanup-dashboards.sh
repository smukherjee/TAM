#!/bin/bash
set -e
SUPERSET_URL="http://localhost:8089"
echo "🧹 Cleaning up Superset Dashboards..."

TOKEN=$(curl -s -X POST "$SUPERSET_URL/api/v1/security/login" -H "Content-Type: application/json" -d '{"username": "admin", "password": "admin", "provider": "db"}' | jq -r '.access_token')
AUTH_HEADER="Authorization: Bearer $TOKEN"

delete_dashboard() {
    local SLUG=$1
    echo "   Checking dashboard: $SLUG"
    ID=$(curl -s -X GET "$SUPERSET_URL/api/v1/dashboard/?q=(filters:!((col:slug,opr:eq,value:$SLUG)))" -H "$AUTH_HEADER" | jq -r ".result[] | select(.slug == \"$SLUG\") | .id")
    
    if [ -n "$ID" ]; then
        echo "   🗑️ Deleting dashboard $SLUG (ID: $ID)..."
        curl -s -X DELETE "$SUPERSET_URL/api/v1/dashboard/$ID" -H "$AUTH_HEADER"
        echo "   ✅ Deleted."
    else
        echo "   ⚠️ Dashboard $SLUG not found."
    fi
}

delete_dashboard "tam_ops"
delete_dashboard "tam_ops_vidp"
delete_dashboard "tam_ops_lirn"

echo "🎉 Cleanup complete."

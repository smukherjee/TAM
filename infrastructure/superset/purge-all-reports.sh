#!/usr/bin/env bash
# Purge all Superset dashboards, charts, and datasets
set -euo pipefail

SUPERSET_URL=${SUPERSET_URL:-"http://localhost:8089"}
ADMIN_USER=${ADMIN_USER:-admin}
ADMIN_PASS=${ADMIN_PASS:-admin}

echo "🧹 Purging Superset content at $SUPERSET_URL"

req() { curl -s "$@"; }

TOKEN=$(req -X POST "$SUPERSET_URL/api/v1/security/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\": \"$ADMIN_USER\", \"password\": \"$ADMIN_PASS\", \"provider\": \"db\"}" | jq -r '.access_token')

if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
  echo "❌ Superset login failed"; exit 1
fi
AUTH_HEADER="Authorization: Bearer $TOKEN"

# Helper to delete all items of a resource
delete_all() {
  local resource=$1
  local id_field=${2:-id}
  local list_url="$SUPERSET_URL/api/v1/$resource/?q=(page_size:1000)"
  echo "   → Listing $resource"
  local ids=$(req -X GET "$list_url" -H "$AUTH_HEADER" | jq -r ".result[] | .${id_field}")
  if [ -z "$ids" ]; then echo "   ↪ no $resource found"; return; fi
  while IFS= read -r id; do
    if [ -z "$id" ]; then continue; fi
    echo "   ✖ Deleting $resource $id"
    req -X DELETE "$SUPERSET_URL/api/v1/$resource/$id" -H "$AUTH_HEADER" >/dev/null 2>&1 || echo "     ⚠️ failed to delete $resource $id"
  done <<< "$ids"
}

# Delete in safe order: dashboards → charts → datasets
delete_all dashboard id
# Give Superset a moment to release relationships
sleep 1

delete_all chart id
sleep 1

delete_all dataset id

echo "✅ Purge complete"

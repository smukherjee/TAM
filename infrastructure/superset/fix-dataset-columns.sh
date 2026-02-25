#!/bin/bash
# Fix missing column metadata for Superset datasets 1-3
set -eo pipefail

SUPERSET_URL=${SUPERSET_URL:-"http://localhost:8089"}

echo "🔧 Fixing Superset dataset column metadata..."

# Login
LOGIN_CSRF=$(curl -s -c /tmp/fix_cookies.txt "$SUPERSET_URL/login/" | grep -o 'csrf_token" type="hidden" value="[^"]*' | cut -d'"' -f5 || echo "")
curl -s -b /tmp/fix_cookies.txt -c /tmp/fix_cookies.txt -X POST "$SUPERSET_URL/login/" \
  -d "username=admin&password=admin&csrf_token=$LOGIN_CSRF" > /dev/null

API_CSRF=$(curl -s -b /tmp/fix_cookies.txt "$SUPERSET_URL/api/v1/security/csrf_token/" | jq -r '.result')
if [ -z "$API_CSRF" ] || [ "$API_CSRF" == "null" ]; then
  echo "❌ Login failed"; exit 1
fi
echo "✅ Logged in"

fix_dataset() {
  local DS_ID=$1
  local COLS_JSON=$2
  local HTTP
  HTTP=$(curl -s -o /tmp/fix_ds_resp.json -w "%{http_code}" \
    -b /tmp/fix_cookies.txt -H "X-CSRFToken: $API_CSRF" -H "Content-Type: application/json" \
    -X PUT "$SUPERSET_URL/api/v1/dataset/$DS_ID" -d "{\"columns\": $COLS_JSON}")
  if [ "${HTTP:-500}" -ge 300 ]; then
    echo "  ❌ Failed (HTTP $HTTP): $(cat /tmp/fix_ds_resp.json)"
  else
    local NAME
    NAME=$(curl -s -b /tmp/fix_cookies.txt "$SUPERSET_URL/api/v1/dataset/$DS_ID" | jq -r '.result.table_name')
    local COUNT
    COUNT=$(curl -s -b /tmp/fix_cookies.txt "$SUPERSET_URL/api/v1/dataset/$DS_ID" | jq '.result.columns | length')
    echo "  ✅ $NAME → $COUNT columns"
  fi
}

echo ""
echo "Fixing dataset 1: v_ops_overview_daily"
fix_dataset 1 '[
  {"column_name":"tenant_code","type":"VARCHAR","is_dttm":false,"filterable":true,"groupby":true},
  {"column_name":"day","type":"TIMESTAMP WITHOUT TIME ZONE","is_dttm":true,"filterable":true,"groupby":true},
  {"column_name":"flights","type":"BIGINT","is_dttm":false,"filterable":true,"groupby":true},
  {"column_name":"vehicles","type":"BIGINT","is_dttm":false,"filterable":true,"groupby":true},
  {"column_name":"alerts","type":"BIGINT","is_dttm":false,"filterable":true,"groupby":true},
  {"column_name":"violations","type":"BIGINT","is_dttm":false,"filterable":true,"groupby":true}
]'

echo ""
echo "Fixing dataset 2: v_flight_movements_hourly"
fix_dataset 2 '[
  {"column_name":"tenant_code","type":"VARCHAR","is_dttm":false,"filterable":true,"groupby":true},
  {"column_name":"hour","type":"TIMESTAMP WITHOUT TIME ZONE","is_dttm":true,"filterable":true,"groupby":true},
  {"column_name":"positions","type":"BIGINT","is_dttm":false,"filterable":true,"groupby":true}
]'

echo ""
echo "Fixing dataset 3: v_vehicle_activity_summary_daily"
fix_dataset 3 '[
  {"column_name":"tenant_code","type":"VARCHAR","is_dttm":false,"filterable":true,"groupby":true},
  {"column_name":"day","type":"TIMESTAMP WITHOUT TIME ZONE","is_dttm":true,"filterable":true,"groupby":true},
  {"column_name":"vehicle_type","type":"VARCHAR","is_dttm":false,"filterable":true,"groupby":true},
  {"column_name":"telemetry_points","type":"BIGINT","is_dttm":false,"filterable":true,"groupby":true},
  {"column_name":"avg_speed","type":"DOUBLE PRECISION","is_dttm":false,"filterable":true,"groupby":true}
]'

echo ""
echo "🎉 Dataset column fix complete!"

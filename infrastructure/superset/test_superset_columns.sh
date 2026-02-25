#!/bin/bash
set -eo pipefail

SUPERSET_URL="http://localhost:8089"
ADMIN_USER="admin"
ADMIN_PASS="admin"

# Login
LOGIN_CSRF=$(curl -s -c cookies.txt "$SUPERSET_URL/login/" | grep -o 'csrf_token" type="hidden" value="[^"]*' | cut -d'"' -f5 || echo "")
curl -s -b cookies.txt -c cookies.txt -X POST "$SUPERSET_URL/login/" -d "username=$ADMIN_USER&password=$ADMIN_PASS&csrf_token=$LOGIN_CSRF" >/dev/null

API_CSRF=$(curl -s -b cookies.txt "$SUPERSET_URL/api/v1/security/csrf_token/" | jq -r '.result')
echo "API_CSRF: $API_CSRF"

# Get DB ID
DB_ID=$(curl -s -b cookies.txt "$SUPERSET_URL/api/v1/database/?q=(page_size:50)" | jq -r '.result[] | select(.database_name == "TimescaleDB") | .id')
echo "DB_ID: $DB_ID"

# Get existing dataset v_ops_overview_daily
EXISTING=$(curl -s -b cookies.txt "$SUPERSET_URL/api/v1/dataset/?q=(page_size:5000)" | jq -r --arg name "v_ops_overview_daily" --argjson db "$DB_ID" '
    [.result[] | select(.table_name == $name and .database.id == $db) | .id]
    | if length > 0 then min else empty end
')
echo "EXISTING DATASET ID: $EXISTING"

# Check columns
COLS=$(curl -s -b cookies.txt "$SUPERSET_URL/api/v1/dataset/$EXISTING" | jq '.result.columns')
echo "COLUMNS:"
echo "$COLS" | jq '.[].column_name'


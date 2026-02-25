#!/bin/bash
set -eo pipefail

SUPERSET_URL="http://localhost:8089"
ADMIN_USER="admin"
ADMIN_PASS="admin"

# Login
LOGIN_CSRF=$(curl -s -c cookies.txt "$SUPERSET_URL/login/" | grep -o 'csrf_token" type="hidden" value="[^"]*' | cut -d'"' -f5 || echo "")
curl -s -b cookies.txt -c cookies.txt -X POST "$SUPERSET_URL/login/" -d "username=$ADMIN_USER&password=$ADMIN_PASS&csrf_token=$LOGIN_CSRF" >/dev/null
API_CSRF=$(curl -s -b cookies.txt "$SUPERSET_URL/api/v1/security/csrf_token/" | jq -r '.result')

EXISTING=1

echo "1. Getting current columns..."
curl -s -b cookies.txt "$SUPERSET_URL/api/v1/dataset/$EXISTING" | jq '.result.columns | length'

echo "2. Temporarily clearing SQL..."
curl -s -o temp_put.json -w "%{http_code}" -b cookies.txt -H "X-CSRFToken: $API_CSRF" -H "Content-Type: application/json" -X PUT "$SUPERSET_URL/api/v1/dataset/$EXISTING" -d '{"sql": ""}'

echo "3. Refreshing dataset..."
curl -s -o temp_ref.json -w "%{http_code}" -b cookies.txt -H "X-CSRFToken: $API_CSRF" -H "Content-Type: application/json" -X PUT "$SUPERSET_URL/api/v1/dataset/$EXISTING/refresh" -d '{}'

echo "4. Getting intermediate columns..."
curl -s -b cookies.txt "$SUPERSET_URL/api/v1/dataset/$EXISTING" | jq '.result.columns | length'

echo "5. Restoring SQL..."
cat << 'EOSQL' > sql.txt
SELECT *
FROM public.v_ops_overview_daily
WHERE tenant_code = COALESCE(NULLIF('{{ tam_tenant_code() }}',''), tenant_code)
EOSQL
SQL_CONTENT=$(cat sql.txt)
PAYLOAD=$(jq -n --arg sql "$SQL_CONTENT" '{"sql": $sql}')
curl -s -o temp_put2.json -w "%{http_code}" -b cookies.txt -H "X-CSRFToken: $API_CSRF" -H "Content-Type: application/json" -X PUT "$SUPERSET_URL/api/v1/dataset/$EXISTING" -d "$PAYLOAD"

echo "6. Getting final columns..."
curl -s -b cookies.txt "$SUPERSET_URL/api/v1/dataset/$EXISTING" | jq '.result.columns | length'

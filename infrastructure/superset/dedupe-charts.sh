#!/usr/bin/env bash
# Remove duplicate charts by slice_name, keep most recently modified
set -euo pipefail
SUPERSET_URL=${SUPERSET_URL:-"http://localhost:8089"}
ADMIN_USER=${ADMIN_USER:-admin}
ADMIN_PASS=${ADMIN_PASS:-admin}

req() { curl -s "$@"; }

TOKEN=$(req -X POST "$SUPERSET_URL/api/v1/security/login" -H "Content-Type: application/json" \
  -d "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASS\",\"provider\":\"db\"}" | jq -r '.access_token')
[ -z "$TOKEN" -o "$TOKEN" = "null" ] && { echo "❌ login failed"; exit 1; }
AUTH_HEADER="Authorization: Bearer $TOKEN"

# Fetch all charts (paged) and group by slice_name
charts=$(req -X GET "$SUPERSET_URL/api/v1/chart/?q=(page_size:10000)" -H "$AUTH_HEADER" | jq '.result')

# Find duplicate slice_names
dup_names=$(echo "$charts" | jq -r 'group_by(.slice_name) | map(select(length>1)) | .[] | .[0].slice_name')

for name in $dup_names; do
  ids=$(echo "$charts" | jq -r --arg n "$name" '.[] | select(.slice_name == $n) | .id')
  keep=$(printf '%s\n' $ids | sort -n | tail -1)
  for id in $ids; do
    if [ "$id" != "$keep" ]; then
      echo "✖ Deleting duplicate chart '$name' (id=$id), keeping id=$keep"
      req -X DELETE "$SUPERSET_URL/api/v1/chart/$id" -H "$AUTH_HEADER" >/dev/null 2>&1 || true
    fi
  done
done

echo "✅ Deduplication complete"

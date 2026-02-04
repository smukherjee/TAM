#!/usr/bin/env sh
set -eu
SUPERSET_URL=${SUPERSET_URL:-"http://superset:8088"}
DB_HOST=${DB_HOST:-"timescaledb"}
DB_NAME=${DB_NAME:-"utam"}
DB_USER=${DB_USER:-"postgres"}
DB_PASS=${DB_PASS:-"password"}

# Install tools
apk add --no-cache bash curl jq postgresql-client >/dev/null

echo "⏳ Waiting for Superset at ${SUPERSET_URL}..."
for i in $(seq 1 120); do
  status=$(curl -s "${SUPERSET_URL}/health" | jq -r '.status' || echo "")
  if [ "$status" = "healthy" ]; then echo "✅ Superset healthy"; break; fi
  sleep 2
  if [ "$i" -eq 120 ]; then echo "❌ Superset not healthy"; exit 1; fi
done

# Run provisioning (uses API and jq)
export SUPERSET_URL
bash /scripts/create-all-reports.sh || echo "Provision script finished (or already provisioned)."

# Seed demo data and refresh MVs
export PGPASSWORD="$DB_PASS"
psql -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 -f /seeds/11-demo-seed.sql || true
psql -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" -c "REFRESH MATERIALIZED VIEW asset_activity_heatmap;" || true
psql -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" -c "REFRESH MATERIALIZED VIEW violation_heatmap;" || true

echo "🎯 Superset bootstrap sidecar complete"

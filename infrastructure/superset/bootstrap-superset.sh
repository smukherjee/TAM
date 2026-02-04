#!/usr/bin/env bash
# Bootstrap Superset: wait for service, provision datasets/charts/dashboards, seed demo data
set -euo pipefail

SUPERSET_URL=${SUPERSET_URL:-"http://localhost:8089"}
DB_HOST=${DB_HOST:-"timescaledb"}
DB_NAME=${DB_NAME:-"utam"}
DB_USER=${DB_USER:-"postgres"}
DB_PASS=${DB_PASS:-"password"}

echo "⏳ Waiting for Superset API at $SUPERSET_URL..."
for i in {1..60}; do
  if curl -s "${SUPERSET_URL}/health" | jq -r '.status' 2>/dev/null | grep -qi "healthy"; then
    echo "✅ Superset is healthy"; break
  fi
  sleep 2
  if [ "$i" -eq 60 ]; then echo "❌ Superset not healthy after 120s"; exit 1; fi
done

# Provision charts/dashboards
bash "$(dirname "$0")/create-all-reports.sh"

# Optional: seed demo data and refresh materialized views
if docker compose ps timescaledb >/dev/null 2>&1; then
  echo "🌱 Seeding demo data into TimescaleDB..."
  docker compose exec -T timescaledb psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 < "$(dirname "$0")/../db/init/11-demo-seed.sql" || true
  echo "🔄 Refreshing materialized views (if present)..."
  docker compose exec -T timescaledb psql -U "$DB_USER" -d "$DB_NAME" -c "REFRESH MATERIALIZED VIEW IF EXISTS asset_activity_heatmap;" || true
  docker compose exec -T timescaledb psql -U "$DB_USER" -d "$DB_NAME" -c "REFRESH MATERIALIZED VIEW IF EXISTS violation_heatmap;" || true
fi

echo "🎯 Superset bootstrap complete. Visit: $SUPERSET_URL"

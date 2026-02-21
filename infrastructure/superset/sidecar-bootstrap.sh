#!/usr/bin/env sh
set -eu
SUPERSET_URL=${SUPERSET_URL:-"http://superset:8088"}
DB_HOST=${DB_HOST:-"timescaledb"}
DB_PORT=${DB_PORT:-"5432"}
DB_NAME=${DB_NAME:-"utam"}
DB_USER=${DB_USER:-"postgres"}
DB_PASS=${DB_PASS:-"password"}
FORCE_BOOTSTRAP=${FORCE_BOOTSTRAP:-"false"}

# Install tools
apk add --no-cache bash curl jq postgresql-client >/dev/null

echo "⏳ Waiting for Superset at ${SUPERSET_URL}..."
for i in $(seq 1 120); do
  if curl -s "${SUPERSET_URL}/health" | grep -qi "OK"; then
    echo "✅ Superset healthy"; break
  fi
  sleep 2
  if [ "$i" -eq 120 ]; then echo "❌ Superset not healthy"; exit 1; fi
done

# Run provisioning (uses API and jq)
export SUPERSET_URL
export DB_HOST DB_PORT DB_NAME DB_USER DB_PASS
SKIP_IF_PROVISIONED=true bash /scripts/create-all-reports.sh

echo "🔒 Configuring Public Role permissions for embedded dashboards via SQL..."
export PGPASSWORD="$DB_PASS"

ensure_bootstrap_state_table() {
  psql -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 << 'EOF'
CREATE TABLE IF NOT EXISTS tam_bootstrap_state (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
EOF
}

get_bootstrap_state() {
  key="$1"
  safe_key=$(printf "%s" "$key" | sed "s/'/''/g")
  psql -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" -At \
    -v ON_ERROR_STOP=1 \
    -c "SELECT value FROM tam_bootstrap_state WHERE key = '$safe_key' LIMIT 1;" 2>/dev/null || true
}

set_bootstrap_state() {
  key="$1"
  value="$2"
  safe_key=$(printf "%s" "$key" | sed "s/'/''/g")
  safe_value=$(printf "%s" "$value" | sed "s/'/''/g")
  psql -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 \
    -c "INSERT INTO tam_bootstrap_state (key, value, updated_at) VALUES ('$safe_key', '$safe_value', NOW()) ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value, updated_at = EXCLUDED.updated_at;" >/dev/null
}

should_run_step() {
  key="$1"
  if [ "$FORCE_BOOTSTRAP" = "true" ]; then
    return 0
  fi
  state="$(get_bootstrap_state "$key")"
  [ "$state" != "done" ]
}

ensure_bootstrap_state_table

# We must ensure the Public role has all_datasource_access and all_database_access to render charts without login
psql -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 << 'EOF'
DO $$ 
DECLARE 
    public_role_id INTEGER;
    ds_perm_id INTEGER;
    db_perm_id INTEGER;
BEGIN
    -- Only run this if the ab_role table exists (Superset is initialized)
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'ab_role') THEN
        
        -- Get or create Public role
        SELECT id INTO public_role_id FROM ab_role WHERE name = 'Public';
        IF public_role_id IS NULL THEN
            INSERT INTO ab_role (name) VALUES ('Public') RETURNING id INTO public_role_id;
        END IF;

        -- Find all_datasource_access permission_view ID
        SELECT pv.id INTO ds_perm_id 
        FROM ab_permission_view pv
        JOIN ab_permission p ON p.id = pv.permission_id
        JOIN ab_view_menu v ON v.id = pv.view_menu_id
        WHERE p.name = 'all_datasource_access' AND v.name = 'all_datasource_access';

        -- Find all_database_access permission_view ID
        SELECT pv.id INTO db_perm_id 
        FROM ab_permission_view pv
        JOIN ab_permission p ON p.id = pv.permission_id
        JOIN ab_view_menu v ON v.id = pv.view_menu_id
        WHERE p.name = 'all_database_access' AND v.name = 'all_database_access';

        -- Grant datasource access to Public
        IF ds_perm_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM ab_permission_view_role WHERE role_id = public_role_id AND permission_view_id = ds_perm_id) THEN
            INSERT INTO ab_permission_view_role (id, role_id, permission_view_id) VALUES (COALESCE((SELECT MAX(id) FROM ab_permission_view_role), 0) + 1, public_role_id, ds_perm_id);
        END IF;

        -- Grant database access to Public
        IF db_perm_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM ab_permission_view_role WHERE role_id = public_role_id AND permission_view_id = db_perm_id) THEN
            INSERT INTO ab_permission_view_role (id, role_id, permission_view_id) VALUES (COALESCE((SELECT MAX(id) FROM ab_permission_view_role), 0) + 2, public_role_id, db_perm_id);
        END IF;

    END IF;
END $$;
EOF
echo "✅ Public Role permissions configured"

# Seed demo data and refresh MVs only once per DB lifecycle.
if should_run_step "superset_demo_seed_v1"; then
  echo "🌱 Seeding demo data and refreshing materialized views..."
  psql -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 -f /seeds/11-demo-seed.sql || true
  psql -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" -c "REFRESH MATERIALIZED VIEW asset_activity_heatmap;" 2>/dev/null || true
  psql -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" -c "REFRESH MATERIALIZED VIEW violation_heatmap;" 2>/dev/null || true
  set_bootstrap_state "superset_demo_seed_v1" "done"
  echo "✅ Demo seed completed"
else
  echo "ℹ️ Demo seed already completed; skipping."
fi

# Generate backend data once unless explicitly forced.
if should_run_step "backend_generators_seed_v1"; then
  echo "⏳ Waiting for Backend API at http://backend:8080..."
  for i in $(seq 1 90); do
    if curl -s "http://backend:8080/api/actuator/health" | grep -qi "UP"; then
      echo "✅ Backend API healthy"
      break
    fi
    sleep 2
    if [ "$i" -eq 90 ]; then
      echo "⚠️ Backend not healthy after 180s, skipping data generators"
    fi
  done

  echo "🚀 Starting Data Generators..."

  # Sync vehicles to asset entries (required for maps to show asset movements)
  curl -s -X POST "http://backend:8080/api/admin/generators/sync-vehicle-assets/all" > /dev/null || true
  echo "✅ Vehicle-Asset sync triggered"

  curl -s -X POST "http://backend:8080/api/admin/generators/batch/all?batchSize=100" > /dev/null || true
  echo "✅ Batch generation (100 base records) triggered"

  # Generate 7 days of historical data for each tenant
  curl -s -X POST "http://backend:8080/api/admin/generators/historical/VIDP?days=7&samplesPerDay=24" > /dev/null || true
  curl -s -X POST "http://backend:8080/api/admin/generators/historical/LIRN?days=7&samplesPerDay=24" > /dev/null || true
  curl -s -X POST "http://backend:8080/api/admin/generators/historical/YBBN?days=7&samplesPerDay=24" > /dev/null || true
  echo "✅ Historical data (7 days x 3 tenants) triggered"

  curl -s -X POST "http://backend:8080/api/admin/generators/continuous/start" > /dev/null || true
  echo "✅ Continuous simulation started"

  # Give the backend a few seconds to commit metrics before refreshing grid views
  sleep 5
  curl -s -X POST "http://backend:8080/api/admin/generators/refresh-views" > /dev/null || true
  echo "✅ Materialized views (Heatmaps) refreshed via API"

  set_bootstrap_state "backend_generators_seed_v1" "done"
else
  echo "ℹ️ Backend data generators already triggered; skipping."
fi

echo "🎯 Superset bootstrap sidecar complete"

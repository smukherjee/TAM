#!/bin/bash
# Create Superset datasets, charts and dashboards for TAM reports
# Date: 2026-02-04
set -eo pipefail

SUPERSET_URL=${SUPERSET_URL:-"http://localhost:8089"}
ADMIN_USER=${ADMIN_USER:-admin}
ADMIN_PASS=${ADMIN_PASS:-admin}
SKIP_IF_PROVISIONED=${SKIP_IF_PROVISIONED:-true}
DB_HOST=${DB_HOST:-"127.0.0.1"}
DB_PORT=${DB_PORT:-"5433"}
DB_NAME=${DB_NAME:-"utam"}
DB_USER=${DB_USER:-"postgres"}
DB_PASS=${DB_PASS:-"password"}

echo "📊 Provisioning Superset reports at $SUPERSET_URL"

req() { curl -s -b /tmp/cookies.txt -H "X-CSRFToken: $API_CSRF" "$@"; }

refresh_dataset_metadata() {
  local DATASET_ID=$1
  [ -z "$DATASET_ID" ] || [ "$DATASET_ID" = "null" ] && return 1

  # For virtual datasets (with SQL), the /refresh endpoint sometimes fails silently in Superset 3.x
  # if the underlying views are just created. A more reliable way is to PUT the dataset again.
  local RESP HTTP COLS
  HTTP=$(curl -s -o /tmp/superset_dataset_refresh_resp.json -w "%{http_code}" \
    -b /tmp/cookies.txt -H "X-CSRFToken: $API_CSRF" -H "Content-Type: application/json" \
    -X PUT "$SUPERSET_URL/api/v1/dataset/$DATASET_ID/refresh" -d '{}')

  if [ "${HTTP:-500}" -ge 300 ]; then
    echo "⚠️ Failed to refresh dataset metadata for id=$DATASET_ID (HTTP $HTTP)" >&2
  fi
  
  # Check if columns are still empty, wait and retry once
  COLS=$(curl -s -b /tmp/cookies.txt "$SUPERSET_URL/api/v1/dataset/$DATASET_ID" | jq '.result.columns | length')
  if [ "${COLS:-0}" -eq 0 ]; then
    sleep 2
    curl -s -o /dev/null -b /tmp/cookies.txt -H "X-CSRFToken: $API_CSRF" -H "Content-Type: application/json" -X PUT "$SUPERSET_URL/api/v1/dataset/$DATASET_ID/refresh" -d '{}'
  fi
}

exec_metadata_sql() {
  local sql="$1"
  if command -v psql >/dev/null 2>&1; then
    PGPASSWORD="$DB_PASS" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 -c "$sql" >/dev/null
    return 0
  fi
  if command -v docker-compose >/dev/null 2>&1; then
    docker-compose -f docker-compose.dev.yml exec -T timescaledb \
      psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 -c "$sql" >/dev/null
    return 0
  fi
  if command -v docker >/dev/null 2>&1; then
    docker exec -i tam-timescaledb-1 psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 -c "$sql" >/dev/null
    return 0
  fi
  echo "❌ Neither psql nor docker-based fallback is available for dashboard chart linking." >&2
  return 1
}

dedupe_charts() {
  # Keep a single canonical chart per (name, datasource), removing older accidental duplicates.
  exec_metadata_sql "
WITH ranked AS (
  SELECT id, ROW_NUMBER() OVER (PARTITION BY slice_name, datasource_id ORDER BY id) AS rn
  FROM slices
)
DELETE FROM slices
WHERE id IN (SELECT id FROM ranked WHERE rn > 1);"
}

# 1) Login via Session Auth (Fixes AnonymousUserMixin bug)
LOGIN_CSRF=$(curl -s -c /tmp/cookies.txt "$SUPERSET_URL/login/" | grep -o 'csrf_token" type="hidden" value="[^"]*' | cut -d'"' -f5 || echo "")
curl -s -b /tmp/cookies.txt -c /tmp/cookies.txt -X POST "$SUPERSET_URL/login/" \
  -d "username=$ADMIN_USER&password=$ADMIN_PASS&csrf_token=$LOGIN_CSRF" >/dev/null

API_CSRF=$(curl -s -b /tmp/cookies.txt "$SUPERSET_URL/api/v1/security/csrf_token/" | jq -r '.result' || echo "")
if [ -z "$API_CSRF" ] || [ "$API_CSRF" == "null" ]; then
  echo "❌ Superset login failed (no API CSRF token retrieved)"
  exit 1
fi

# 2) Ensure TimescaleDB connection exists
DB_ID=$(req -X GET "$SUPERSET_URL/api/v1/database/?page_size=5000" | jq -r '.result[] | select(.database_name == "TimescaleDB") | .id' | head -n 1)

if [ -z "$DB_ID" ] || [ "$DB_ID" == "null" ]; then
  echo "ℹ️ Creating TimescaleDB database connection"
  PAYLOAD=$(jq -n \
    '{database_name: "TimescaleDB", sqlalchemy_uri: "postgresql+psycopg2://postgres:password@timescaledb:5432/utam", expose_in_sqllab: true, allow_run_async: true}')
  RESP=$(req -X POST "$SUPERSET_URL/api/v1/database/" -H "Content-Type: application/json" -d "$PAYLOAD")
  DB_ID=$(echo "$RESP" | jq -r '.id')
fi

if [ -z "$DB_ID" ] || [ "$DB_ID" == "null" ]; then
  echo "❌ Could not find/create TimescaleDB datasource in Superset metadata DB"
  exit 1
fi
echo "   ✅ Database ID: $DB_ID"

dedupe_charts

list_datasets() {
  req -X GET "$SUPERSET_URL/api/v1/dataset/?q=(page_size:5000)"
}

list_charts() {
  req -X GET "$SUPERSET_URL/api/v1/chart/?q=(page_size:5000)"
}

list_dashboards() {
  req -X GET "$SUPERSET_URL/api/v1/dashboard/?q=(page_size:5000)"
}

create_dataset() {
  local NAME=$1
  local SCHEMA=${2:-public}
  local TABLE=${3:-$1}
  local SQL=${4:-}
  local EXISTING
  EXISTING=$(list_datasets | jq -r --arg name "$NAME" --argjson db "$DB_ID" '
    [.result[] | select(.table_name == $name and .database.id == $db) | .id]
    | if length > 0 then min else empty end
  ')

  if [ -n "$EXISTING" ]; then
    # Clear SQL to force physical column extraction
    local TEMP_PAYLOAD
    TEMP_PAYLOAD=$(jq -n --arg name "$NAME" --arg schema "$SCHEMA" '{table_name: $name, schema: $schema, sql: ""}')
    req -X PUT "$SUPERSET_URL/api/v1/dataset/$EXISTING" -H "Content-Type: application/json" -d "$TEMP_PAYLOAD" >/dev/null

    refresh_dataset_metadata "$EXISTING"

    # Restore SQL
    if [ -n "$SQL" ]; then
      local UPDATE_PAYLOAD
      UPDATE_PAYLOAD=$(jq -n --arg name "$NAME" --arg sql "$SQL" --arg schema "$SCHEMA" '{table_name: $name, schema: $schema, sql: $sql}')
      req -X PUT "$SUPERSET_URL/api/v1/dataset/$EXISTING" -H "Content-Type: application/json" -d "$UPDATE_PAYLOAD" >/dev/null
    fi

    echo "$EXISTING"
    return
  fi

  # For new datasets, create without SQL first to fetch columns
  local PAYLOAD
  PAYLOAD=$(jq -n --argjson db "$DB_ID" --arg schema "$SCHEMA" --arg table "$TABLE" \
    '{database: $db, schema: $schema, table_name: $table}')

  RESP=$(req -X POST "$SUPERSET_URL/api/v1/dataset/" -H "Content-Type: application/json" -d "$PAYLOAD")
  local CREATED_ID
  CREATED_ID=$(echo "$RESP" | jq -r '.id // empty')
  if [ -z "$CREATED_ID" ]; then
    echo "❌ Failed to create dataset $NAME: $RESP" >&2
    exit 1
  fi
  
  refresh_dataset_metadata "$CREATED_ID"
  
  # Then update with SQL if provided
  if [ -n "$SQL" ]; then
    local UPDATE_PAYLOAD
    UPDATE_PAYLOAD=$(jq -n --arg name "$NAME" --arg sql "$SQL" --arg schema "$SCHEMA" '{table_name: $name, schema: $schema, sql: $sql}')
    req -X PUT "$SUPERSET_URL/api/v1/dataset/$CREATED_ID" -H "Content-Type: application/json" -d "$UPDATE_PAYLOAD" >/dev/null
  fi

  echo "$CREATED_ID"
}

tenant_scoped_sql() {
  local SCHEMA=${1:-public}
  local TABLE=$2
  cat <<EOF
SELECT *
FROM ${SCHEMA}.${TABLE}
WHERE tenant_code = COALESCE(NULLIF('{{ tam_tenant_code() }}',''), tenant_code)
EOF
}

create_tenant_dataset() {
  local NAME=$1
  local SCHEMA=${2:-public}
  local TABLE=${3:-$1}
  local SQL
  SQL=$(tenant_scoped_sql "$SCHEMA" "$TABLE")
  create_dataset "$NAME" "$SCHEMA" "$TABLE" "$SQL"
}

create_chart() {
  local NAME=$1; local DS=$2; local VIZ=$3; local PARAMS_JSON=$4
  if [ -z "$DS" ] || [ "$DS" == "null" ]; then echo ""; return; fi
  if ! echo "$PARAMS_JSON" | jq -c . >/dev/null 2>&1; then
    echo "❌ Invalid params for chart $NAME: $PARAMS_JSON" >&2
    exit 1
  fi
  local PARAMS_CLEAN
  PARAMS_CLEAN=$(echo "$PARAMS_JSON" | jq -c .)
  local EXIST_ID
  # Always reuse the oldest matching chart id to keep slice ids stable across re-runs.
  EXIST_ID=$(list_charts | jq -r --arg name "$NAME" --argjson ds "$DS" '
    [.result[]
      | select(.slice_name == $name and ((.datasource_id // (.datasource.id // -1)) == $ds))
      | .id
    ] | if length > 0 then min else empty end
  ')
  if [ -z "$EXIST_ID" ]; then
    EXIST_ID=$(list_charts | jq -r --arg name "$NAME" '
      [.result[] | select(.slice_name == $name) | .id]
      | if length > 0 then min else empty end
    ')
  fi
  local PAYLOAD
  PAYLOAD=$(jq -n --arg name "$NAME" --argjson ds "$DS" --arg viz "$VIZ" --arg params "$PARAMS_CLEAN" \
    '{slice_name: $name, datasource_id: $ds, datasource_type: "table", viz_type: $viz, params: $params}')
  if [ -n "$EXIST_ID" ] && [ "$EXIST_ID" != "null" ]; then
    req -X PUT "$SUPERSET_URL/api/v1/chart/$EXIST_ID" -H "Content-Type: application/json" -d "$PAYLOAD" >/dev/null
    echo "$EXIST_ID"
  else
    RESP=$(req -X POST "$SUPERSET_URL/api/v1/chart/" -H "Content-Type: application/json" -d "$PAYLOAD")
    echo "$RESP" | jq -r '.id'
  fi
}

create_table_raw_chart() {
  local NAME=$1; local DS=$2; shift 2
  local COLS=("$@")
  local cols_json
  cols_json=$(printf '%s\n' "${COLS[@]}" | jq -R . | jq -s .)
  local params
  params=$(jq -n --argjson cols "$cols_json" '{all_columns:$cols, query_mode:"raw", row_limit:500, time_range:"No filter"}')
  create_chart "$NAME" "$DS" "table" "$params"
}

sync_raw_table_query_context() {
  local CHART_ID=$1; local DS=$2; shift 2
  local COLS=("$@")
  [ -z "$CHART_ID" ] || [ -z "$DS" ] && return

  local cols_json chart_meta slice_name viz_type params query_context payload
  cols_json=$(printf '%s\n' "${COLS[@]}" | jq -R . | jq -s .)
  chart_meta=$(req -X GET "$SUPERSET_URL/api/v1/chart/$CHART_ID")
  slice_name=$(echo "$chart_meta" | jq -r '.result.slice_name')
  viz_type=$(echo "$chart_meta" | jq -r '.result.viz_type // "table"')

  params=$(jq -n --argjson cols "$cols_json" '{all_columns:$cols, query_mode:"raw", row_limit:500, time_range:"No filter"}')
  query_context=$(jq -n \
    --argjson ds "$DS" \
    --argjson cid "$CHART_ID" \
    --argjson cols "$cols_json" \
    '{
      datasource:{id:$ds,type:"table"},
      force:false,
      queries:[{
        time_range:"No filter",
        filters:[],
        extras:{having:"",where:""},
        applied_time_extras:{},
        columns:$cols,
        orderby:[],
        annotation_layers:[],
        row_limit:500,
        series_limit:0,
        order_desc:true,
        url_params:{},
        custom_params:{},
        custom_form_data:{},
        post_processing:[]
      }],
      form_data:{
        all_columns:$cols,
        datasource:($ds|tostring + "__table"),
        query_mode:"raw",
        row_limit:500,
        slice_id:$cid,
        time_range:"No filter",
        viz_type:"table",
        force:false,
        result_format:"json",
        result_type:"full",
        include_time:false
      },
      result_format:"json",
      result_type:"full"
    }')

  payload=$(jq -n \
    --arg name "$slice_name" \
    --arg viz "$viz_type" \
    --argjson ds "$DS" \
    --arg params "$(echo "$params" | jq -c .)" \
    --arg qc "$(echo "$query_context" | jq -c .)" \
    '{slice_name:$name,datasource_id:$ds,datasource_type:"table",viz_type:$viz,params:$params,query_context:$qc}')

  req -X PUT "$SUPERSET_URL/api/v1/chart/$CHART_ID" -H "Content-Type: application/json" -d "$payload" >/dev/null
}

create_dashboard() {
  local TITLE=$1; local SLUG=$2
  local EXIST_ID
  EXIST_ID=$(list_dashboards | jq -r --arg slug "$SLUG" '
    [.result[] | select(.slug == $slug) | .id]
    | if length > 0 then min else empty end
  ')
  if [ -z "$EXIST_ID" ]; then
    EXIST_ID=$(list_dashboards | jq -r --arg title "$TITLE" '
      [.result[] | select(.dashboard_title == $title) | .id]
      | if length > 0 then min else empty end
    ')
  fi
  if [ -n "$EXIST_ID" ] && [ "$EXIST_ID" != "null" ]; then echo "$EXIST_ID"; return; fi
  local payload
  payload=$(jq -n --arg title "$TITLE" --arg slug "$SLUG" '{dashboard_title:$title, slug:$slug}')
  RESP=$(req -X POST "$SUPERSET_URL/api/v1/dashboard/" -H "Content-Type: application/json" -d "$payload")
  echo "$RESP" | jq -r '.id'
}

add_chart_to_dashboard() {
  local DASH_ID=$1
  shift
  [ -z "$DASH_ID" ] && return

  # Superset 3 dashboard chart association is stored in dashboard_slices.
  # The old /dashboard/<id>/charts POST path is not writable in this version.
  exec_metadata_sql "DELETE FROM dashboard_slices WHERE dashboard_id = ${DASH_ID};"

  for CHART_ID in "$@"; do
    [ -z "$CHART_ID" ] && continue
    exec_metadata_sql "INSERT INTO dashboard_slices (dashboard_id, slice_id) VALUES (${DASH_ID}, ${CHART_ID}) ON CONFLICT (dashboard_id, slice_id) DO NOTHING;"
  done
}

is_already_provisioned() {
  local missing_dash missing_ds missing_charts missing_links missing_layout invalid_layout

  missing_dash=$(list_dashboards | jq -r '
    [.result[]?.slug] as $have |
    ["tam_ops_full","tam_safety","tam_turnaround","tam_assets","tam_pipeline","tam_predictive"]
    | map(select(. as $slug | ($have | index($slug)) | not))
    | join(",")
  ')

  missing_ds=$(list_datasets | jq -r --argjson db "$DB_ID" '
    [.result[] | select(.database.id == $db) | .table_name] as $have |
    [
      "v_ops_overview_daily","v_flight_movements_hourly","v_vehicle_activity_summary_daily",
      "v_stand_gate_occupancy","v_turnaround_sla_compliance","v_delay_root_causes",
      "v_speed_violations_by_zone","v_restricted_zone_breach_dwell","v_discrepancy_trends_daily",
      "v_asset_utilization_status_counts","v_maintenance_downtime_by_type","v_dwell_proxy_by_zone_hourly",
      "v_alerts_summary_type_hour","v_repeat_offenders_assets","v_throughput_ops_volume_today",
      "v_pipeline_health_events_per_minute","v_activity_heatmap_latest","v_violation_heatmap_latest",
      "v_stand_conflicts","pred_turnaround_risk","pred_congestion","pred_zone_breach",
      "pred_asset_violation_risk","forecast_violations_hourly"
    ]
    | map(select(. as $name | ($have | index($name)) | not))
    | join(",")
  ')

  missing_charts=$(list_charts | jq -r '
    [.result[]?.slice_name] as $have |
    [
      "Ops Overview Daily","Flight Movements Hourly","Vehicle Activity Daily",
      "Stand Occupancy","SLA Compliance by Task","Delay Root Causes","Violations by Zone",
      "Breach Dwell Stats","Discrepancy Trends Daily","Asset Utilization Status",
      "Maintenance Downtime","Dwell Proxy by Zone Hourly","Alerts by Type/Hour",
      "Repeat Offenders","Throughput Today","Pipeline Events/min","Stand Conflicts"
    ]
    | map(select(. as $name | ($have | index($name)) | not))
    | join(",")
  ')

  missing_links=""
  missing_layout=""
  invalid_layout=""
  for slug in tam_ops_full tam_safety tam_turnaround tam_assets tam_pipeline tam_predictive; do
    chart_count=$(req -X GET "$SUPERSET_URL/api/v1/dashboard/$slug/charts" | jq -r '.result | length')
    if [ "${chart_count:-0}" -eq 0 ]; then
      if [ -z "$missing_links" ]; then
        missing_links="$slug"
      else
        missing_links="$missing_links,$slug"
      fi
    fi

    dash_payload=$(req -X GET "$SUPERSET_URL/api/v1/dashboard/$slug")
    grid_children=$(echo "$dash_payload" | jq -r '.result.position_json | (fromjson? // {}) | .GRID_ID.children | length')
    root_first_child=$(echo "$dash_payload" | jq -r '.result.position_json | (fromjson? // {}) | .ROOT_ID.children[0] // ""')
    root_first_type=$(echo "$dash_payload" | jq -r --arg cid "$root_first_child" '.result.position_json | (fromjson? // {}) as $p | ($p[$cid].type // "")')
    row_meta_ok=$(echo "$dash_payload" | jq -r '.result.position_json | (fromjson? // {}) as $p | (($p.GRID_ID.children // []) | map(($p[.].meta.background // "") != "") | all)')

    # Valid root child is either GRID_ID (no tabs) or a TABS component.
    if [ -z "$root_first_child" ] || { [ "$root_first_child" != "GRID_ID" ] && [ "$root_first_type" != "TABS" ]; }; then
      if [ -z "$invalid_layout" ]; then
        invalid_layout="$slug"
      else
        invalid_layout="$invalid_layout,$slug"
      fi
    fi
    if [ "${row_meta_ok:-false}" != "true" ]; then
      if [ -z "$invalid_layout" ]; then
        invalid_layout="$slug"
      else
        invalid_layout="$invalid_layout,$slug"
      fi
    fi

    if [ "${grid_children:-0}" -eq 0 ]; then
      if [ -z "$missing_layout" ]; then
        missing_layout="$slug"
      else
        missing_layout="$missing_layout,$slug"
      fi
    fi
  done

  if [ -z "$missing_dash" ] && [ -z "$missing_ds" ] && [ -z "$missing_charts" ] && [ -z "$missing_links" ] && [ -z "$missing_layout" ] && [ -z "$invalid_layout" ]; then
    return 0
  fi

  [ -n "$missing_dash" ] && echo "ℹ️ Missing dashboards: $missing_dash"
  [ -n "$missing_ds" ] && echo "ℹ️ Missing datasets: $missing_ds"
  [ -n "$missing_charts" ] && echo "ℹ️ Missing charts: $missing_charts"
  [ -n "$missing_links" ] && echo "ℹ️ Missing dashboard chart links: $missing_links"
  [ -n "$missing_layout" ] && echo "ℹ️ Missing dashboard layout rows: $missing_layout"
  [ -n "$invalid_layout" ] && echo "ℹ️ Invalid dashboard root layout: $invalid_layout"
  return 1
}

if [ "$SKIP_IF_PROVISIONED" = "true" ] && is_already_provisioned; then
  echo "✅ Superset reports already provisioned. Skipping create/update."
  exit 0
fi

METRIC_COUNT=$(jq -n '{expressionType:"SIMPLE",aggregate:"COUNT",column:null,label:"Count"}')

DS_OPS_OVR=$(create_tenant_dataset "v_ops_overview_daily")
DS_FLT_HR=$(create_tenant_dataset "v_flight_movements_hourly")
DS_VEH_SUM=$(create_tenant_dataset "v_vehicle_activity_summary_daily")
DS_STAND_OCC=$(create_tenant_dataset "v_stand_gate_occupancy")
DS_SLA=$(create_tenant_dataset "v_turnaround_sla_compliance")
DS_DELAY=$(create_tenant_dataset "v_delay_root_causes")
DS_VIOL_ZONE=$(create_tenant_dataset "v_speed_violations_by_zone")
DS_BREACH_DWELL=$(create_tenant_dataset "v_restricted_zone_breach_dwell")
DS_DISCREP=$(create_tenant_dataset "v_discrepancy_trends_daily")
DS_UTIL=$(create_tenant_dataset "v_asset_utilization_status_counts")
DS_MAINT=$(create_tenant_dataset "v_maintenance_downtime_by_type")
DS_DWELL_PROXY=$(create_tenant_dataset "v_dwell_proxy_by_zone_hourly")
DS_ALERTS=$(create_tenant_dataset "v_alerts_summary_type_hour")
DS_OFFEND=$(create_tenant_dataset "v_repeat_offenders_assets")
DS_THRPT=$(create_tenant_dataset "v_throughput_ops_volume_today")
DS_PIPE=$(create_tenant_dataset "v_pipeline_health_events_per_minute")
DS_HM_ACT=$(create_tenant_dataset "v_activity_heatmap_latest")
DS_HM_VIOL=$(create_tenant_dataset "v_violation_heatmap_latest")
DS_STAND_CONFLICT=$(create_tenant_dataset "v_stand_conflicts")
DS_PRED_TA=$(create_tenant_dataset "pred_turnaround_risk")
DS_PRED_CONG=$(create_tenant_dataset "pred_congestion")
DS_PRED_ZONE=$(create_tenant_dataset "pred_zone_breach")
DS_PRED_ASSET=$(create_tenant_dataset "pred_asset_violation_risk")
DS_FORE_VIOL=$(create_tenant_dataset "forecast_violations_hourly")

echo "✅ Datasets created. Building charts..."

CH_OPS_F=$(create_table_raw_chart "Ops Overview Daily" "$DS_OPS_OVR" tenant_code day flights vehicles alerts violations)
CH_FLT_HR=$(create_table_raw_chart "Flight Movements Hourly" "$DS_FLT_HR" tenant_code hour positions)
CH_VEH_SUM=$(create_table_raw_chart "Vehicle Activity Daily" "$DS_VEH_SUM" tenant_code day vehicle_type telemetry_points avg_speed)
CH_STAND_OCC=$(create_table_raw_chart "Stand Occupancy" "$DS_STAND_OCC" tenant_code stand_id sessions avg_turnaround_min)
CH_SLA=$(create_table_raw_chart "SLA Compliance by Task" "$DS_SLA" tenant_code task_type avg_delay_min on_time_ratio tasks)
CH_DELAY=$(create_table_raw_chart "Delay Root Causes" "$DS_DELAY" tenant_code cause total_delay_min affected_tasks)
CH_VIOL_ZONE=$(create_table_raw_chart "Violations by Zone" "$DS_VIOL_ZONE" tenant_code zone_name severity violations)
CH_BREACH=$(create_table_raw_chart "Breach Dwell Stats" "$DS_BREACH_DWELL" tenant_code zone_name zone_type avg_dwell_sec max_dwell_sec breaches)
CH_DISCREP=$(create_table_raw_chart "Discrepancy Trends Daily" "$DS_DISCREP" tenant_code discrepancy_type day discrepancies)
CH_UTIL=$(create_table_raw_chart "Asset Utilization Status" "$DS_UTIL" tenant_code status assets)
CH_MAINT=$(create_table_raw_chart "Maintenance Downtime" "$DS_MAINT" tenant_code status count)
CH_DWELL=$(create_table_raw_chart "Dwell Proxy by Zone Hourly" "$DS_DWELL_PROXY" tenant_code zone hour movement_points)
CH_ALERTS=$(create_table_raw_chart "Alerts by Type/Hour" "$DS_ALERTS" tenant_code type hour alerts)
CH_OFFEND=$(create_table_raw_chart "Repeat Offenders" "$DS_OFFEND" tenant_code asset_identifier violations)
CH_THRPT=$(create_table_raw_chart "Throughput Today" "$DS_THRPT" tenant_code flights_today tasks_done_today alerts_closed_today)
CH_PIPE=$(create_table_raw_chart "Pipeline Events/min" "$DS_PIPE" tenant_code minute events)
CH_HM_ACT=$(create_table_raw_chart "Activity Heatmap Table" "$DS_HM_ACT" tenant_code time_bucket grid_latitude grid_longitude activity_count unique_assets avg_speed max_speed median_speed first_activity last_activity)
CH_HM_VIOL=$(create_table_raw_chart "Violation Heatmap Table" "$DS_HM_VIOL" tenant_code time_bucket grid_latitude grid_longitude violation_count critical_count high_count medium_count low_count unique_violating_assets unique_zones_violated most_common_zone_type first_violation last_violation)
CH_STAND_CONFLICT=$(create_table_raw_chart "Stand Conflicts" "$DS_STAND_CONFLICT" tenant_code stand_id overlapping_pairs)

P_DECK_ACT=$(jq -n '{spatial:{type:"latlon",latCol:"grid_latitude",lonCol:"grid_longitude"}, mapbox_style:"mapbox://styles/mapbox/dark-v10", point_radius_fixed:20, row_limit:5000, time_grain_sqla:"PT1H", granularity_sqla:"time_bucket", weight:"activity_count", metric:{expressionType:"SQL",sqlExpression:"sum(activity_count)",label:"Activity"}}')
CH_DECK_ACT=$(create_chart "Activity Heatmap (deck.gl)" "$DS_HM_ACT" "deck_heatmap" "$P_DECK_ACT")
P_DECK_VIOL=$(jq -n '{spatial:{type:"latlon",latCol:"grid_latitude",lonCol:"grid_longitude"}, mapbox_style:"mapbox://styles/mapbox/dark-v10", point_radius_fixed:20, row_limit:5000, time_grain_sqla:"PT1H", granularity_sqla:"time_bucket", weight:"violation_count", metric:{expressionType:"SQL",sqlExpression:"sum(violation_count)",label:"Violations"}}')
CH_DECK_VIOL=$(create_chart "Violation Heatmap (deck.gl)" "$DS_HM_VIOL" "deck_heatmap" "$P_DECK_VIOL")

P_LINE_FLT=$(jq -n '{metrics:[{"expressionType":"SQL","sqlExpression":"sum(positions)","label":"Positions"}], granularity_sqla:"hour", time_grain_sqla:"PT1H", time_range:"last 24 hours"}')
CH_LINE_FLT=$(create_chart "Flight Movements (Line)" "$DS_FLT_HR" "line" "$P_LINE_FLT")

P_LINE_ALERTS=$(jq -n '{metrics:[{"expressionType":"SQL","sqlExpression":"sum(alerts)","label":"Alerts"}], granularity_sqla:"hour", time_grain_sqla:"PT1H", time_range:"last 24 hours", groupby:["type"]}')
CH_LINE_ALERTS=$(create_chart "Alerts by Type (Line)" "$DS_ALERTS" "line" "$P_LINE_ALERTS")

CH_PRED_TA=$(create_table_raw_chart "Turnaround Risk" "$DS_PRED_TA" tenant_code flight_id stand_id risk_score risk_band as_of created_at)
CH_PRED_CONG=$(create_table_raw_chart "Congestion Forecast" "$DS_PRED_CONG" tenant_code zone forecast_time expected_density created_at)
CH_PRED_ZONE=$(create_table_raw_chart "Zone Breach Probability" "$DS_PRED_ZONE" tenant_code zone_id horizon_minutes probability top_asset_categories as_of)
CH_PRED_ASSET=$(create_table_raw_chart "Asset Violation Risk" "$DS_PRED_ASSET" tenant_code asset_identifier probability expected_severity as_of created_at)
CH_FORE_VIOL=$(create_table_raw_chart "Violations Forecast (Hourly)" "$DS_FORE_VIOL" hour tenant_code expected_count lower upper created_at)

# Ensure chart query_context is synced to the tenant-scoped dataset.
sync_raw_table_query_context "$CH_OPS_F" "$DS_OPS_OVR" tenant_code day flights vehicles alerts violations

echo "✅ Charts created. Building dashboards..."

DASH_OPS=$(create_dashboard "TAM Ops Overview" "tam_ops_full")
DASH_SAFE=$(create_dashboard "TAM Safety & Security" "tam_safety")
DASH_TA=$(create_dashboard "TAM Turnaround" "tam_turnaround")
DASH_ASSET=$(create_dashboard "TAM Assets" "tam_assets")
DASH_PIPE=$(create_dashboard "TAM Pipeline" "tam_pipeline")
DASH_PRED=$(create_dashboard "TAM Predictive" "tam_predictive")

echo "🎉 Provisioned dashboards (IDs): $DASH_OPS, $DASH_SAFE, $DASH_TA, $DASH_ASSET, $DASH_PIPE, $DASH_PRED"

attach_to_dashboard() {
  local DASH_ID=$1; shift
  local TITLE=$1; shift
  local FILTER_DS_ID=$1; shift
  local CHART_IDS=($@)

  local pos
  pos=$(jq -n '{"DASHBOARD_VERSION":"v2","ROOT_ID":{"id":"ROOT_ID","type":"ROOT","children":["GRID_ID"]},"GRID_ID":{"id":"GRID_ID","type":"GRID","children":[]}}')

  local row_index=1
  local buffer=()
  for cid in "${CHART_IDS[@]}"; do
    buffer+=("$cid")
    if [ ${#buffer[@]} -eq 2 ]; then
      local row_key="ROW-${row_index}"
      local col1="COL-${row_index}-1"
      local col2="COL-${row_index}-2"
      local ch1="CHART-${buffer[0]}"
      local ch2="CHART-${buffer[1]}"
      pos=$(echo "$pos" | jq --arg rk "$row_key" --arg c1 "$col1" --arg c2 "$col2" '.[$rk]={id:$rk,type:"ROW",meta:{background:"BACKGROUND_TRANSPARENT"},children:[$c1,$c2]}')
      pos=$(echo "$pos" | jq --arg ck "$col1" --arg ch "$ch1" '.[$ck]={id:$ck,type:"COLUMN",children:[$ch]}')
      pos=$(echo "$pos" | jq --arg ck "$col2" --arg ch "$ch2" '.[$ck]={id:$ck,type:"COLUMN",children:[$ch]}')
      pos=$(echo "$pos" | jq --arg ch "$ch1" --argjson cid "${buffer[0]}" '.[$ch]={id:$ch,type:"CHART",meta:{chartId:$cid,slice_id:$cid,uuid:$ch}}')
      pos=$(echo "$pos" | jq --arg ch "$ch2" --argjson cid "${buffer[1]}" '.[$ch]={id:$ch,type:"CHART",meta:{chartId:$cid,slice_id:$cid,uuid:$ch}}')
      pos=$(echo "$pos" | jq --arg rk "$row_key" '.GRID_ID.children += [$rk]')
      buffer=()
      row_index=$((row_index+1))
    fi
  done

  if [ ${#buffer[@]} -eq 1 ]; then
    local row_key="ROW-${row_index}"
    local col1="COL-${row_index}-1"
    local ch1="CHART-${buffer[0]}"
    pos=$(echo "$pos" | jq --arg rk "$row_key" --arg c1 "$col1" '.[$rk]={id:$rk,type:"ROW",meta:{background:"BACKGROUND_TRANSPARENT"},children:[$c1]}')
    pos=$(echo "$pos" | jq --arg ck "$col1" --arg ch "$ch1" '.[$ck]={id:$ck,type:"COLUMN",children:[$ch]}')
    pos=$(echo "$pos" | jq --arg ch "$ch1" --argjson cid "${buffer[0]}" '.[$ch]={id:$ch,type:"CHART",meta:{chartId:$cid,slice_id:$cid,uuid:$ch}}')
    pos=$(echo "$pos" | jq --arg rk "$row_key" '.GRID_ID.children += [$rk]')
  fi

  local meta
  meta=$(jq -n '{native_filter_configuration:[]}')
  local pos_str meta_str payload
  pos_str=$(echo "$pos" | jq -c .)
  meta_str=$(echo "$meta" | jq -c .)
  payload=$(jq -n --arg title "$TITLE" --arg pos "$pos_str" --arg meta "$meta_str" '{dashboard_title:$title, position_json:$pos, json_metadata:$meta}')

  local http_code
  http_code=$(curl -s -o /tmp/superset_dash_update_resp.json -w "%{http_code}" \
    -b /tmp/cookies.txt -H "X-CSRFToken: $API_CSRF" -H "Content-Type: application/json" \
    -X PUT "$SUPERSET_URL/api/v1/dashboard/$DASH_ID" -d "$payload")
  if [ "${http_code:-500}" -ge 300 ]; then
    echo "❌ Failed to update dashboard layout for $TITLE (HTTP $http_code): $(cat /tmp/superset_dash_update_resp.json)"
    exit 1
  fi
}

attach_to_dashboard "$DASH_OPS" "TAM Ops Overview" "$DS_OPS_OVR" "$CH_OPS_F" "$CH_FLT_HR" "$CH_VEH_SUM" "$CH_THRPT" "$CH_LINE_FLT"
attach_to_dashboard "$DASH_SAFE" "TAM Safety & Security" "$DS_ALERTS" "$CH_VIOL_ZONE" "$CH_BREACH" "$CH_DISCREP" "$CH_OFFEND" "$CH_DECK_VIOL" "$CH_LINE_ALERTS"
attach_to_dashboard "$DASH_TA" "TAM Turnaround" "$DS_STAND_OCC" "$CH_STAND_OCC" "$CH_SLA" "$CH_DELAY" "$CH_STAND_CONFLICT"
attach_to_dashboard "$DASH_ASSET" "TAM Assets" "$DS_UTIL" "$CH_UTIL" "$CH_MAINT" "$CH_DWELL" "$CH_DECK_ACT"
attach_to_dashboard "$DASH_PIPE" "TAM Pipeline" "$DS_PIPE" "$CH_PIPE"
attach_to_dashboard "$DASH_PRED" "TAM Predictive" "$DS_FORE_VIOL" "$CH_PRED_TA" "$CH_PRED_CONG" "$CH_PRED_ZONE" "$CH_PRED_ASSET" "$CH_FORE_VIOL"

add_chart_to_dashboard "$DASH_OPS" "$CH_OPS_F" "$CH_FLT_HR" "$CH_VEH_SUM" "$CH_THRPT" "$CH_LINE_FLT"
add_chart_to_dashboard "$DASH_SAFE" "$CH_VIOL_ZONE" "$CH_BREACH" "$CH_DISCREP" "$CH_OFFEND" "$CH_DECK_VIOL" "$CH_LINE_ALERTS"
add_chart_to_dashboard "$DASH_TA" "$CH_STAND_OCC" "$CH_SLA" "$CH_DELAY" "$CH_STAND_CONFLICT"
add_chart_to_dashboard "$DASH_ASSET" "$CH_UTIL" "$CH_MAINT" "$CH_DWELL" "$CH_DECK_ACT"
add_chart_to_dashboard "$DASH_PIPE" "$CH_PIPE"
add_chart_to_dashboard "$DASH_PRED" "$CH_PRED_TA" "$CH_PRED_CONG" "$CH_PRED_ZONE" "$CH_PRED_ASSET" "$CH_FORE_VIOL"

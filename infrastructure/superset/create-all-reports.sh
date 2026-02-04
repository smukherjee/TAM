#!/bin/bash
# Create Superset datasets, charts and dashboards for TAM reports
# Date: 2026-02-04
set -e

SUPERSET_URL=${SUPERSET_URL:-"http://localhost:8089"}
ADMIN_USER=${ADMIN_USER:-admin}
ADMIN_PASS=${ADMIN_PASS:-admin}

echo "📊 Provisioning Superset reports at $SUPERSET_URL"

req() { curl -s "$@"; }

# 1) Login
TOKEN=$(req -X POST "$SUPERSET_URL/api/v1/security/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\": \"$ADMIN_USER\", \"password\": \"$ADMIN_PASS\", \"provider\": \"db\"}" | jq -r '.access_token')

if [ -z "$TOKEN" ] || [ "$TOKEN" == "null" ]; then
  echo "❌ Superset login failed"
  exit 1
fi
AUTH_HEADER="Authorization: Bearer $TOKEN"

# 2) Ensure TimescaleDB connection exists
DB_ID=$(req -X GET "$SUPERSET_URL/api/v1/database/" -H "$AUTH_HEADER" | jq -r '.result[] | select(.database_name == "TimescaleDB") | .id')

if [ -z "$DB_ID" ]; then
  echo "ℹ️ Creating TimescaleDB database connection"
  PAYLOAD=$(jq -n \
    '{database_name: "TimescaleDB", sqlalchemy_uri: "postgresql+psycopg2://postgres:password@timescaledb:5432/utam", expose_in_sqllab: true, allow_run_async: true}')
  RESP=$(req -X POST "$SUPERSET_URL/api/v1/database/" -H "$AUTH_HEADER" -H "Content-Type: application/json" -d "$PAYLOAD")
  DB_ID=$(echo "$RESP" | jq -r '.id')
fi
echo "   ✅ Database ID: $DB_ID"

create_dataset() {
  local NAME=$1
  local SCHEMA=${2:-public}
  local TABLE=${3:-$1}
  local SQL=${4:-}
  local EXISTING
  EXISTING=$(req -X GET "$SUPERSET_URL/api/v1/dataset/?q=(filters:!((col:table_name,opr:eq,value:$NAME)))" -H "$AUTH_HEADER" | jq -r \
    ".result[] | select(.table_name == \"$NAME\") | .id")
  if [ -n "$EXISTING" ]; then echo "$EXISTING"; return; fi

  local PAYLOAD
  if [ -n "$SQL" ]; then
    PAYLOAD=$(jq -n --arg name "$NAME" --arg sql "$SQL" --argjson db "$DB_ID" --arg schema "$SCHEMA" \
      '{database: $db, table_name: $name, sql: $sql, schema: $schema}')
  else
    PAYLOAD=$(jq -n --argjson db "$DB_ID" --arg schema "$SCHEMA" --arg table "$TABLE" \
      '{database: $db, schema: $schema, table_name: $table}')
  fi

  RESP=$(req -X POST "$SUPERSET_URL/api/v1/dataset/" -H "$AUTH_HEADER" -H "Content-Type: application/json" -d "$PAYLOAD")
  echo "$RESP" | jq -r '.id'
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
  EXIST_ID=$(req -X GET "$SUPERSET_URL/api/v1/chart/?q=(page_size:5000,filters:!((col:slice_name,opr:eq,value:$NAME)))" -H "$AUTH_HEADER" \
    | jq -r '.result[0].id')
  local PAYLOAD
  PAYLOAD=$(jq -n --arg name "$NAME" --argjson ds "$DS" --arg viz "$VIZ" --arg params "$PARAMS_CLEAN" \
    '{slice_name: $name, datasource_id: $ds, datasource_type: "table", viz_type: $viz, params: $params}')
  if [ -n "$EXIST_ID" ] && [ "$EXIST_ID" != "null" ]; then
    req -X PUT "$SUPERSET_URL/api/v1/chart/$EXIST_ID" -H "$AUTH_HEADER" -H "Content-Type: application/json" -d "$PAYLOAD" >/dev/null
    echo "$EXIST_ID"
  else
    RESP=$(req -X POST "$SUPERSET_URL/api/v1/chart/" -H "$AUTH_HEADER" -H "Content-Type: application/json" -d "$PAYLOAD")
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

create_dashboard() {
  local TITLE=$1; local SLUG=$2
  local EXIST_ID
  EXIST_ID=$(req -X GET "$SUPERSET_URL/api/v1/dashboard/?q=(filters:!((col:dashboard_title,opr:eq,value:$TITLE)))" -H "$AUTH_HEADER" | jq -r '.result[0].id')
  if [ -n "$EXIST_ID" ] && [ "$EXIST_ID" != "null" ]; then echo "$EXIST_ID"; return; fi
  local payload
  payload=$(jq -n --arg title "$TITLE" --arg slug "$SLUG" '{dashboard_title:$title, slug:$slug}')
  RESP=$(req -X POST "$SUPERSET_URL/api/v1/dashboard/" -H "$AUTH_HEADER" -H "Content-Type: application/json" -d "$payload")
  echo "$RESP" | jq -r '.id'
}

add_chart_to_dashboard() {
  local DASH_ID=$1; local CHART_ID=$2
  [ -z "$DASH_ID" ] || [ -z "$CHART_ID" ] && return
  req -X POST "$SUPERSET_URL/api/v1/dashboard/$DASH_ID/charts/" -H "$AUTH_HEADER" -H "Content-Type: application/json" \
    -d "{\"chartIds\":[${CHART_ID}],\"newSliceIds\":[${CHART_ID}]}" >/dev/null 2>&1 || true
}

METRIC_COUNT=$(jq -n '{expressionType:"SIMPLE",aggregate:"COUNT",column:null,label:"Count"}')

DS_OPS_OVR=$(create_dataset "v_ops_overview_daily")
DS_FLT_HR=$(create_dataset "v_flight_movements_hourly")
DS_VEH_SUM=$(create_dataset "v_vehicle_activity_summary_daily")
DS_STAND_OCC=$(create_dataset "v_stand_gate_occupancy")
DS_SLA=$(create_dataset "v_turnaround_sla_compliance")
DS_DELAY=$(create_dataset "v_delay_root_causes")
DS_VIOL_ZONE=$(create_dataset "v_speed_violations_by_zone")
DS_BREACH_DWELL=$(create_dataset "v_restricted_zone_breach_dwell")
DS_DISCREP=$(create_dataset "v_discrepancy_trends_daily")
DS_UTIL=$(create_dataset "v_asset_utilization_status_counts")
DS_MAINT=$(create_dataset "v_maintenance_downtime_by_type")
DS_DWELL_PROXY=$(create_dataset "v_dwell_proxy_by_zone_hourly")
DS_ALERTS=$(create_dataset "v_alerts_summary_type_hour")
DS_OFFEND=$(create_dataset "v_repeat_offenders_assets")
DS_THRPT=$(create_dataset "v_throughput_ops_volume_today")
DS_PIPE=$(create_dataset "v_pipeline_health_events_per_minute")
DS_HM_ACT=$(create_dataset "v_activity_heatmap_latest")
DS_HM_VIOL=$(create_dataset "v_violation_heatmap_latest")
DS_STAND_CONFLICT=$(create_dataset "v_stand_conflicts")
DS_PRED_TA=$(create_dataset "pred_turnaround_risk")
DS_PRED_CONG=$(create_dataset "pred_congestion")
DS_PRED_ZONE=$(create_dataset "pred_zone_breach")
DS_PRED_ASSET=$(create_dataset "pred_asset_violation_risk")
DS_FORE_VIOL=$(create_dataset "forecast_violations_hourly")

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
  local CSS_DARK='.dashboard{background:#0f172a;color:#e5e7eb} .chart-container{background:#0f172a} .ant-card{background:#0f172a;color:#e5e7eb}'
  local CHART_IDS=($@)

  local pos=$(jq -n '{"DASHBOARD_VERSION":"v2","ROOT_ID":{"id":"ROOT_ID","type":"ROOT","children":["HEADER_ID","GRID_ID"]},"HEADER_ID":{"id":"HEADER_ID","type":"HEADER","meta":{"text":""}},"GRID_ID":{"id":"GRID_ID","type":"GRID","children":[]}}')
  pos=$(echo "$pos" | jq --arg t "$TITLE" '.HEADER_ID.meta.text = $t')

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
      pos=$(echo "$pos" | jq --arg rk "$row_key" --arg c1 "$col1" --arg c2 "$col2" '.[$rk]={id:$rk,type:"ROW",children:[$c1,$c2]}')
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
    pos=$(echo "$pos" | jq --arg rk "$row_key" --arg c1 "$col1" '.[$rk]={id:$rk,type:"ROW",children:[$c1]}')
    pos=$(echo "$pos" | jq --arg ck "$col1" --arg ch "$ch1" '.[$ck]={id:$ck,type:"COLUMN",children:[$ch]}')
    pos=$(echo "$pos" | jq --arg ch "$ch1" --argjson cid "${buffer[0]}" '.[$ch]={id:$ch,type:"CHART",meta:{chartId:$cid,slice_id:$cid,uuid:$ch}}')
    pos=$(echo "$pos" | jq --arg rk "$row_key" '.GRID_ID.children += [$rk]')
  fi

  local meta
  meta=$(jq -n '{}')
  local pos_str meta_str payload
  pos_str=$(echo "$pos" | jq -c .)
  meta_str=$(echo "$meta" | jq -c .)
  payload=$(jq -n --arg title "$TITLE" --arg pos "$pos_str" --arg meta "$meta_str" '{dashboard_title:$title, position_json:$pos, json_metadata:$meta}')

  req -X PUT "$SUPERSET_URL/api/v1/dashboard/$DASH_ID" -H "$AUTH_HEADER" -H "Content-Type: application/json" -d "$payload" >/dev/null 2>&1 || true
}

attach_to_dashboard "$DASH_OPS" "TAM Ops Overview" "$DS_OPS_OVR" "$CH_OPS_F" "$CH_FLT_HR" "$CH_VEH_SUM" "$CH_THRPT" "$CH_LINE_FLT"
attach_to_dashboard "$DASH_SAFE" "TAM Safety & Security" "$DS_ALERTS" "$CH_VIOL_ZONE" "$CH_BREACH" "$CH_DISCREP" "$CH_OFFEND" "$CH_DECK_VIOL" "$CH_LINE_ALERTS"
attach_to_dashboard "$DASH_TA" "TAM Turnaround" "$DS_STAND_OCC" "$CH_STAND_OCC" "$CH_SLA" "$CH_DELAY" "$CH_STAND_CONFLICT"
attach_to_dashboard "$DASH_ASSET" "TAM Assets" "$DS_UTIL" "$CH_UTIL" "$CH_MAINT" "$CH_DWELL" "$CH_DECK_ACT"
attach_to_dashboard "$DASH_PIPE" "TAM Pipeline" "$DS_PIPE" "$CH_PIPE"
attach_to_dashboard "$DASH_PRED" "TAM Predictive" "$DS_FORE_VIOL" "$CH_PRED_TA" "$CH_PRED_CONG" "$CH_PRED_ZONE" "$CH_PRED_ASSET" "$CH_FORE_VIOL"

for cid in "$CH_OPS_F" "$CH_FLT_HR" "$CH_VEH_SUM" "$CH_THRPT"; do add_chart_to_dashboard "$DASH_OPS" "$cid"; done
add_chart_to_dashboard "$DASH_OPS" "$CH_LINE_FLT"
for cid in "$CH_VIOL_ZONE" "$CH_BREACH" "$CH_DISCREP" "$CH_OFFEND" "$CH_DECK_VIOL"; do add_chart_to_dashboard "$DASH_SAFE" "$cid"; done
add_chart_to_dashboard "$DASH_SAFE" "$CH_LINE_ALERTS"
for cid in "$CH_STAND_OCC" "$CH_SLA" "$CH_DELAY" "$CH_STAND_CONFLICT"; do add_chart_to_dashboard "$DASH_TA" "$cid"; done
for cid in "$CH_UTIL" "$CH_MAINT" "$CH_DWELL" "$CH_DECK_ACT"; do add_chart_to_dashboard "$DASH_ASSET" "$cid"; done
add_chart_to_dashboard "$DASH_PIPE" "$CH_PIPE"
for cid in "$CH_PRED_TA" "$CH_PRED_CONG" "$CH_PRED_ZONE" "$CH_PRED_ASSET" "$CH_FORE_VIOL"; do add_chart_to_dashboard "$DASH_PRED" "$cid"; done

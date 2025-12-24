#!/bin/bash
set -e
SUPERSET_URL="http://localhost:8089"
echo "📊 Creating Tenant Assets..."

TOKEN=$(curl -s -X POST "$SUPERSET_URL/api/v1/security/login" -H "Content-Type: application/json" -d '{"username": "admin", "password": "admin", "provider": "db"}' | jq -r '.access_token')
AUTH_HEADER="Authorization: Bearer $TOKEN"

DB_ID=$(curl -s -X GET "$SUPERSET_URL/api/v1/database/" -H "$AUTH_HEADER" | jq -r '.result[] | select(.database_name == "TimescaleDB") | .id')

create_dataset() {
    local NAME=$1
    local SQL=$2
    # Check if exists
    EXISTING=$(curl -s -X GET "$SUPERSET_URL/api/v1/dataset/?q=(filters:!((col:table_name,opr:eq,value:$NAME)))" -H "$AUTH_HEADER" | jq -r ".result[] | select(.table_name == \"$NAME\") | .id")
    if [ -n "$EXISTING" ]; then echo "$EXISTING"; return; fi

    RESP=$(curl -s -X POST "$SUPERSET_URL/api/v1/dataset/" -H "$AUTH_HEADER" -H "Content-Type: application/json" -d "{\"database\": $DB_ID, \"table_name\": \"$NAME\", \"sql\": \"$SQL\", \"schema\": \"public\"}")
    echo "$RESP" | jq -r '.id'
}

create_chart() {
    local NAME=$1; local DS=$2; local PARAMS=$3
    PAYLOAD=$(jq -n --arg name "$NAME" --argjson ds "$DS" --arg params "$PARAMS" '{slice_name: $name, datasource_id: $ds, datasource_type: "table", viz_type: "pie", params: $params}')
    RESP=$(curl -s -X POST "$SUPERSET_URL/api/v1/chart/" -H "$AUTH_HEADER" -H "Content-Type: application/json" -d "$PAYLOAD")
    echo "$RESP" | jq -r '.id'
}

for TENANT in "VIDP" "LIRN"; do
    LOWER=$(echo "$TENANT" | tr '[:upper:]' '[:lower:]')
    echo "   📍 Tenant: $TENANT"
    
    DS_F=$(create_dataset "flights_$LOWER" "SELECT * FROM flights WHERE tenant_code = '$TENANT'")
    DS_V=$(create_dataset "vehicles_$LOWER" "SELECT * FROM vehicles WHERE tenant_code = '$TENANT'")
    
    # Use explicit ad-hoc metric to avoid "Field may not be null" error
    METRIC_COUNT='{"expressionType": "SQL", "sqlExpression": "COUNT(*)", "label": "Count"}'
    
    PARAMS_F=$(jq -n --argjson metric "$METRIC_COUNT" '{"metric": $metric, "groupby": ["status"], "adhoc_filters": [], "row_limit": 100}')
    CHART_F=$(create_chart "Flights ($TENANT)" "$DS_F" "$PARAMS_F")
    
    PARAMS_V=$(jq -n --argjson metric "$METRIC_COUNT" '{"metric": $metric, "groupby": ["vehicle_type"], "adhoc_filters": [], "row_limit": 100}')
    CHART_V=$(create_chart "Vehicles ($TENANT)" "$DS_V" "$PARAMS_V")
    
    # Check if dashboard exists
    DASH_EXIST=$(curl -s -X GET "$SUPERSET_URL/api/v1/dashboard/?q=(filters:!((col:slug,opr:eq,value:tam_ops_$LOWER)))" -H "$AUTH_HEADER" | jq -r ".result[] | select(.slug == \"tam_ops_$LOWER\") | .id")
    
    if [ -z "$DASH_EXIST" ]; then
        DASH_RESP=$(curl -s -X POST "$SUPERSET_URL/api/v1/dashboard/" -H "$AUTH_HEADER" -H "Content-Type: application/json" -d "{\"dashboard_title\": \"TAM Ops - $TENANT\", \"slug\": \"tam_ops_$LOWER\", \"published\": true}")
        DASH_ID=$(echo "$DASH_RESP" | jq -r '.id')
        echo "     ✅ Created Dashboard: tam_ops_$LOWER (ID: $DASH_ID). Charts: $CHART_F, $CHART_V created."
    else
        echo "     ⚠️ Dashboard tam_ops_$LOWER already exists."
    fi
done

#!/bin/bash
# Create Superset Assets (Datasets, Charts, Dashboard)

set -e
SUPERSET_URL="http://localhost:8089"

echo "📊 Creating Superset Assets..."

# 1. Login
TOKEN=$(curl -s -X POST "$SUPERSET_URL/api/v1/security/login" \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin", "provider": "db"}' | jq -r '.access_token')

if [ -z "$TOKEN" ] || [ "$TOKEN" == "null" ]; then
    echo "   ❌ Failed to login."
    exit 1
fi
AUTH_HEADER="Authorization: Bearer $TOKEN"

# 2. Get Database ID
DB_ID=$(curl -s -X GET "$SUPERSET_URL/api/v1/database/" -H "$AUTH_HEADER" | jq -r '.result[] | select(.database_name == "TimescaleDB") | .id')
echo "   ✅ Found Database ID: $DB_ID" >&2

# 3. Create Datasets
create_dataset() {
    local TABLE=$1
    echo "   Creating Dataset for table: $TABLE" >&2
    
    EXISTING=$(curl -s -X GET "$SUPERSET_URL/api/v1/dataset/?q=(filters:!((col:table_name,opr:eq,value:$TABLE)))" -H "$AUTH_HEADER" | jq -r ".result[] | select(.table_name == \"$TABLE\") | .id")
    
    if [ -n "$EXISTING" ]; then
        echo "   ⚠️ Dataset $TABLE already exists (ID: $EXISTING)" >&2
        echo "$EXISTING"
        return
    fi
    
    RESP=$(curl -s -X POST "$SUPERSET_URL/api/v1/dataset/" \
        -H "$AUTH_HEADER" \
        -H "Content-Type: application/json" \
        -d "{
            \"database\": $DB_ID,
            \"schema\": \"public\",
            \"table_name\": \"$TABLE\"
        }")
    
    ID=$(echo "$RESP" | jq -r '.id')
    echo "   ✅ Created Dataset $TABLE (ID: $ID)" >&2
    echo "$ID"
}

FLIGHTS_ID=$(create_dataset "flights") 
VEHICLES_ID=$(create_dataset "vehicles") 
EVENTS_ID=$(create_dataset "turnaround_events")

# 4. Create Charts
create_chart() {
    local NAME=$1
    local DS_ID=$2
    local VIZ=$3
    local PARAMS_JSON=$4
    
    echo "   Creating Chart: $NAME (DS: $DS_ID)" >&2
    
    if [ -z "$DS_ID" ] || [ "$DS_ID" == "null" ]; then
        echo "   ❌ Invalid Dataset ID for chart $NAME" >&2
        return
    fi
    
    # Construct Payload
    PAYLOAD=$(jq -n \
      --arg name "$NAME" \
      --argjson ds_id "$DS_ID" \
      --arg viz "$VIZ" \
      --arg params "$PARAMS_JSON" \
      '{slice_name: $name, datasource_id: $ds_id, datasource_type: "table", viz_type: $viz, params: $params}')

    RESP=$(curl -s -X POST "$SUPERSET_URL/api/v1/chart/" \
        -H "$AUTH_HEADER" \
        -H "Content-Type: application/json" \
        -d "$PAYLOAD")
    
    # Check if 400/500
    MSG=$(echo "$RESP" | jq -r '.message // empty')
    if [ -n "$MSG" ]; then
         echo "   ⚠️ Failed to create chart. Message: $MSG" >&2
         # echo "Payload: $PAYLOAD" >&2
    else
         ID=$(echo "$RESP" | jq -r '.id')
         echo "   ✅ Created Chart (ID: $ID)" >&2
         echo "$ID"
    fi
}

# Params as Raw String (not JSON encoded yet)
PARAMS_FLIGHT_STATUS='{"metrics": ["count"], "groupby": ["status"], "adhoc_filters": [], "row_limit": 100}'
CHART_1=$(create_chart "Flights by Status" "$FLIGHTS_ID" "pie" "$PARAMS_FLIGHT_STATUS")

PARAMS_VEHICLE_TYPE='{"metrics": ["count"], "groupby": ["vehicletype"], "adhoc_filters": [], "row_limit": 100}'
CHART_2=$(create_chart "Vehicle Types" "$VEHICLES_ID" "pie" "$PARAMS_VEHICLE_TYPE")

# 5. Create Dashboard
echo "   Creating Dashboard..." >&2
slug="tam_ops_$(date +%s)" # unique slug
DASH_PAYLOAD=$(jq -n --arg slug "$slug" '{dashboard_title: "TAM Operations Dashboard", published: true, slug: $slug}')

DASH_RESP=$(curl -s -X POST "$SUPERSET_URL/api/v1/dashboard/" \
    -H "$AUTH_HEADER" \
    -H "Content-Type: application/json" \
    -d "$DASH_PAYLOAD")

DASH_ID=$(echo "$DASH_RESP" | jq -r '.id')

if [ "$DASH_ID" == "null" ]; then
    echo "   ⚠️ Failed to create dashboard. Response: $DASH_RESP" >&2
else
    echo "   ✅ Created Dashboard 'TAM Operations Dashboard' (ID: $DASH_ID)" >&2
fi

echo "🎉 Created Assets."

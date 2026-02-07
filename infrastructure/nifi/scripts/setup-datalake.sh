#!/bin/bash
# Setup Data Lake Flows (MinIO Storage)

set -e
NIFI_URL="http://localhost:8091/nifi-api"

echo "🌊 Setting up Data Lake (MinIO) flows..."

# Get Process Groups
ROOT_PG=$(curl -s "$NIFI_URL/flow/process-groups/root" | jq -r '.processGroupFlow.id')
PG_LIST=$(curl -s "$NIFI_URL/process-groups/$ROOT_PG/process-groups")
ADSB_PG=$(echo "$PG_LIST" | jq -r '.processGroups[] | select(.component.name == "ADSB Ingestion") | .id')
VEHICLE_PG=$(echo "$PG_LIST" | jq -r '.processGroups[] | select(.component.name == "Vehicle Ingestion") | .id')
CV_PG=$(echo "$PG_LIST" | jq -r '.processGroups[] | select(.component.name == "CV Event Ingestion") | .id')

# Function to add S3 storage
add_s3_storage() {
    local PG_ID=$1
    local FOLDER_NAME=$2
    
    echo "   Configuring $FOLDER_NAME in PG: $PG_ID"
    
    # Check if PutS3Object already exists
    EXISTING=$(curl -s "$NIFI_URL/process-groups/$PG_ID/processors" | jq -r '.processors[] | select(.component.type | contains("PutS3Object")) | .id')
    if [ -n "$EXISTING" ]; then
        echo "   ⚠️ PutS3Object already exists: $EXISTING"
        return
    fi

    # Create PutS3Object
    S3_PROC=$(curl -s -X POST "$NIFI_URL/process-groups/$PG_ID/processors" \
      -H "Content-Type: application/json" \
      -d '{
        "revision":{"version":0},
        "component":{
            "type":"org.apache.nifi.processors.aws.s3.PutS3Object",
            "name":"Store in MinIO",
            "position":{"x":500,"y":300},
            "config":{
                "properties":{
                    "Object Key": "'$FOLDER_NAME'/${now():format('\''yyyy-MM-dd'\'')}/${uuid}.json",
                    "Bucket": "tam-raw-data",
                    "Access Key": "minioadmin",
                    "Secret Key": "minioadmin",
                    "Endpoint URL": "http://tam-minio:9000",
                    "Signer Override": "Default Signature" 
                },
                "autoTerminatedRelationships": ["failure"]
            }
        }
      }' | jq -r '.id')
      
    echo "   ✅ Created PutS3Object: $S3_PROC"
    
    # Get ListenHTTP ID
    LISTEN_PROC=$(curl -s "$NIFI_URL/process-groups/$PG_ID/processors" | jq -r '.processors[] | select(.component.type | contains("ListenHTTP")) | .id')
    
    # Connect ListenHTTP -> PutS3Object
    curl -s -X POST "$NIFI_URL/process-groups/$PG_ID/connections" \
        -H "Content-Type: application/json" \
        -d "{
            \"revision\": {\"version\": 0},
            \"component\": {
                \"source\": {\"id\": \"$LISTEN_PROC\", \"type\": \"PROCESSOR\", \"groupId\": \"$PG_ID\"},
                \"destination\": {\"id\": \"$S3_PROC\", \"type\": \"PROCESSOR\", \"groupId\": \"$PG_ID\"},
                \"selectedRelationships\": [\"success\"]
            }
        }" > /dev/null
        
    echo "   ✅ Connected ListenHTTP -> PutS3Object"
    
    # Start PutS3Object
    curl -s -X PUT "$NIFI_URL/processors/$S3_PROC/run-status" \
        -H "Content-Type: application/json" \
        -d "{\"revision\":{\"version\":0},\"state\":\"RUNNING\"}" > /dev/null
        
    echo "   ✅ Started PutS3Object"
}

if [ -n "$ADSB_PG" ]; then add_s3_storage "$ADSB_PG" "adsb"; fi
if [ -n "$VEHICLE_PG" ]; then add_s3_storage "$VEHICLE_PG" "vehicle"; fi
if [ -n "$CV_PG" ]; then add_s3_storage "$CV_PG" "cv"; fi

echo "🎉 Data Lake flows configured!"

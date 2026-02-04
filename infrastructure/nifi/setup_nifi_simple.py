#!/usr/bin/env python3
"""
Simple NiFi setup script that creates flows without problematic HTTP Context Map.
"""
import requests

NIFI_URL = "http://localhost:8091/nifi-api"
REDPANDA = "redpanda:29092"

def get_root_pg():
    r = requests.get(f"{NIFI_URL}/flow/process-groups/root")
    return r.json()['processGroupFlow']['id']

def create_pg(parent_id, name, x=100, y=100):
    r = requests.post(f"{NIFI_URL}/process-groups/{parent_id}/process-groups",
        headers={"Content-Type": "application/json"},
        json={"revision":{"version":0},"component":{"name":name,"position":{"x":x,"y":y}}})
    return r.json().get('id')

def create_listen_http(pg_id, name, port, path):
    r = requests.post(f"{NIFI_URL}/process-groups/{pg_id}/processors",
        headers={"Content-Type": "application/json"},
        json={
            "revision":{"version":0},
            "component":{
                "type":"org.apache.nifi.processors.standard.ListenHTTP",
                "name":name,
                "position":{"x":100,"y":100},
                "config":{"properties":{"Listening Port":port,"Base Path":path}}
            }
        })
    return r.json().get('id')

def create_publish_kafka(pg_id, name, topic):
    r = requests.post(f"{NIFI_URL}/process-groups/{pg_id}/processors",
        headers={"Content-Type": "application/json"},
        json={
            "revision":{"version":0},
            "component":{
                "type":"org.apache.nifi.processors.kafka.pubsub.PublishKafka_2_6",
                "name":name,
                "position":{"x":100,"y":300},
                "config":{"properties":{
                    "bootstrap.servers":REDPANDA,
                    "topic":topic,
                    "use-transactions":"false"
                }}
            }
        })
    return r.json().get('id')

def create_connection(pg_id, src_id, dest_id, relationships):
    r = requests.post(f"{NIFI_URL}/process-groups/{pg_id}/connections",
        headers={"Content-Type": "application/json"},
        json={
            "revision":{"version":0},
            "component":{
                "source":{"id":src_id,"type":"PROCESSOR"},
                "destination":{"id":dest_id,"type":"PROCESSOR"},
                "selectedRelationships":relationships
            }
        })
    return r.json().get('id')

def start_pg(pg_id):
    requests.put(f"{NIFI_URL}/flow/process-groups/{pg_id}",
        headers={"Content-Type": "application/json"},
        json={"id":pg_id,"state":"RUNNING"})

def setup_flow(root, name, x, port, path, topic):
    print(f"Setting up {name}...")
    pg_id = create_pg(root, name, x, 100)
    if not pg_id:
        print(f"  Failed to create process group {name}")
        return
    print(f"  Created PG: {pg_id}")
    
    http_id = create_listen_http(pg_id, f"Listen {name}", port, path)
    if not http_id:
        print(f"  Failed to create ListenHTTP")
        return
    print(f"  Created ListenHTTP: {http_id}")
    
    kafka_id = create_publish_kafka(pg_id, f"Publish {name}", topic)
    if not kafka_id:
        print(f"  Failed to create PublishKafka")
        return
    print(f"  Created PublishKafka: {kafka_id}")
    
    conn_id = create_connection(pg_id, http_id, kafka_id, ["success"])
    if conn_id:
        print(f"  Created connection: {conn_id}")
    
    start_pg(pg_id)
    print(f"  Started {name}")

if __name__ == "__main__":
    try:
        root = get_root_pg()
        print(f"Root PG: {root}")
        
        setup_flow(root, "ADSB Ingestion", 100, "8092", "adsb-ingest", "flight-raw-json")
        setup_flow(root, "Vehicle Ingestion", 400, "8093", "vehicle-ingest", "vehicle-raw-json")
        setup_flow(root, "CV Event Ingestion", 700, "8094", "cv-event-ingest", "cv-event-raw-json")
        
        print("\n✅ NiFi setup complete!")
    except Exception as e:
        print(f"Error: {e}")
        import traceback
        traceback.print_exc()

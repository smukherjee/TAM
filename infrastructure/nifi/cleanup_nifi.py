import requests
import json
import time

NIFI_API = "http://localhost:8091/nifi-api"

def get_root_pg():
    resp = requests.get(f"{NIFI_API}/flow/process-groups/root")
    return resp.json()['processGroupFlow']['id']

def list_pgs(root_id):
    resp = requests.get(f"{NIFI_API}/flow/process-groups/{root_id}")
    return resp.json()['processGroupFlow']['flow']['processGroups']

def stop_pg(pg_id):
    # To stop a PG, we need to update its state to STOPPED
    # First get current revision
    resp = requests.get(f"{NIFI_API}/process-groups/{pg_id}")
    pg_data = resp.json()
    version = pg_data['revision']['version']
    
    payload = {
        "id": pg_id,
        "state": "STOPPED",
        "revision": {
            "version": version
        },
        "disconnectedNodeAcknowledged": False
    }
    
    requests.put(f"{NIFI_API}/flow/process-groups/{pg_id}", json=payload)
    print(f"Stopped PG {pg_id}")
    time.sleep(2) # Wait for stop to propagate

def empty_queues(pg_id):
    # Get all connections in the PG
    resp = requests.get(f"{NIFI_API}/flow/process-groups/{pg_id}")
    connections = resp.json()['processGroupFlow']['flow']['connections']
    
    for conn in connections:
        conn_id = conn['id']
        # Create drop request
        requests.post(f"{NIFI_API}/flowfile-queues/{conn_id}/drop-requests")
        print(f"Initiated drop request for connection {conn_id}")
    
    if connections:
        time.sleep(2) # Wait for drops to process

def delete_pg(pg_id):
    # Empty queues first
    empty_queues(pg_id)
    
    # Get revision
    resp = requests.get(f"{NIFI_API}/process-groups/{pg_id}")
    if resp.status_code != 200:
        return
    version = resp.json()['revision']['version']
    
    resp = requests.delete(f"{NIFI_API}/process-groups/{pg_id}?version={version}")
    if resp.status_code == 200:
        print(f"Deleted PG {pg_id}")
    else:
        print(f"Failed to delete PG {pg_id}: {resp.text}")

def main():
    try:
        root_id = get_root_pg()
        pgs = list_pgs(root_id)
        
        for pg in pgs:
            pg_id = pg['id']
            name = pg['component']['name']
            print(f"Processing {name} ({pg_id})...")
            
            # Stop everything in the PG first (recursively ideally, but let's try PG state)
            stop_pg(pg_id)
            time.sleep(1) # Wait for stop
            delete_pg(pg_id)
            
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    main()

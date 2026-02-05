#!/usr/bin/env sh
set -eu
SUPERSET_URL=${SUPERSET_URL:-"http://superset:8088"}

echo "⏳ Waiting for Superset at ${SUPERSET_URL}..."

# Wait for Superset to be healthy using Python (built-in libraries)
python3 -c "
import time
import urllib.request
import sys

url = '${SUPERSET_URL}/health'
print(f'Checking {url}...')

for i in range(120):
    try:
        with urllib.request.urlopen(url) as response:
            if response.status == 200:
                print('✅ Superset healthy')
                sys.exit(0)
    except Exception as e:
        print(f'Waiting... ({e})')
        time.sleep(2)

print('❌ Superset not healthy after 240s')
sys.exit(1)
"

# Run provisioning (Python script handles Datasets, Charts, Dashboards, and Seeding)
echo "🚀 Starting Superset provisioning..."
python3 /app/provision_superset.py

echo "🎯 Superset bootstrap sidecar complete"

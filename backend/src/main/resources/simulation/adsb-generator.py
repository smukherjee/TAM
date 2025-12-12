import requests
import time
import random
import uuid
import json
from datetime import datetime

NIFI_URL = "http://localhost:8091/api/adsblivedata"
NUM_FLIGHTS = 10

def generate_flight(index):
    return {
        "LivePlotId": str(uuid.uuid4()),
        "Time": datetime.utcnow().isoformat() + "Z",
        "CallSign": f"FLT{index:03d}",
        "Lat": 28.5562 + random.uniform(-0.1, 0.1),
        "Lon": 77.1000 + random.uniform(-0.1, 0.1),
        "Speed": random.uniform(200, 500),
        "Heading": random.uniform(0, 360),
        "Altitude": random.uniform(1000, 30000),
        "Status": "Airborne",
        "TrackId": f"TRK{index:03d}",
        "ModeSId": f"MODE{index:03d}",
        "FlightLevel": random.uniform(100, 300),
        "ROC": random.uniform(-1000, 1000),
        "SSR": "1234",
        "SafetyAlert": False,
        "SystemStatus": "OK",
        "Spi": False,
        "UpdateType": "Position"
    }

def main():
    print(f"Starting Mock ADSB Generator targeting {NIFI_URL}")
    while True:
        flights = [generate_flight(i) for i in range(NUM_FLIGHTS)]
        try:
            response = requests.post(NIFI_URL, json=flights)
            if response.status_code == 200:
                print(f"Sent {len(flights)} flights. Status: {response.status_code}")
            else:
                print(f"Failed to send flights. Status: {response.status_code}")
        except Exception as e:
            print(f"Error sending data: {e}")
        
        time.sleep(2)

if __name__ == "__main__":
    main()

import requests
import time
import random
import json
from datetime import datetime, timedelta

NIFI_URL = "http://localhost:8091/api/veh_live_data_con"

VEHICLE_CONFIGS = [
    {"name": "TMD-000013", "no": "PBT11", "type": "Compactor"},
    {"name": "TMD254HV-000009", "no": "BFL30", "type": "SUV"},
    {"name": "BUS-101", "no": "DL1PC0001", "type": "BUS"},
    {"name": "TRUCK-55", "no": "HR55X9999", "type": "TRUCK"}
]

def generate_vehicle_data():
    config = random.choice(VEHICLE_CONFIGS)
    now = datetime.now()
    
    # Random lat/lon around IGIA (New Delhi)
    base_lat = 28.5562
    base_lon = 77.1000
    
    vehicle = {
        "vehicleName": config["name"],
        "vehicleNo": config["no"],
        "type": config["type"],
        "company": "Phase3_DIAL",
        "branch": "Phase3_DIAL",
        "temperature": "--",
        "gps": "ON",
        "door1": "--",
        "door2": "--",
        "door3": "--",
        "door4": "--",
        "timestamp": now.isoformat(),
        "gpsActualTime": (now - timedelta(seconds=1)).strftime("%d-%m-%Y %H:%M:%S"),
        "status": "RUNNING" if random.choice([True, False]) else "IDLE",
        "deviceModel": "MT4G-CANV2-MQTT",
        "latitude": base_lat + (random.random() - 0.5) * 0.02,
        "longitude": base_lon + (random.random() - 0.5) * 0.02,
        "speed": random.random() * 100,
        "ac": "--",
        "imeiNo": "359214420" + str(100000 + random.randint(0, 900000)),
        "odometer": str(100000 + random.randint(0, 10000)),
        "poi": "--",
        "driverFirstName": "--",
        "driverMiddleName": "--",
        "driverLastName": "--",
        "immobilizeState": "--",
        "ign": "ON",
        "angle": random.random() * 360,
        "sos": "--",
        "fuel": [],
        "batteryPercentage": "0",
        "externalVolt": "28.00" if random.choice([True, False]) else "12.70",
        "power": "ON",
        "altitude": 0.0,
        "location": "IGIA, New Delhi"
    }
    return vehicle

def main():
    print(f"Starting Mock Telit Generator targeting {NIFI_URL}")
    while True:
        # Java code sends a list containing a single vehicle object
        payload = [generate_vehicle_data()]
        try:
            response = requests.post(NIFI_URL, json=payload)
            if response.status_code == 200:
                print(f"Sent vehicle data: {payload[0]['vehicleName']}")
            else:
                print(f"Failed to send vehicle data. Status: {response.status_code}")
        except Exception as e:
            print(f"Error sending data: {e}")
        
        time.sleep(3)

if __name__ == "__main__":
    main()

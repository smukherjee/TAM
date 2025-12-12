import requests
import time
import random
import uuid
import json
from datetime import datetime, timedelta

NIFI_URL = "http://localhost:8091/api/cv/events"

ACTIVITY_TYPES = [
    "Passenger Boarding Bridge", "Passenger Step Ladder Front", "Passenger Step Ladder Back",
    "Towable Conveyor Belt Front", "Towable Conveyor Belt Back", "Aircraft Back Door",
    "Aircraft Front Door", "Aircraft Front Belly", "Aircraft Back Belly", "Passenger Coach",
    "Person Arrival Movement", "Person Departure Movement", "Head Unit (EBT) / Diesel Tug (DT)",
    "Bag Movement Arrival", "Bag Movement Departure", "Fuel Vehicle", "Water Cart",
    "Toilet Cart", "Ambulance", "Push Back Tug"
]

STANDS = ["D7", "B106", "C005", "A12", "E4"]

# Stand -> {Activity: StartTime}
stand_activity_state = {}

def pick_random_inactive_activity(active_types):
    candidates = [a for a in ACTIVITY_TYPES if a not in active_types]
    if not candidates:
        return None
    return random.choice(candidates)

def create_event(stand, activity_type, event_type):
    cam_id = str(random.randint(1, 10))
    
    # Randomly format to test normalization (snake_case vs Title Case)
    if random.choice([True, False]):
        formatted_activity = activity_type.lower().replace(" ", "_").replace("/", "").replace("(", "").replace(")", "")
    else:
        formatted_activity = activity_type

    return {
        "eventUniqueId": str(uuid.uuid4()),
        "cameraId": cam_id,
        "cameraName": f"Cam-{cam_id}",
        "activityType": formatted_activity,
        "eventType": event_type,
        "eventTimeStamp": datetime.now().isoformat(),
        "stand": stand
    }

def process_stand(stand):
    if stand not in stand_activity_state:
        stand_activity_state[stand] = {}
    
    active_activities = stand_activity_state[stand]
    events_to_send = []
    
    # 1. Try to STOP active activities
    # Create a list of keys to iterate over safely while modifying the dictionary
    for activity in list(active_activities.keys()):
        start_time = active_activities[activity]
        
        # Min duration 10s
        if datetime.now() > start_time + timedelta(seconds=10):
            # 20% chance to stop
            if random.random() < 0.2:
                events_to_send.append(create_event(stand, activity, 1)) # STOP
                del active_activities[activity]
    
    # 2. Try to START new activities (max 3 concurrent)
    if len(active_activities) < 3:
        # 30% chance to start a new one
        if random.random() < 0.3:
            new_activity = pick_random_inactive_activity(active_activities.keys())
            if new_activity:
                events_to_send.append(create_event(stand, new_activity, 0)) # START
                active_activities[new_activity] = datetime.now()
                
    return events_to_send

def main():
    print(f"Starting Mock CV Event Generator targeting {NIFI_URL}")
    while True:
        all_events = []
        for stand in STANDS:
            events = process_stand(stand)
            all_events.extend(events)
        
        for event in all_events:
            try:
                # Java sends one by one in a list
                payload = [event]
                print(f"Generated CV event: {event['activityType']} ({'START' if event['eventType'] == 0 else 'STOP'}) at {event['stand']}")
                response = requests.post(NIFI_URL, json=payload)
                if response.status_code != 200:
                    print(f"Failed to send event. Status: {response.status_code}")
            except Exception as e:
                print(f"Error sending event: {e}")
        
        time.sleep(2)

if __name__ == "__main__":
    main()

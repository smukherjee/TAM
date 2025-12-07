package com.utam.controller;

import com.utam.model.Flight;
import com.utam.model.Vehicle;
import com.utam.model.TurnaroundEvent;
import com.utam.model.dto.CvEventDto;
import com.utam.model.dto.TajSatsVehicleDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@RestController
public class IngestionController {

    private static final Logger log = LoggerFactory.getLogger(IngestionController.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final DateTimeFormatter TAJSATS_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH);
    private static final DateTimeFormatter CV_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public IngestionController(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping("/api/adsblivedata")
    public void ingestFlightData(@RequestBody List<Flight> flights) {
        log.info("Received {} flight records via HTTP ingestion", flights.size());
        for (Flight flight : flights) {
            kafkaTemplate.send("flight-raw-json", flight.getCallsign(), flight);
        }
    }

    @PostMapping("/api/veh_live_data_con")
    public void ingestVehicleData(@RequestBody List<Vehicle> vehicles) {
        log.info("Received {} vehicle records via HTTP ingestion", vehicles.size());
        for (Vehicle vehicle : vehicles) {
            kafkaTemplate.send("vehicle-raw-json", vehicle.getVehicleNo(), vehicle);
        }
    }

    @PostMapping("/api/tajsats")
    public void ingestTajSatsData(@RequestBody List<TajSatsVehicleDto> dtos) {
        log.info("Received {} TAjSats vehicle records via HTTP ingestion", dtos.size());
        for (TajSatsVehicleDto dto : dtos) {
            try {
                Vehicle vehicle = new Vehicle();
                vehicle.setVehicleNo(dto.getVehicleNo());
                vehicle.setVehicleName(dto.getVehicleName());
                vehicle.setType(dto.getVehicleType());
                
                // Parse numeric fields
                vehicle.setLatitude(parseDouble(dto.getLatitude()));
                vehicle.setLongitude(parseDouble(dto.getLongitude()));
                vehicle.setSpeed(parseDouble(dto.getSpeed()));
                vehicle.setAngle(parseDouble(dto.getAngle()));
                vehicle.setAltitude(0.0); // Default as not in source
                
                vehicle.setStatus(dto.getStatus());
                vehicle.setCompany(dto.getCompany());
                vehicle.setBranch(dto.getBranch());
                vehicle.setTemperature(dto.getTemperature());
                vehicle.setGps(dto.getGps());
                vehicle.setDeviceModel(dto.getDeviceModel());
                vehicle.setImeiNo(dto.getImeiNo());
                vehicle.setOdometer(dto.getOdometer());
                vehicle.setPoi(dto.getPoi());
                vehicle.setIgn(dto.getIgn());
                vehicle.setBatteryPercentage(dto.getBatteryPercentage());
                vehicle.setExternalVolt(dto.getExternalVolt());
                vehicle.setPower(dto.getPower());
                vehicle.setLocation(dto.getLocation());
                
                // Set defaults for missing fields
                vehicle.setDoor1("--");
                vehicle.setDoor2("--");
                vehicle.setDoor3("--");
                vehicle.setDoor4("--");
                vehicle.setSos("--");
                vehicle.setImmobilizeState("--");
                
                // Handle timestamps
                vehicle.setGpsActualTime(dto.getGpsActualTime());
                if (dto.getDatetime() != null) {
                    try {
                        vehicle.setTimestamp(LocalDateTime.parse(dto.getDatetime(), TAJSATS_DATE_FORMATTER));
                    } catch (Exception e) {
                        log.warn("Failed to parse datetime '{}' for vehicle {}", dto.getDatetime(), dto.getVehicleNo());
                        vehicle.setTimestamp(LocalDateTime.now());
                    }
                } else {
                    vehicle.setTimestamp(LocalDateTime.now());
                }

                kafkaTemplate.send("vehicle-raw-json", vehicle.getVehicleNo(), vehicle);
            } catch (Exception e) {
                log.error("Error processing TAjSats record for vehicle {}: {}", dto.getVehicleNo(), e.getMessage());
            }
        }
    }

    @PostMapping("/api/cv/events")
    public void ingestCvEvents(@RequestBody List<CvEventDto> dtos) {
        log.info("Received {} CV event records via HTTP ingestion", dtos.size());
        for (CvEventDto dto : dtos) {
            try {
                TurnaroundEvent event = new TurnaroundEvent();
                event.setEventUniqueId(dto.getEventUniqueId());
                event.setCameraId(dto.getCameraId());
                event.setCameraName(dto.getCameraName());
                event.setEventType(dto.getEventType());
                event.setStand(dto.getStand());
                
                // Map activity type
                event.setActivityType(mapActivityType(dto.getActivityType()));
                
                // Parse timestamp
                if (dto.getEventTimeStamp() != null) {
                    try {
                        event.setEventTimeStamp(LocalDateTime.parse(dto.getEventTimeStamp(), CV_DATE_FORMATTER));
                    } catch (Exception e) {
                        log.warn("Failed to parse datetime '{}' for event {}", dto.getEventTimeStamp(), dto.getEventUniqueId());
                        event.setEventTimeStamp(LocalDateTime.now());
                    }
                } else {
                    event.setEventTimeStamp(LocalDateTime.now());
                }

                kafkaTemplate.send("turnaround-raw-json", event.getEventUniqueId(), event);
            } catch (Exception e) {
                log.error("Error processing CV event {}: {}", dto.getEventUniqueId(), e.getMessage());
            }
        }
    }

    private Double parseDouble(String value) {
        if (value == null || value.isEmpty()) return 0.0;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private String mapActivityType(String rawType) {
        if (rawType == null) return "Unknown";
        
        // Normalize: replace underscores with spaces and capitalize words
        // Example: "push_back_vehicle" -> "Push Back Vehicle"
        // The user provided specific mappings, but without a full map, we'll do a best-effort formatting
        // or specific overrides if needed.
        
        switch (rawType.toLowerCase()) {
            case "push_back_vehicle": return "Push Back Tug";
            case "passenger_boarding_bridge": return "Passenger Boarding Bridge";
            case "passenger_step_ladder_front": return "Passenger Step Ladder Front";
            case "passenger_step_ladder_back": return "Passenger Step Ladder Back";
            case "towable_conveyor_belt_front": return "Towable Conveyor Belt Front";
            case "towable_conveyor_belt_back": return "Towable Conveyor Belt Back";
            case "aircraft_back_door": return "Aircraft Back Door";
            case "aircraft_front_door": return "Aircraft Front Door";
            case "aircraft_front_belly": return "Aircraft Front Belly";
            case "aircraft_back_belly": return "Aircraft Back Belly";
            case "passenger_coach": return "Passenger Coach";
            case "person_arrival_movement": return "Person Arrival Movement";
            case "person_departure_movement": return "Person Departure Movement";
            case "head_unit": return "Head Unit (EBT) / Diesel Tug (DT)";
            case "bag_movement_arrival": return "Bag Movement Arrival";
            case "bag_movement_departure": return "Bag Movement Departure";
            case "fuel_vehicle": return "Fuel Vehicle";
            case "water_cart": return "Water Cart";
            case "toilet_cart": return "Toilet Cart";
            case "ambulance": return "Ambulance";
            default: 
                // Fallback: Capitalize words
                String[] words = rawType.split("_");
                StringBuilder sb = new StringBuilder();
                for (String word : words) {
                    if (word.length() > 0) {
                        sb.append(Character.toUpperCase(word.charAt(0)))
                          .append(word.substring(1).toLowerCase())
                          .append(" ");
                    }
                }
                return sb.toString().trim();
        }
    }
}

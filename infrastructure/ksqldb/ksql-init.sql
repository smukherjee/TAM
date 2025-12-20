-- =========================================================
-- ksqlDB Initialization Script for TAM Dev Environment
-- Creates streams and tables for real-time data processing
-- =========================================================

-- =========================================================
-- 1. FLIGHT STREAM - Raw flight data from Kafka
-- =========================================================
CREATE STREAM IF NOT EXISTS flights_stream (
    LivePlotId VARCHAR KEY,
    Time DOUBLE,
    CallSign VARCHAR,
    Lat DOUBLE,
    Lon DOUBLE,
    Speed DOUBLE,
    Heading DOUBLE,
    Altitude DOUBLE,
    Status VARCHAR,
    TrackId VARCHAR,
    ModeSId VARCHAR,
    FlightLevel DOUBLE,
    ROC DOUBLE,
    SSR VARCHAR,
    SafetyAlert BOOLEAN,
    SystemStatus VARCHAR,
    Spi BOOLEAN,
    UpdateType VARCHAR
) WITH (
    KAFKA_TOPIC = 'flight-raw-json',
    VALUE_FORMAT = 'JSON'
);

-- =========================================================
-- 2. VEHICLE STREAM - Raw vehicle data from Kafka
-- =========================================================
CREATE STREAM IF NOT EXISTS vehicles_stream (
    vehicle_no VARCHAR KEY,
    vehicletype VARCHAR,
    latitude VARCHAR,
    longitude VARCHAR,
    speed VARCHAR,
    altitude VARCHAR,
    status VARCHAR,
    datetime VARCHAR,
    vehicle_name VARCHAR,
    company VARCHAR,
    temperature VARCHAR,
    gps VARCHAR,
    branch VARCHAR,
    gpsactualtime VARCHAR,
    devicemodel VARCHAR,
    ign VARCHAR,
    angle VARCHAR,
    location VARCHAR
) WITH (
    KAFKA_TOPIC = 'vehicle-raw-json',
    VALUE_FORMAT = 'JSON'
);

-- =========================================================
-- 3. SPEED VIOLATIONS STREAM - CEP: Vehicles exceeding 70 km/h
-- =========================================================
CREATE STREAM IF NOT EXISTS speed_violations AS
    SELECT 
        vehicle_no,
        vehicletype,
        CAST(latitude AS DOUBLE) AS latitude,
        CAST(longitude AS DOUBLE) AS longitude,
        CAST(speed AS DOUBLE) AS speed,
        vehicle_name,
        company,
        datetime,
        'SPEED_VIOLATION' AS alert_type
    FROM vehicles_stream
    WHERE CAST(speed AS DOUBLE) > 70.0
    EMIT CHANGES;

-- =========================================================
-- 4. FLIGHTS AGGREGATION - Flight count per 5-minute window
-- =========================================================
CREATE TABLE IF NOT EXISTS flights_5min_count AS
    SELECT 
        CallSign,
        COUNT(*) AS update_count,
        LATEST_BY_OFFSET(Altitude) AS latest_altitude,
        LATEST_BY_OFFSET(Speed) AS latest_speed,
        LATEST_BY_OFFSET(Status) AS latest_status
    FROM flights_stream
    WINDOW TUMBLING (SIZE 5 MINUTES)
    GROUP BY CallSign
    EMIT CHANGES;

-- =========================================================
-- 5. AIRBORNE FLIGHTS - Only airborne flights
-- =========================================================
CREATE STREAM IF NOT EXISTS airborne_flights AS
    SELECT *
    FROM flights_stream
    WHERE Status = 'AIRBORNE'
    EMIT CHANGES;

-- =========================================================
-- 6. HIGH SPEED FLIGHTS - Flights > 450 knots (possible alert)
-- =========================================================
CREATE STREAM IF NOT EXISTS high_speed_flights AS
    SELECT 
        LivePlotId,
        CallSign,
        Speed,
        Altitude,
        Lat,
        Lon,
        'HIGH_SPEED_FLIGHT' AS alert_type
    FROM flights_stream
    WHERE Speed > 450.0
    EMIT CHANGES;

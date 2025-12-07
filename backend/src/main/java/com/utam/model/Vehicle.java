package com.utam.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.utam.converter.StringListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @JsonProperty("vehicle_no")
    @Column(name = "vehicle_no")
    private String vehicleNo;

    @JsonProperty("vehicletype")
    @Column(name = "type")
    private String type;

    @JsonProperty("latitude")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Column(name = "latitude")
    private Double latitude;

    @JsonProperty("longitude")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Column(name = "longitude")
    private Double longitude;

    @JsonProperty("speed")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Column(name = "speed")
    private Double speed;

    @JsonProperty("altitude")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Column(name = "altitude")
    private Double altitude;

    @JsonProperty("status")
    @Column(name = "status")
    private String status;

    @JsonProperty("datetime")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @JsonProperty("vehicle_name")
    @Column(name = "vehicle_name")
    private String vehicleName;

    @JsonProperty("company")
    @Column(name = "company")
    private String company;

    @JsonProperty("temperature")
    @Column(name = "temperature")
    private String temperature;

    @JsonProperty("gps")
    @Column(name = "gps")
    private String gps;

    @JsonProperty("door1")
    @Column(name = "door1")
    private String door1;

    @JsonProperty("door2")
    @Column(name = "door2")
    private String door2;

    @JsonProperty("door3")
    @Column(name = "door3")
    private String door3;

    @JsonProperty("door4")
    @Column(name = "door4")
    private String door4;

    @JsonProperty("branch")
    @Column(name = "branch")
    private String branch;

    @JsonProperty("gpsactualtime")
    @Column(name = "gps_actual_time")
    private String gpsActualTime;

    @JsonProperty("devicemodel")
    @Column(name = "device_model")
    private String deviceModel;

    @JsonProperty("ac")
    @Column(name = "ac")
    private String ac;

    @JsonProperty("imeino")
    @Column(name = "imei_no")
    private String imeiNo;

    @JsonProperty("odometer")
    @Column(name = "odometer")
    private String odometer;

    @JsonProperty("poi")
    @Column(name = "poi")
    private String poi;

    @JsonProperty("driver_middle_name")
    @Column(name = "driver_middle_name")
    private String driverMiddleName;

    @JsonProperty("driver_first_name")
    @Column(name = "driver_first_name")
    private String driverFirstName;

    @JsonProperty("driver_last_name")
    @Column(name = "driver_last_name")
    private String driverLastName;

    @JsonProperty("immobilize_state")
    @Column(name = "immobilize_state")
    private String immobilizeState;

    @JsonProperty("ign")
    @Column(name = "ign")
    private String ign;

    @JsonProperty("angle")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Column(name = "angle")
    private Double angle;

    @JsonProperty("sos")
    @Column(name = "sos")
    private String sos;

    @JsonProperty("fuel")
    @Convert(converter = StringListConverter.class)
    @Column(name = "fuel")
    private List<String> fuel;

    @JsonProperty("battery_percentage")
    @Column(name = "battery_percentage")
    private String batteryPercentage;

    @JsonProperty("externalvolt")
    @Column(name = "external_volt")
    private String externalVolt;

    @JsonProperty("power")
    @Column(name = "power")
    private String power;

    @JsonProperty("location")
    @Column(name = "location")
    private String location;

    public Vehicle() {
    }

    public Vehicle(String vehicleNo, String type, Double latitude, Double longitude, Double speed, Double altitude, String status, LocalDateTime timestamp, String vehicleName, String company, String temperature, String gps, String door1, String door2, String door3, String door4, String branch, String gpsActualTime, String deviceModel, String ac, String imeiNo, String odometer, String poi, String driverMiddleName, String driverFirstName, String driverLastName, String immobilizeState, String ign, Double angle, String sos, List<String> fuel, String batteryPercentage, String externalVolt, String power, String location) {
        this.vehicleNo = vehicleNo;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.speed = speed;
        this.altitude = altitude;
        this.status = status;
        this.timestamp = timestamp;
        this.vehicleName = vehicleName;
        this.company = company;
        this.temperature = temperature;
        this.gps = gps;
        this.door1 = door1;
        this.door2 = door2;
        this.door3 = door3;
        this.door4 = door4;
        this.branch = branch;
        this.gpsActualTime = gpsActualTime;
        this.deviceModel = deviceModel;
        this.ac = ac;
        this.imeiNo = imeiNo;
        this.odometer = odometer;
        this.poi = poi;
        this.driverMiddleName = driverMiddleName;
        this.driverFirstName = driverFirstName;
        this.driverLastName = driverLastName;
        this.immobilizeState = immobilizeState;
        this.ign = ign;
        this.angle = angle;
        this.sos = sos;
        this.fuel = fuel;
        this.batteryPercentage = batteryPercentage;
        this.externalVolt = externalVolt;
        this.power = power;
        this.location = location;
    }

    public String getVehicleNo() {
        return vehicleNo;
    }

    public void setVehicleNo(String vehicleNo) {
        this.vehicleNo = vehicleNo;
    }

    @JsonProperty("vehicletype")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getSpeed() {
        return speed;
    }

    public void setSpeed(Double speed) {
        this.speed = speed;
    }

    public Double getAltitude() {
        return altitude;
    }

    public void setAltitude(Double altitude) {
        this.altitude = altitude;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getVehicleName() {
        return vehicleName;
    }

    public void setVehicleName(String vehicleName) {
        this.vehicleName = vehicleName;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getTemperature() {
        return temperature;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public String getGps() {
        return gps;
    }

    public void setGps(String gps) {
        this.gps = gps;
    }

    public String getDoor1() {
        return door1;
    }

    public void setDoor1(String door1) {
        this.door1 = door1;
    }

    public String getDoor2() {
        return door2;
    }

    public void setDoor2(String door2) {
        this.door2 = door2;
    }

    public String getDoor3() {
        return door3;
    }

    public void setDoor3(String door3) {
        this.door3 = door3;
    }

    public String getDoor4() {
        return door4;
    }

    public void setDoor4(String door4) {
        this.door4 = door4;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getGpsActualTime() {
        return gpsActualTime;
    }

    public void setGpsActualTime(String gpsActualTime) {
        this.gpsActualTime = gpsActualTime;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }

    public String getAc() {
        return ac;
    }

    public void setAc(String ac) {
        this.ac = ac;
    }

    public String getImeiNo() {
        return imeiNo;
    }

    public void setImeiNo(String imeiNo) {
        this.imeiNo = imeiNo;
    }

    public String getOdometer() {
        return odometer;
    }

    public void setOdometer(String odometer) {
        this.odometer = odometer;
    }

    public String getPoi() {
        return poi;
    }

    public void setPoi(String poi) {
        this.poi = poi;
    }

    public String getDriverMiddleName() {
        return driverMiddleName;
    }

    public void setDriverMiddleName(String driverMiddleName) {
        this.driverMiddleName = driverMiddleName;
    }

    public String getDriverFirstName() {
        return driverFirstName;
    }

    public void setDriverFirstName(String driverFirstName) {
        this.driverFirstName = driverFirstName;
    }

    public String getDriverLastName() {
        return driverLastName;
    }

    public void setDriverLastName(String driverLastName) {
        this.driverLastName = driverLastName;
    }

    public String getImmobilizeState() {
        return immobilizeState;
    }

    public void setImmobilizeState(String immobilizeState) {
        this.immobilizeState = immobilizeState;
    }

    public String getIgn() {
        return ign;
    }

    public void setIgn(String ign) {
        this.ign = ign;
    }

    public Double getAngle() {
        return angle;
    }

    public void setAngle(Double angle) {
        this.angle = angle;
    }

    public String getSos() {
        return sos;
    }

    public void setSos(String sos) {
        this.sos = sos;
    }

    public List<String> getFuel() {
        return fuel;
    }

    public void setFuel(List<String> fuel) {
        this.fuel = fuel;
    }

    public String getBatteryPercentage() {
        return batteryPercentage;
    }

    public void setBatteryPercentage(String batteryPercentage) {
        this.batteryPercentage = batteryPercentage;
    }

    public String getExternalVolt() {
        return externalVolt;
    }

    public void setExternalVolt(String externalVolt) {
        this.externalVolt = externalVolt;
    }

    public String getPower() {
        return power;
    }

    public void setPower(String power) {
        this.power = power;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    public String toString() {
        return "Vehicle{" +
                "vehicleNo='" + vehicleNo + '\'' +
                ", type='" + type + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", speed=" + speed +
                ", altitude=" + altitude +
                ", status='" + status + '\'' +
                ", timestamp=" + timestamp +
                ", vehicleName='" + vehicleName + '\'' +
                ", company='" + company + '\'' +
                ", temperature='" + temperature + '\'' +
                ", gps='" + gps + '\'' +
                ", door1='" + door1 + '\'' +
                ", door2='" + door2 + '\'' +
                ", door3='" + door3 + '\'' +
                ", door4='" + door4 + '\'' +
                ", branch='" + branch + '\'' +
                ", gpsActualTime='" + gpsActualTime + '\'' +
                ", deviceModel='" + deviceModel + '\'' +
                ", ac='" + ac + '\'' +
                ", imeiNo='" + imeiNo + '\'' +
                ", odometer='" + odometer + '\'' +
                ", poi='" + poi + '\'' +
                ", driverMiddleName='" + driverMiddleName + '\'' +
                ", driverFirstName='" + driverFirstName + '\'' +
                ", driverLastName='" + driverLastName + '\'' +
                ", immobilizeState='" + immobilizeState + '\'' +
                ", ign='" + ign + '\'' +
                ", angle=" + angle +
                ", sos='" + sos + '\'' +
                ", fuel=" + fuel +
                ", batteryPercentage='" + batteryPercentage + '\'' +
                ", externalVolt='" + externalVolt + '\'' +
                ", power='" + power + '\'' +
                ", location='" + location + '\'' +
                '}';
    }
}

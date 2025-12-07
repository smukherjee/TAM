package com.utam.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TajSatsVehicleDto {
    @JsonProperty("vehicle_Name")
    private String vehicleName;
    
    @JsonProperty("company")
    private String company;
    
    @JsonProperty("temperature")
    private String temperature;
    
    @JsonProperty("latitude")
    private String latitude;
    
    @JsonProperty("gps")
    private String gps;
    
    @JsonProperty("vehicle_No")
    private String vehicleNo;
    
    @JsonProperty("branch")
    private String branch;
    
    @JsonProperty("vehicletype")
    private String vehicleType;
    
    @JsonProperty("gpsActualTime")
    private String gpsActualTime;
    
    @JsonProperty("datetime")
    private String datetime;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("deviceModel")
    private String deviceModel;
    
    @JsonProperty("speed")
    private String speed;
    
    @JsonProperty("imeino")
    private String imeiNo;
    
    @JsonProperty("odometer")
    private String odometer;
    
    @JsonProperty("poi")
    private String poi;
    
    @JsonProperty("longitude")
    private String longitude;
    
    @JsonProperty("ign")
    private String ign;
    
    @JsonProperty("angle")
    private String angle;
    
    @JsonProperty("battery_percentage")
    private String batteryPercentage;
    
    @JsonProperty("externalVolt")
    private String externalVolt;
    
    @JsonProperty("power")
    private String power;
    
    @JsonProperty("location")
    private String location;
    
    @JsonProperty("beacon")
    private String beacon;

    // Getters and Setters
    public String getVehicleName() { return vehicleName; }
    public void setVehicleName(String vehicleName) { this.vehicleName = vehicleName; }
    
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    
    public String getTemperature() { return temperature; }
    public void setTemperature(String temperature) { this.temperature = temperature; }
    
    public String getLatitude() { return latitude; }
    public void setLatitude(String latitude) { this.latitude = latitude; }
    
    public String getGps() { return gps; }
    public void setGps(String gps) { this.gps = gps; }
    
    public String getVehicleNo() { return vehicleNo; }
    public void setVehicleNo(String vehicleNo) { this.vehicleNo = vehicleNo; }
    
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    
    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    
    public String getGpsActualTime() { return gpsActualTime; }
    public void setGpsActualTime(String gpsActualTime) { this.gpsActualTime = gpsActualTime; }
    
    public String getDatetime() { return datetime; }
    public void setDatetime(String datetime) { this.datetime = datetime; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getDeviceModel() { return deviceModel; }
    public void setDeviceModel(String deviceModel) { this.deviceModel = deviceModel; }
    
    public String getSpeed() { return speed; }
    public void setSpeed(String speed) { this.speed = speed; }
    
    public String getImeiNo() { return imeiNo; }
    public void setImeiNo(String imeiNo) { this.imeiNo = imeiNo; }
    
    public String getOdometer() { return odometer; }
    public void setOdometer(String odometer) { this.odometer = odometer; }
    
    public String getPoi() { return poi; }
    public void setPoi(String poi) { this.poi = poi; }
    
    public String getLongitude() { return longitude; }
    public void setLongitude(String longitude) { this.longitude = longitude; }
    
    public String getIgn() { return ign; }
    public void setIgn(String ign) { this.ign = ign; }
    
    public String getAngle() { return angle; }
    public void setAngle(String angle) { this.angle = angle; }
    
    public String getBatteryPercentage() { return batteryPercentage; }
    public void setBatteryPercentage(String batteryPercentage) { this.batteryPercentage = batteryPercentage; }
    
    public String getExternalVolt() { return externalVolt; }
    public void setExternalVolt(String externalVolt) { this.externalVolt = externalVolt; }
    
    public String getPower() { return power; }
    public void setPower(String power) { this.power = power; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public String getBeacon() { return beacon; }
    public void setBeacon(String beacon) { this.beacon = beacon; }
}

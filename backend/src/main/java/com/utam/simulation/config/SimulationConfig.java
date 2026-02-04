package com.utam.simulation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for data simulation generators.
 * Configurable via application.properties with prefix "simulation".
 * 
 * Example properties:
 * simulation.retention-days=90
 * simulation.batch-size=100
 * simulation.continuous-enabled=true
 * simulation.rates.vehicle-updates-per-second=5
 * simulation.tenants.VIDP.timezone=Asia/Kolkata
 */
@Configuration
@ConfigurationProperties(prefix = "simulation")
public class SimulationConfig {

    /** Data retention period in days (default 90) */
    private int retentionDays = 90;

    /** Batch size for initial data population */
    private int batchSize = 100;

    /** Enable continuous real-time simulation */
    private boolean continuousEnabled = true;

    /** Master enable switch for simulation */
    private boolean enabled = true;

    /** Generation rates configuration */
    private RatesConfig rates = new RatesConfig();

    /** Per-tenant configuration */
    private Map<String, TenantConfig> tenants = new HashMap<>();

    /** Fleet sizes per airport */
    private FleetConfig fleet = new FleetConfig();

    /** Alert configuration */
    private AlertConfig alerts = new AlertConfig();

    /** Financial calculation configuration */
    private FinancialConfig financial = new FinancialConfig();

    public SimulationConfig() {
        // Initialize default tenant configurations
        initializeDefaultTenants();
    }

    private void initializeDefaultTenants() {
        // VIDP - Delhi
        TenantConfig vidp = new TenantConfig();
        vidp.setTimezone(ZoneId.of("Asia/Kolkata"));
        vidp.setAirportCode("VIDP");
        vidp.setAirportName("Indira Gandhi International Airport");
        vidp.setCenterLatitude(28.5562);
        vidp.setCenterLongitude(77.1000);
        tenants.put("VIDP", vidp);

        // YBBN - Brisbane
        TenantConfig ybbn = new TenantConfig();
        ybbn.setTimezone(ZoneId.of("Australia/Brisbane"));
        ybbn.setAirportCode("YBBN");
        ybbn.setAirportName("Brisbane Airport");
        ybbn.setCenterLatitude(-27.3842);
        ybbn.setCenterLongitude(153.1175);
        tenants.put("YBBN", ybbn);
    }

    // Getters and Setters
    public int getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public boolean isContinuousEnabled() {
        return continuousEnabled;
    }

    public void setContinuousEnabled(boolean continuousEnabled) {
        this.continuousEnabled = continuousEnabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public RatesConfig getRates() {
        return rates;
    }

    public void setRates(RatesConfig rates) {
        this.rates = rates;
    }

    public Map<String, TenantConfig> getTenants() {
        return tenants;
    }

    public void setTenants(Map<String, TenantConfig> tenants) {
        this.tenants = tenants;
    }

    public TenantConfig getTenant(String tenantCode) {
        return tenants.get(tenantCode);
    }

    public FleetConfig getFleet() {
        return fleet;
    }

    public void setFleet(FleetConfig fleet) {
        this.fleet = fleet;
    }

    public AlertConfig getAlerts() {
        return alerts;
    }

    public void setAlerts(AlertConfig alerts) {
        this.alerts = alerts;
    }

    public FinancialConfig getFinancial() {
        return financial;
    }

    public void setFinancial(FinancialConfig financial) {
        this.financial = financial;
    }

    /**
     * Generation rates configuration.
     */
    public static class RatesConfig {
        /** Vehicle position updates per second */
        private int vehicleUpdatesPerSecond = 5;

        /** Flight position updates per second */
        private int flightUpdatesPerSecond = 2;

        /** Alerts generated per hour (during simulation) */
        private int alertsPerHour = 50;

        /** Turnaround sessions per hour peak */
        private int turnaroundsPerHourPeak = 20;

        public int getVehicleUpdatesPerSecond() {
            return vehicleUpdatesPerSecond;
        }

        public void setVehicleUpdatesPerSecond(int vehicleUpdatesPerSecond) {
            this.vehicleUpdatesPerSecond = vehicleUpdatesPerSecond;
        }

        public int getFlightUpdatesPerSecond() {
            return flightUpdatesPerSecond;
        }

        public void setFlightUpdatesPerSecond(int flightUpdatesPerSecond) {
            this.flightUpdatesPerSecond = flightUpdatesPerSecond;
        }

        public int getAlertsPerHour() {
            return alertsPerHour;
        }

        public void setAlertsPerHour(int alertsPerHour) {
            this.alertsPerHour = alertsPerHour;
        }

        public int getTurnaroundsPerHourPeak() {
            return turnaroundsPerHourPeak;
        }

        public void setTurnaroundsPerHourPeak(int turnaroundsPerHourPeak) {
            this.turnaroundsPerHourPeak = turnaroundsPerHourPeak;
        }
    }

    /**
     * Per-tenant configuration.
     */
    public static class TenantConfig {
        private ZoneId timezone;
        private String airportCode;
        private String airportName;
        private String name;
        private double centerLatitude;
        private double centerLongitude;
        private int flightsPerHour = 20;
        private int groundVehicleCount = 100;

        public ZoneId getTimezone() {
            return timezone;
        }

        public void setTimezone(ZoneId timezone) {
            this.timezone = timezone;
        }

        public void setTimezone(String timezone) {
            this.timezone = ZoneId.of(timezone);
        }

        public String getAirportCode() {
            return airportCode;
        }

        public void setAirportCode(String airportCode) {
            this.airportCode = airportCode;
        }

        public String getAirportName() {
            return airportName;
        }

        public void setAirportName(String airportName) {
            this.airportName = airportName;
        }

        public String getName() {
            return name != null ? name : airportName;
        }

        public void setName(String name) {
            this.name = name;
            if (this.airportName == null) {
                this.airportName = name;
            }
        }

        public double getCenterLatitude() {
            return centerLatitude;
        }

        public void setCenterLatitude(double centerLatitude) {
            this.centerLatitude = centerLatitude;
        }

        public double getCenterLongitude() {
            return centerLongitude;
        }

        public void setCenterLongitude(double centerLongitude) {
            this.centerLongitude = centerLongitude;
        }

        public int getFlightsPerHour() {
            return flightsPerHour;
        }

        public void setFlightsPerHour(int flightsPerHour) {
            this.flightsPerHour = flightsPerHour;
        }

        public int getGroundVehicleCount() {
            return groundVehicleCount;
        }

        public void setGroundVehicleCount(int groundVehicleCount) {
            this.groundVehicleCount = groundVehicleCount;
        }
    }

    /**
     * GSE fleet sizes configuration.
     */
    public static class FleetConfig {
        private int fuelTrucks = 20;
        private int cateringTrucks = 15;
        private int baggageTugs = 30;
        private int baggageCarts = 100;
        private int beltLoaders = 25;
        private int gpuUnits = 20;
        private int pushbackTugs = 15;
        private int passengerStairs = 10;
        private int waterTrucks = 8;
        private int lavatoryTrucks = 8;
        private int deicingTrucks = 5;
        private int airStartUnits = 5;
        private int passengerBuses = 10;
        private int cargoLoaders = 10;
        private int ambuliftVehicles = 3;

        // Standard accessor methods matching VehicleDataGenerator
        public int getFuelTrucks() { return fuelTrucks; }
        public void setFuelTrucks(int fuelTrucks) { this.fuelTrucks = fuelTrucks; }
        public int getCateringTrucks() { return cateringTrucks; }
        public void setCateringTrucks(int cateringTrucks) { this.cateringTrucks = cateringTrucks; }
        public int getBaggageTugs() { return baggageTugs; }
        public void setBaggageTugs(int baggageTugs) { this.baggageTugs = baggageTugs; }
        public int getBaggageCarts() { return baggageCarts; }
        public void setBaggageCarts(int baggageCarts) { this.baggageCarts = baggageCarts; }
        public int getBeltLoaders() { return beltLoaders; }
        public void setBeltLoaders(int beltLoaders) { this.beltLoaders = beltLoaders; }
        public int getGpuUnits() { return gpuUnits; }
        public void setGpuUnits(int gpuUnits) { this.gpuUnits = gpuUnits; }
        public int getPushbackTugs() { return pushbackTugs; }
        public void setPushbackTugs(int pushbackTugs) { this.pushbackTugs = pushbackTugs; }
        public int getPassengerStairs() { return passengerStairs; }
        public void setPassengerStairs(int passengerStairs) { this.passengerStairs = passengerStairs; }
        public int getWaterTrucks() { return waterTrucks; }
        public void setWaterTrucks(int waterTrucks) { this.waterTrucks = waterTrucks; }
        public int getLavatoryTrucks() { return lavatoryTrucks; }
        public void setLavatoryTrucks(int lavatoryTrucks) { this.lavatoryTrucks = lavatoryTrucks; }
        public int getDeicingTrucks() { return deicingTrucks; }
        public void setDeicingTrucks(int deicingTrucks) { this.deicingTrucks = deicingTrucks; }
        public int getAirStartUnits() { return airStartUnits; }
        public void setAirStartUnits(int airStartUnits) { this.airStartUnits = airStartUnits; }
        public int getPassengerBuses() { return passengerBuses; }
        public void setPassengerBuses(int passengerBuses) { this.passengerBuses = passengerBuses; }
        public int getCargoLoaders() { return cargoLoaders; }
        public void setCargoLoaders(int cargoLoaders) { this.cargoLoaders = cargoLoaders; }
        public int getAmbuliftVehicles() { return ambuliftVehicles; }
        public void setAmbuliftVehicles(int ambuliftVehicles) { this.ambuliftVehicles = ambuliftVehicles; }
        
        // Aliases for VehicleDataGenerator compatibility
        public int getGpus() { return gpuUnits; }
        public int getPushbackTractors() { return pushbackTugs; }
        public int getStairs() { return passengerStairs; }
        public int getAsus() { return airStartUnits; }
        public int getBuses() { return passengerBuses; }
        public int getAmbulifts() { return ambuliftVehicles; }
    }

    /**
     * Alert generation configuration.
     */
    public static class AlertConfig {
        /** Alert de-duplication window in minutes */
        private int deduplicationWindowMinutes = 30;

        /** Cascade recalculation interval in minutes */
        private int cascadeRecalcIntervalMinutes = 15;

        /** Vehicle delay threshold in minutes */
        private int vehicleDelayThresholdMinutes = 5;

        /** Task behind schedule threshold in minutes */
        private int taskBehindThresholdMinutes = 5;

        /** Milestone missed grace period in minutes */
        private int milestoneMissedGraceMinutes = 5;

        /** Predictive alert lead time in minutes */
        private int predictiveLeadTimeMinutes = 30;

        public int getDeduplicationWindowMinutes() { return deduplicationWindowMinutes; }
        public void setDeduplicationWindowMinutes(int deduplicationWindowMinutes) { this.deduplicationWindowMinutes = deduplicationWindowMinutes; }
        public int getCascadeRecalcIntervalMinutes() { return cascadeRecalcIntervalMinutes; }
        public void setCascadeRecalcIntervalMinutes(int cascadeRecalcIntervalMinutes) { this.cascadeRecalcIntervalMinutes = cascadeRecalcIntervalMinutes; }
        public int getVehicleDelayThresholdMinutes() { return vehicleDelayThresholdMinutes; }
        public void setVehicleDelayThresholdMinutes(int vehicleDelayThresholdMinutes) { this.vehicleDelayThresholdMinutes = vehicleDelayThresholdMinutes; }
        public int getTaskBehindThresholdMinutes() { return taskBehindThresholdMinutes; }
        public void setTaskBehindThresholdMinutes(int taskBehindThresholdMinutes) { this.taskBehindThresholdMinutes = taskBehindThresholdMinutes; }
        public int getMilestoneMissedGraceMinutes() { return milestoneMissedGraceMinutes; }
        public void setMilestoneMissedGraceMinutes(int milestoneMissedGraceMinutes) { this.milestoneMissedGraceMinutes = milestoneMissedGraceMinutes; }
        public int getPredictiveLeadTimeMinutes() { return predictiveLeadTimeMinutes; }
        public void setPredictiveLeadTimeMinutes(int predictiveLeadTimeMinutes) { this.predictiveLeadTimeMinutes = predictiveLeadTimeMinutes; }
        
        // Alias for AlertDataGenerator compatibility
        public int getDelayThresholdMinutes() { return vehicleDelayThresholdMinutes; }
    }

    /**
     * Financial calculation configuration.
     */
    public static class FinancialConfig {
        /** Default delay cost per minute (USD) */
        private double delayCostPerMinute = 125.0;

        /** Minimum delay cost per minute (USD) */
        private double minDelayCostPerMinute = 100.0;

        /** Maximum delay cost per minute (USD) */
        private double maxDelayCostPerMinute = 150.0;

        /** Slot value ranges by time of day */
        private double slotValueMin = 20000.0;
        private double slotValueMax = 80000.0;

        /** Annualized opportunity target (for demonstration) */
        private double annualOpportunityTarget = 11000000.0;

        public double getDelayCostPerMinute() { return delayCostPerMinute; }
        public void setDelayCostPerMinute(double delayCostPerMinute) { this.delayCostPerMinute = delayCostPerMinute; }
        public double getMinDelayCostPerMinute() { return minDelayCostPerMinute; }
        public void setMinDelayCostPerMinute(double minDelayCostPerMinute) { this.minDelayCostPerMinute = minDelayCostPerMinute; }
        public double getMaxDelayCostPerMinute() { return maxDelayCostPerMinute; }
        public void setMaxDelayCostPerMinute(double maxDelayCostPerMinute) { this.maxDelayCostPerMinute = maxDelayCostPerMinute; }
        public double getSlotValueMin() { return slotValueMin; }
        public void setSlotValueMin(double slotValueMin) { this.slotValueMin = slotValueMin; }
        public double getSlotValueMax() { return slotValueMax; }
        public void setSlotValueMax(double slotValueMax) { this.slotValueMax = slotValueMax; }
        public double getAnnualOpportunityTarget() { return annualOpportunityTarget; }
        public void setAnnualOpportunityTarget(double annualOpportunityTarget) { this.annualOpportunityTarget = annualOpportunityTarget; }
    }
}

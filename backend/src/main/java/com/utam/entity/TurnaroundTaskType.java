package com.utam.entity;

/**
 * Enum defining all 34 turnaround sub-process types.
 * Implements FR-052, FR-053: 30+ turnaround sub-processes.
 */
public enum TurnaroundTaskType {

    // Arrival Phase
    AIRCRAFT_ARRIVAL("Aircraft Arrival", "Arrival", 5, true),
    CHOCKS_ON("Chocks On", "Arrival", 1, true),
    GPU_CONNECT("GPU Connect", "Arrival", 3, false),
    JETBRIDGE_CONNECT("Jetbridge Connect", "Arrival", 2, false),
    DOOR_OPEN_FWD("Forward Door Open", "Arrival", 1, true),
    DOOR_OPEN_AFT("Aft Door Open", "Arrival", 1, false),

    // Deboarding Phase
    PAX_DEBOARD("Passenger Deboarding", "Deboarding", 15, true),
    CREW_CHANGE("Crew Change", "Deboarding", 10, false),
    WHEELCHAIR_ASSIST("Wheelchair Assistance", "Deboarding", 5, false),
    VIP_ESCORT("VIP Escort", "Deboarding", 5, false),

    // Unloading Phase
    BAGGAGE_UNLOAD_FWD("Forward Baggage Unload", "Unloading", 12, true),
    BAGGAGE_UNLOAD_AFT("Aft Baggage Unload", "Unloading", 12, true),
    CARGO_UNLOAD("Cargo Unload", "Unloading", 15, false),
    MAIL_UNLOAD("Mail Unload", "Unloading", 5, false),

    // Servicing Phase
    FUEL_SERVICE("Fuel Service", "Servicing", 20, true),
    CATERING_FWD("Forward Catering", "Servicing", 25, true),
    CATERING_AFT("Aft Catering", "Servicing", 20, false),
    WATER_SERVICE("Water Service", "Servicing", 10, true),
    LAVATORY_SERVICE("Lavatory Service", "Servicing", 10, true),
    CABIN_CLEANING("Cabin Cleaning", "Servicing", 25, true),
    WINDOW_CLEANING("Window Cleaning", "Servicing", 10, false),
    TOILET_SERVICING("Toilet Servicing", "Servicing", 8, false),
    DEICING("De-icing", "Servicing", 15, false),
    TECHNICAL_CHECK("Technical Check", "Servicing", 10, false),
    AIRCRAFT_REFUEL("Aircraft Refueling", "Servicing", 25, true),

    // Loading Phase  
    BAGGAGE_LOAD_FWD("Forward Baggage Load", "Loading", 12, true),
    BAGGAGE_LOAD_AFT("Aft Baggage Load", "Loading", 12, true),
    CARGO_LOAD("Cargo Load", "Loading", 15, false),
    MAIL_LOAD("Mail Load", "Loading", 5, false),

    // Boarding Phase
    PAX_BOARD("Passenger Boarding", "Boarding", 20, true),
    WHEELCHAIR_BOARD("Wheelchair Boarding", "Boarding", 5, false),
    VIP_BOARD("VIP Boarding", "Boarding", 5, false),

    // Departure Phase
    DOOR_CLOSE("Door Close", "Departure", 2, true),
    JETBRIDGE_DISCONNECT("Jetbridge Disconnect", "Departure", 2, false),
    GPU_DISCONNECT("GPU Disconnect", "Departure", 2, false),
    PUSHBACK("Pushback", "Departure", 5, true),
    CHOCKS_OFF("Chocks Off", "Departure", 1, true),
    AIRCRAFT_DEPARTURE("Aircraft Departure", "Departure", 5, true);

    private final String description;
    private final String phase;
    private final int defaultDurationMinutes;
    private final boolean mandatory;

    TurnaroundTaskType(String description, String phase, int defaultDurationMinutes, boolean mandatory) {
        this.description = description;
        this.phase = phase;
        this.defaultDurationMinutes = defaultDurationMinutes;
        this.mandatory = mandatory;
    }

    public String getDescription() {
        return description;
    }

    public String getPhase() {
        return phase;
    }

    public int getDefaultDurationMinutes() {
        return defaultDurationMinutes;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    /**
     * Get vehicle type required for this task.
     * Returns null if no vehicle needed (e.g., door operations).
     */
    public String getRequiredVehicleType() {
        return switch (this) {
            case BAGGAGE_UNLOAD_FWD, BAGGAGE_UNLOAD_AFT, BAGGAGE_LOAD_FWD, BAGGAGE_LOAD_AFT -> "BAGGAGE_TUG";
            case CARGO_UNLOAD, CARGO_LOAD, MAIL_UNLOAD, MAIL_LOAD -> "CARGO";
            case FUEL_SERVICE, AIRCRAFT_REFUEL -> "FUEL";
            case CATERING_FWD, CATERING_AFT -> "CATERING";
            case WATER_SERVICE -> "WATER";
            case LAVATORY_SERVICE, TOILET_SERVICING -> "LAVATORY";
            case GPU_CONNECT, GPU_DISCONNECT -> "GPU";
            case PUSHBACK -> "PUSHBACK";
            case PAX_DEBOARD, PAX_BOARD -> "STAIRS";
            case DEICING -> "DEICING";
            case WHEELCHAIR_ASSIST, WHEELCHAIR_BOARD -> "AMBULIFT";
            case JETBRIDGE_CONNECT, JETBRIDGE_DISCONNECT -> null; // Fixed infrastructure
            case CABIN_CLEANING, WINDOW_CLEANING -> null; // Manual labor
            default -> null;
        };
    }

    /**
     * Get tasks that must complete before this task can start.
     */
    public TurnaroundTaskType[] getDependencies() {
        return switch (this) {
            case DOOR_OPEN_FWD, DOOR_OPEN_AFT -> new TurnaroundTaskType[]{CHOCKS_ON, GPU_CONNECT};
            case PAX_DEBOARD -> new TurnaroundTaskType[]{DOOR_OPEN_FWD};
            case BAGGAGE_UNLOAD_FWD, BAGGAGE_UNLOAD_AFT -> new TurnaroundTaskType[]{DOOR_OPEN_AFT};
            case FUEL_SERVICE -> new TurnaroundTaskType[]{PAX_DEBOARD};
            case CATERING_FWD -> new TurnaroundTaskType[]{PAX_DEBOARD};
            case BAGGAGE_LOAD_FWD, BAGGAGE_LOAD_AFT -> new TurnaroundTaskType[]{BAGGAGE_UNLOAD_FWD};
            case PAX_BOARD -> new TurnaroundTaskType[]{CABIN_CLEANING, CATERING_FWD};
            case DOOR_CLOSE -> new TurnaroundTaskType[]{PAX_BOARD, BAGGAGE_LOAD_AFT};
            case PUSHBACK -> new TurnaroundTaskType[]{DOOR_CLOSE, GPU_DISCONNECT, JETBRIDGE_DISCONNECT};
            case CHOCKS_OFF -> new TurnaroundTaskType[]{PUSHBACK};
            default -> new TurnaroundTaskType[0];
        };
    }

    /**
     * Get all mandatory tasks for a standard narrow-body turnaround.
     */
    public static TurnaroundTaskType[] getMandatoryTasks() {
        return java.util.Arrays.stream(values())
                .filter(TurnaroundTaskType::isMandatory)
                .toArray(TurnaroundTaskType[]::new);
    }

    /**
     * Get all tasks for a specific phase.
     */
    public static TurnaroundTaskType[] getTasksForPhase(String phase) {
        return java.util.Arrays.stream(values())
                .filter(t -> t.getPhase().equals(phase))
                .toArray(TurnaroundTaskType[]::new);
    }
}

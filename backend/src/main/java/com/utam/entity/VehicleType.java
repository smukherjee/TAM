package com.utam.entity;

import jakarta.persistence.*;

/**
 * Defines GSE (Ground Support Equipment) vehicle categories with operational parameters.
 * 
 * The 15 GSE types per FR-056:
 * - FUEL: Fuel Trucks
 * - CATERING: Catering High-Lift Trucks
 * - BAGGAGE_TUG: Baggage Tugs
 * - BAGGAGE_CART: Baggage Carts
 * - BELT_LOADER: Belt Loaders
 * - GPU: Ground Power Units
 * - PUSHBACK: Pushback Tugs
 * - STAIRS: Passenger Stairs
 * - WATER: Water Service Trucks
 * - LAVATORY: Lavatory Service Trucks
 * - DEICING: De-icing Trucks
 * - ASU: Air Start Units
 * - BUS: Passenger Buses
 * - CARGO: Cargo Loaders
 * - AMBULIFT: Ambulift Vehicles
 */
@Entity
@Table(name = "vehicle_types", indexes = {
        @Index(name = "idx_vehicle_type_code", columnList = "code"),
        @Index(name = "idx_vehicle_types_tenant", columnList = "tenant_code")
})
public class VehicleType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_code", nullable = false, length = 10)
    private String tenantCode;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 50)
    private String category;

    @Column(name = "min_speed")
    private Integer minSpeed = 5;  // km/h

    @Column(name = "max_speed")
    private Integer maxSpeed = 40;  // km/h

    @Column(name = "default_quantity")
    private Integer defaultQuantity = 10;

    @Column(name = "default_depot_type", length = 30)
    private String defaultDepotType;

    @Column(name = "icon_name", length = 100)
    private String iconName;

    @Column(name = "icon_color", length = 20)
    private String iconColor;

    @Column(name = "is_motorized")
    private Boolean isMotorized = true;

    @Column(name = "active")
    private Boolean active = true;

    public VehicleType() {
    }

    public VehicleType(String tenantCode, String code, String name, String description, Integer minSpeed, Integer maxSpeed,
                       Integer defaultQuantity, String defaultDepotType) {
        this.tenantCode = tenantCode;
        this.code = code;
        this.name = name;
        this.description = description;
        this.minSpeed = minSpeed;
        this.maxSpeed = maxSpeed;
        this.defaultQuantity = defaultQuantity;
        this.defaultDepotType = defaultDepotType;
        this.isMotorized = true;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String tenantCode) { this.tenantCode = tenantCode; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getMinSpeed() { return minSpeed; }
    public void setMinSpeed(Integer minSpeed) { this.minSpeed = minSpeed; }

    public Integer getMaxSpeed() { return maxSpeed; }
    public void setMaxSpeed(Integer maxSpeed) { this.maxSpeed = maxSpeed; }

    public Integer getDefaultQuantity() { return defaultQuantity; }
    public void setDefaultQuantity(Integer defaultQuantity) { this.defaultQuantity = defaultQuantity; }

    public String getDefaultDepotType() { return defaultDepotType; }
    public void setDefaultDepotType(String defaultDepotType) { this.defaultDepotType = defaultDepotType; }

    public String getIconName() { return iconName; }
    public void setIconName(String iconName) { this.iconName = iconName; }

    public String getIconColor() { return iconColor; }
    public void setIconColor(String iconColor) { this.iconColor = iconColor; }

    public Boolean getIsMotorized() { return isMotorized; }
    public void setIsMotorized(Boolean isMotorized) { this.isMotorized = isMotorized; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    @Override
    public String toString() {
        return "VehicleType{" +
                "tenantCode='" + tenantCode + '\'' +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", defaultQuantity=" + defaultQuantity +
                '}';
    }

    /**
     * Predefined GSE type codes.
     */
    public static class Codes {
        public static final String FUEL = "FUEL";
        public static final String CATERING = "CATERING";
        public static final String BAGGAGE_TUG = "BAGGAGE_TUG";
        public static final String BAGGAGE_CART = "BAGGAGE_CART";
        public static final String BELT_LOADER = "BELT_LOADER";
        public static final String GPU = "GPU";
        public static final String PUSHBACK = "PUSHBACK";
        public static final String STAIRS = "STAIRS";
        public static final String WATER = "WATER";
        public static final String LAVATORY = "LAVATORY";
        public static final String DEICING = "DEICING";
        public static final String ASU = "ASU";
        public static final String BUS = "BUS";
        public static final String CARGO = "CARGO";
        public static final String AMBULIFT = "AMBULIFT";
    }
}

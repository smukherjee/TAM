package com.utam.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for Zone operations.
 * Implements FR-040 to FR-048: Admin zone management.
 */
public class ZoneDTO {

    private Long id;
    private String tenantCode;
    private String name;
    private String code;
    private String type;
    private String description;
    
    // GeoJSON polygon coordinates: [[lng, lat], [lng, lat], ...]
    private List<List<Double>> coordinates;
    
    // Zone properties
    private String color;
    private Double opacity;
    private Boolean restricted;
    private Boolean active;
    
    // Access control
    private List<String> allowedVehicleTypes;
    private List<String> allowedRoles;
    private String accessSchedule; // JSON schedule
    
    // Alert configuration
    private Boolean alertOnEntry;
    private Boolean alertOnExit;
    private Integer dwellTimeAlertMinutes;
    
    // Metadata
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    // Zone type constants
    public static final String TYPE_RESTRICTED = "RESTRICTED";
    public static final String TYPE_OPERATIONAL = "OPERATIONAL";
    public static final String TYPE_PARKING = "PARKING";
    public static final String TYPE_TAXIWAY = "TAXIWAY";
    public static final String TYPE_RUNWAY = "RUNWAY";
    public static final String TYPE_TERMINAL = "TERMINAL";
    public static final String TYPE_CARGO = "CARGO";
    public static final String TYPE_MAINTENANCE = "MAINTENANCE";
    public static final String TYPE_SECURITY = "SECURITY";
    public static final String TYPE_CUSTOM = "CUSTOM";

    // Default constructor
    public ZoneDTO() {}

    // Full constructor
    public ZoneDTO(Long id, String tenantCode, String name, String code, String type,
                   List<List<Double>> coordinates) {
        this.id = id;
        this.tenantCode = tenantCode;
        this.name = name;
        this.code = code;
        this.type = type;
        this.coordinates = coordinates;
    }

    // Builder pattern for fluent creation
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ZoneDTO dto = new ZoneDTO();

        public Builder id(Long id) {
            dto.id = id;
            return this;
        }

        public Builder tenantCode(String tenantCode) {
            dto.tenantCode = tenantCode;
            return this;
        }

        public Builder name(String name) {
            dto.name = name;
            return this;
        }

        public Builder code(String code) {
            dto.code = code;
            return this;
        }

        public Builder type(String type) {
            dto.type = type;
            return this;
        }

        public Builder description(String description) {
            dto.description = description;
            return this;
        }

        public Builder coordinates(List<List<Double>> coordinates) {
            dto.coordinates = coordinates;
            return this;
        }

        public Builder color(String color) {
            dto.color = color;
            return this;
        }

        public Builder opacity(Double opacity) {
            dto.opacity = opacity;
            return this;
        }

        public Builder restricted(Boolean restricted) {
            dto.restricted = restricted;
            return this;
        }

        public Builder active(Boolean active) {
            dto.active = active;
            return this;
        }

        public Builder allowedVehicleTypes(List<String> types) {
            dto.allowedVehicleTypes = types;
            return this;
        }

        public Builder allowedRoles(List<String> roles) {
            dto.allowedRoles = roles;
            return this;
        }

        public Builder alertOnEntry(Boolean alertOnEntry) {
            dto.alertOnEntry = alertOnEntry;
            return this;
        }

        public Builder alertOnExit(Boolean alertOnExit) {
            dto.alertOnExit = alertOnExit;
            return this;
        }

        public Builder dwellTimeAlertMinutes(Integer minutes) {
            dto.dwellTimeAlertMinutes = minutes;
            return this;
        }

        public ZoneDTO build() {
            return dto;
        }
    }

    // Convert to GeoJSON Feature
    public String toGeoJson() {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"type\":\"Feature\",");
        json.append("\"properties\":{");
        json.append("\"id\":").append(id).append(",");
        json.append("\"name\":\"").append(escapeJson(name)).append("\",");
        json.append("\"code\":\"").append(escapeJson(code)).append("\",");
        json.append("\"type\":\"").append(escapeJson(type)).append("\",");
        if (description != null) {
            json.append("\"description\":\"").append(escapeJson(description)).append("\",");
        }
        if (color != null) {
            json.append("\"color\":\"").append(escapeJson(color)).append("\",");
        }
        if (opacity != null) {
            json.append("\"opacity\":").append(opacity).append(",");
        }
        json.append("\"restricted\":").append(restricted != null && restricted).append(",");
        json.append("\"active\":").append(active != null && active);
        json.append("},");
        json.append("\"geometry\":{");
        json.append("\"type\":\"Polygon\",");
        json.append("\"coordinates\":[");
        json.append(coordinatesToJson());
        json.append("]");
        json.append("}");
        json.append("}");
        return json.toString();
    }

    // Convert coordinates to GeoJSON format
    private String coordinatesToJson() {
        if (coordinates == null || coordinates.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < coordinates.size(); i++) {
            List<Double> point = coordinates.get(i);
            if (point.size() >= 2) {
                sb.append("[").append(point.get(0)).append(",").append(point.get(1)).append("]");
                if (i < coordinates.size() - 1) {
                    sb.append(",");
                }
            }
        }
        sb.append("]");
        return sb.toString();
    }

    // Escape JSON string
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // Calculate approximate area in square meters (Shoelace formula)
    public double calculateAreaSqM() {
        if (coordinates == null || coordinates.size() < 3) {
            return 0;
        }

        double area = 0;
        int n = coordinates.size();

        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            List<Double> pi = coordinates.get(i);
            List<Double> pj = coordinates.get(j);
            
            if (pi.size() >= 2 && pj.size() >= 2) {
                // Convert lng/lat to approximate meters
                double lat1 = Math.toRadians(pi.get(1));
                double lat2 = Math.toRadians(pj.get(1));
                double lng1 = pi.get(0);
                double lng2 = pj.get(0);
                
                // Approximate conversion at this latitude
                double metersPerDegreeLat = 111320;
                double metersPerDegreeLng = 111320 * Math.cos((lat1 + lat2) / 2);
                
                double x1 = lng1 * metersPerDegreeLng;
                double y1 = Math.toDegrees(lat1) * metersPerDegreeLat;
                double x2 = lng2 * metersPerDegreeLng;
                double y2 = Math.toDegrees(lat2) * metersPerDegreeLat;
                
                area += x1 * y2 - x2 * y1;
            }
        }

        return Math.abs(area) / 2;
    }

    // Check if point is inside zone (ray casting algorithm)
    public boolean containsPoint(double lng, double lat) {
        if (coordinates == null || coordinates.size() < 3) {
            return false;
        }

        boolean inside = false;
        int n = coordinates.size();

        for (int i = 0, j = n - 1; i < n; j = i++) {
            List<Double> pi = coordinates.get(i);
            List<Double> pj = coordinates.get(j);

            if (pi.size() < 2 || pj.size() < 2) continue;

            double xi = pi.get(0), yi = pi.get(1);
            double xj = pj.get(0), yj = pj.get(1);

            if (((yi > lat) != (yj > lat)) &&
                (lng < (xj - xi) * (lat - yi) / (yj - yi) + xi)) {
                inside = !inside;
            }
        }

        return inside;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<List<Double>> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<List<Double>> coordinates) {
        this.coordinates = coordinates;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Double getOpacity() {
        return opacity;
    }

    public void setOpacity(Double opacity) {
        this.opacity = opacity;
    }

    public Boolean getRestricted() {
        return restricted;
    }

    public void setRestricted(Boolean restricted) {
        this.restricted = restricted;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public List<String> getAllowedVehicleTypes() {
        return allowedVehicleTypes;
    }

    public void setAllowedVehicleTypes(List<String> allowedVehicleTypes) {
        this.allowedVehicleTypes = allowedVehicleTypes;
    }

    public List<String> getAllowedRoles() {
        return allowedRoles;
    }

    public void setAllowedRoles(List<String> allowedRoles) {
        this.allowedRoles = allowedRoles;
    }

    public String getAccessSchedule() {
        return accessSchedule;
    }

    public void setAccessSchedule(String accessSchedule) {
        this.accessSchedule = accessSchedule;
    }

    public Boolean getAlertOnEntry() {
        return alertOnEntry;
    }

    public void setAlertOnEntry(Boolean alertOnEntry) {
        this.alertOnEntry = alertOnEntry;
    }

    public Boolean getAlertOnExit() {
        return alertOnExit;
    }

    public void setAlertOnExit(Boolean alertOnExit) {
        this.alertOnExit = alertOnExit;
    }

    public Integer getDwellTimeAlertMinutes() {
        return dwellTimeAlertMinutes;
    }

    public void setDwellTimeAlertMinutes(Integer dwellTimeAlertMinutes) {
        this.dwellTimeAlertMinutes = dwellTimeAlertMinutes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}

package com.utam.asset.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for hotspot detail modal/popup.
 * Provides drill-down information when user clicks a heatmap cell.
 * <p>
 * Feature: 005-asset-tracking-security (US6)
 * Task: T032
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HotspotDetailDTO {

    /**
     * Hotspot location (grid cell center)
     */
    private Location location;

    /**
     * Heatmap mode (activity, violations, dwell)
     */
    private String mode;

    /**
     * Total activity count in this hotspot
     */
    private Long activityCount;

    /**
     * Number of unique assets contributing to this hotspot
     */
    private Integer uniqueAssets;

    /**
     * List of asset identifiers contributing to this hotspot
     */
    private List<String> assets;

    /**
     * Violation details (if mode = violations)
     */
    private ViolationDetails violations;

    /**
     * Time distribution data for charts (hourly breakdown)
     * Map of hour (0-23) → count
     */
    private Map<Integer, Long> timeDistribution;

    /**
     * Date range for this hotspot data
     */
    private DateRange dateRange;

    /**
     * Location sub-class
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Location {
        private Double latitude;
        private Double longitude;
        private Double gridSize; // in degrees
    }

    /**
     * Violation details sub-class
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ViolationDetails {
        private Long totalViolations;
        private Long criticalCount;
        private Long highCount;
        private Long mediumCount;
        private Long lowCount;
        private List<String> mostCommonZoneTypes;
        private List<String> violatingAssets;
    }

    /**
     * Date range sub-class
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateRange {
        private String startDate;
        private String endDate;
    }
}

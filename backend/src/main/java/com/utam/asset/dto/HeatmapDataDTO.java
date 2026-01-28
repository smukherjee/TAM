package com.utam.asset.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for heatmap grid cell data.
 * Used for activity density, violation density, and dwell time heatmaps.
 * <p>
 * Feature: 005-asset-tracking-security (US6 - Heatmap Visualization)
 * Task: T031
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HeatmapDataDTO {

    /**
     * Grid cell center latitude
     */
    private Double latitude;

    /**
     * Grid cell center longitude
     */
    private Double longitude;

    /**
     * Normalized intensity (0-1 scale)
     * Calculated using percentile-based normalization
     */
    private Double intensity;

    /**
     * Metadata for the grid cell
     */
    private HeatmapMetadata metadata;

    /**
     * Metadata sub-class for additional statistics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class HeatmapMetadata {

        /**
         * Total activity count in this cell
         * (movements for activity, violations for violation heatmap)
         */
        private Long activityCount;

        /**
         * Number of unique assets in this cell
         */
        private Integer uniqueAssets;

        /**
         * Average speed in km/h (activity heatmap only)
         */
        private Double avgSpeed;

        /**
         * Maximum speed in km/h (activity heatmap only)
         */
        private Double maxSpeed;

        /**
         * Number of critical violations (violation heatmap only)
         */
        private Long criticalCount;

        /**
         * Number of high severity violations (violation heatmap only)
         */
        private Long highCount;

        /**
         * Number of medium severity violations (violation heatmap only)
         */
        private Long mediumCount;

        /**
         * Number of low severity violations (violation heatmap only)
         */
        private Long lowCount;

        /**
         * First activity timestamp in this cell
         */
        private String firstActivity;

        /**
         * Last activity timestamp in this cell
         */
        private String lastActivity;
    }
}

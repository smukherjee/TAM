package com.utam.asset.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for heatmap statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeatmapStatistics {
    private Integer totalDataPoints;
    private Integer uniqueAssets;
    private Double maxIntensity;
    private Double avgIntensity;
    private String dateRange;
}

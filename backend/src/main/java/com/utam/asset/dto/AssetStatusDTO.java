package com.utam.asset.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for asset status information linked to a vehicle.
 * Used to display consistent status in map popup.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetStatusDTO {
    private String status;
    private String assetId;
    private String name;
    private String category;
}

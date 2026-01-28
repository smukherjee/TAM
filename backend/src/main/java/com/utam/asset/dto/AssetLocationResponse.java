package com.utam.asset.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response wrapper for asset location API endpoints.
 * Includes pagination metadata and result count.
 * <p>
 * Feature: 005-asset-tracking-security (US5)
 * Task: T029
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssetLocationResponse {

    /**
     * List of live asset locations
     */
    private List<AssetLocationDTO> assets;

    /**
     * Total count of assets (before pagination)
     */
    private Long totalCount;

    /**
     * Number of assets returned in this response
     */
    private Integer count;

    /**
     * Current page number (1-indexed)
     */
    private Integer page;

    /**
     * Page size
     */
    private Integer pageSize;

    /**
     * Total number of pages
     */
    private Integer totalPages;

    /**
     * Timestamp when data was retrieved
     */
    private String timestamp;
}

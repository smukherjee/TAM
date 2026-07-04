package com.utam.asset.controller;

import com.utam.common.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Lists distinct asset categories present in a tenant, for the Category filter
 * on the Universal Asset Map. Kept in sync with the DB instead of a hardcoded list.
 */
@RestController
@RequestMapping("/api/asset-categories")
public class AssetCategoryController {

    private final JdbcTemplate jdbcTemplate;

    public AssetCategoryController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ApiResponse<List<String>> getAssetCategories(@RequestParam String tenantCode) {
        List<String> categories = jdbcTemplate.queryForList(
                "SELECT DISTINCT category FROM assets WHERE tenant_code = ? AND category IS NOT NULL ORDER BY category",
                String.class, tenantCode);
        return ApiResponse.success(categories);
    }
}

package com.utam.asset.controller;

import com.utam.common.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Lists ground handlers with assets in a tenant, for the GH filter dropdown
 * on the Universal Asset Map (admin/non-GH roles pick from this list).
 */
@RestController
@RequestMapping("/api/ground-handlers")
public class GroundHandlerController {

    private final JdbcTemplate jdbcTemplate;

    public GroundHandlerController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ApiResponse<List<String>> getGroundHandlers(@RequestParam String tenantCode) {
        List<String> handlers = new java.util.ArrayList<>(jdbcTemplate.queryForList(
                "SELECT DISTINCT company FROM assets WHERE tenant_code = ? AND company IS NOT NULL ORDER BY company",
                String.class, tenantCode));
        Integer untaggedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM assets WHERE tenant_code = ? AND company IS NULL",
                Integer.class, tenantCode);
        if (untaggedCount != null && untaggedCount > 0) {
            handlers.add("Untagged");
        }
        return ApiResponse.success(handlers);
    }
}

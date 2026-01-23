package com.utam.asset.controller;

import com.utam.asset.domain.Asset;
import com.utam.asset.service.AssetService;
import com.utam.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
@CrossOrigin(origins = "*")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @GetMapping
    public ApiResponse<List<Asset>> getAssets(
            @RequestHeader(value = "X-User-ICAO", required = false) String icaoCode) {
        String tenantCode = icaoCode != null ? icaoCode : "VIDP";
        List<Asset> assets = assetService.getAssetsByTenant(tenantCode);
        return ApiResponse.success(assets);
    }

    @PostMapping
    public ApiResponse<Asset> createAsset(
            @RequestBody Asset asset,
            @RequestHeader(value = "X-User-ICAO", required = false) String icaoCode) {
        String tenantCode = icaoCode != null ? icaoCode : "VIDP";
        asset.setTenantCode(tenantCode);
        Asset created = assetService.createAsset(asset);
        return ApiResponse.success(created);
    }

    @PutMapping("/{id}")
    public ApiResponse<Asset> updateAsset(
            @PathVariable String id,
            @RequestBody Asset asset,
            @RequestHeader(value = "X-User-ICAO", required = false) String icaoCode) {
        String tenantCode = icaoCode != null ? icaoCode : "VIDP";
        asset.setTenantCode(tenantCode);
        Asset updated = assetService.updateAsset(asset);
        return ApiResponse.success(updated);
    }
}

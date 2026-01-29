package com.utam.asset.service;

import com.utam.asset.domain.Asset;
import com.utam.asset.repository.AssetRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AssetService {

    private final AssetRepository assetRepository;

    public AssetService(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    public Asset getAssetById(String id) {
        try {
            UUID uuid = UUID.fromString(id);
            return assetRepository.findById(uuid).orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public List<Asset> getAssetsByTenant(String tenantCode) {
        return assetRepository.findByTenantCode(tenantCode);
    }

    public List<Asset> getAssetsByTenantAndStatus(String tenantCode, String status) {
        return assetRepository.findByTenantCodeAndStatus(tenantCode, status);
    }

    public List<Asset> searchAssets(String tenantCode, String query, int limit) {
        if (query == null || query.trim().isEmpty()) {
            return assetRepository.findByTenantCode(tenantCode)
                    .stream()
                    .limit(limit)
                    .toList();
        }
        return assetRepository.searchByTenantCodeAndQuery(tenantCode, query.trim(), PageRequest.of(0, limit));
    }

    public Asset createAsset(Asset asset) {
        return assetRepository.save(asset);
    }

    public Asset updateAsset(Asset asset) {
        return assetRepository.save(asset);
    }
}

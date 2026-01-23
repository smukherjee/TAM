package com.utam.asset.service;

import com.utam.asset.domain.Asset;
import com.utam.asset.repository.AssetRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AssetService {

    private final AssetRepository assetRepository;

    public AssetService(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    public List<Asset> getAssetsByTenant(String tenantCode) {
        return assetRepository.findByTenantCode(tenantCode);
    }

    public List<Asset> getAssetsByTenantAndStatus(String tenantCode, String status) {
        return assetRepository.findByTenantCodeAndStatus(tenantCode, status);
    }

    public Asset createAsset(Asset asset) {
        return assetRepository.save(asset);
    }

    public Asset updateAsset(Asset asset) {
        return assetRepository.save(asset);
    }
}

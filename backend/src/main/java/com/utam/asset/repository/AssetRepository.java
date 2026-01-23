package com.utam.asset.repository;

import com.utam.asset.domain.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AssetRepository extends JpaRepository<Asset, UUID> {
    List<Asset> findByTenantCode(String tenantCode);
    List<Asset> findByTenantCodeAndStatus(String tenantCode, String status);
    List<Asset> findByTenantCodeAndCategory(String tenantCode, String category);
}

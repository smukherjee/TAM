package com.utam.asset.repository;

import com.utam.asset.domain.Asset;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AssetRepository extends JpaRepository<Asset, UUID> {
    List<Asset> findByTenantCode(String tenantCode);
    List<Asset> findByTenantCodeAndStatus(String tenantCode, String status);
    List<Asset> findByTenantCodeAndCategory(String tenantCode, String category);

    @Query("SELECT a FROM Asset a WHERE a.tenantCode = :tenantCode " +
           "AND (LOWER(a.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(a.assetId) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(COALESCE(a.qrId, '')) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(a.category) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Asset> searchByTenantCodeAndQuery(
            @Param("tenantCode") String tenantCode, 
            @Param("query") String query, 
            Pageable pageable);
}

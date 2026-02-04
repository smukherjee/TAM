package com.utam.repository;

import com.utam.entity.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleTypeRepository extends JpaRepository<VehicleType, Long> {

    Optional<VehicleType> findByCode(String code);

    boolean existsByCode(String code);
}

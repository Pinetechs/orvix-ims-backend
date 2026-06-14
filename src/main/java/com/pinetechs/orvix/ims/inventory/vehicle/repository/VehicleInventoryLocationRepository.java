package com.pinetechs.orvix.ims.inventory.vehicle.repository;

import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleInventoryLocationRepository
        extends JpaRepository<VehicleInventoryLocation, Long> {

    Optional<VehicleInventoryLocation> findByInventoryTaskIdAndStoreNo(
            Long taskId,
            String storeNo
    );

    List<VehicleInventoryLocation> findByInventoryTaskId(Long taskId);

    boolean existsByInventoryTaskIdAndStoreNo(
            Long taskId,
            String storeNo
    );

    long countByInventoryTaskId(Long taskId);
}
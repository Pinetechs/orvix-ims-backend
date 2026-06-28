package com.pinetechs.orvix.ims.inventory.vehicle.repository;

import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
           delete from VehicleInventoryLocation l
           where l.inventoryTask.id = :taskId
           """)
    int deleteByTaskId(@Param("taskId") Long taskId);

}
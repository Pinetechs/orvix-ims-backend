package com.pinetechs.orvix.ims.inventory.vehicle.repository;

import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryItem;
import com.pinetechs.orvix.ims.inventory.vehicle.enums.VehicleInventoryItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleInventoryItemRepository
        extends JpaRepository<VehicleInventoryItem, Long> {

    boolean existsByInventoryTaskIdAndVinNo(Long taskId, String vinNo);

    Optional<VehicleInventoryItem> findByInventoryTaskIdAndVinNo(
            Long taskId,
            String vinNo
    );




    List<VehicleInventoryItem> findByInventoryTaskId(Long taskId);

    List<VehicleInventoryItem> findByInventoryTaskIdAndStoreNo(
            Long taskId,
            String storeNo
    );

    List<VehicleInventoryItem> findByInventoryTaskIdAndStatus(
            Long taskId,
            VehicleInventoryItemStatus status
    );

    long countByInventoryTaskId(Long taskId);

    long countByInventoryTaskIdAndStatus(
            Long taskId,
            VehicleInventoryItemStatus status
    );

    long countByInventoryTaskIdAndStoreNo(
            Long taskId,
            String storeNo
    );

    long countByInventoryTaskIdAndStoreNoAndStatus(
            Long taskId,
            String storeNo,
            VehicleInventoryItemStatus status
    );
}
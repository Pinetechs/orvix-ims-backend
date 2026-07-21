package com.pinetechs.orvix.ims.inventory.vehicle.repository;

import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;

public interface VehicleInventoryScanRepository extends JpaRepository<VehicleInventoryScan, Long> {

    Optional<VehicleInventoryScan> findByInventoryTaskIdAndClientScanId(Long taskId, String clientScanId);

    Optional<VehicleInventoryScan> findByIdAndInventoryTaskId(Long scanId, Long taskId);

    long countByInventoryTaskId(Long taskId);

    @Query("select distinct scan.scanImage.id from VehicleInventoryScan scan where scan.inventoryTask.id = :taskId and scan.scanImage is not null")
    List<Long> findScanImageIdsByTaskId(@Param("taskId") Long taskId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from VehicleInventoryScan scan where scan.inventoryTask.id = :taskId")
    int deleteByTaskId(@Param("taskId") Long taskId);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update VehicleInventoryScan v set v.correctsScan =null where v.inventoryTask.id =:taskId")
    void removeItemsByTaskId(@Param("taskId") Long taskId);






}

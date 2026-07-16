package com.pinetechs.orvix.ims.inventory.asset.repository;

import com.pinetechs.orvix.ims.inventory.asset.entity.AssetInventoryScan;
import com.pinetechs.orvix.ims.inventory.common.dto.HierarchyScanProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssetInventoryScanRepository extends JpaRepository<AssetInventoryScan, Long> {

    long countByInventoryTaskId(Long taskId);

    @Query("select distinct scan.scanImage.id from AssetInventoryScan scan where scan.inventoryTask.id = :taskId and scan.scanImage is not null")
    List<Long> findScanImageIdsByTaskId(@Param("taskId") Long taskId);

    Optional<AssetInventoryScan> findByInventoryTaskIdAndClientScanId(Long taskId, String clientScanId);

    Optional<AssetInventoryScan> findByIdAndInventoryTaskId(Long scanId, Long taskId);

    @Query("""
           select new com.pinetechs.orvix.ims.inventory.common.dto.HierarchyScanProgress(
               scan.actualFloor.id, count(scan.id), max(scan.scannedAt)
           )
           from AssetInventoryScan scan
           where scan.inventoryTask.id = :taskId
             and scan.actualLocation.id = :locationId
           group by scan.actualFloor.id
           """)
    List<HierarchyScanProgress> summarizeFloorProgress(
            @Param("taskId") Long taskId,
            @Param("locationId") Long locationId
    );

    @Query("""
           select new com.pinetechs.orvix.ims.inventory.common.dto.HierarchyScanProgress(
               scan.actualPlace.id, count(scan.id), max(scan.scannedAt)
           )
           from AssetInventoryScan scan
           where scan.inventoryTask.id = :taskId
             and scan.actualFloor.id = :floorId
           group by scan.actualPlace.id
           """)
    List<HierarchyScanProgress> summarizePlaceProgress(
            @Param("taskId") Long taskId,
            @Param("floorId") Long floorId
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
           delete from AssetInventoryScan scan
           where scan.inventoryTask.id = :taskId
           """)
    int deleteByTaskId(@Param("taskId") Long taskId);
}

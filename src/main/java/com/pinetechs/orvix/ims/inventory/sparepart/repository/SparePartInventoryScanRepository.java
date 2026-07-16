package com.pinetechs.orvix.ims.inventory.sparepart.repository;

import com.pinetechs.orvix.ims.inventory.sparepart.entity.SparePartInventoryItem;
import com.pinetechs.orvix.ims.inventory.sparepart.entity.SparePartInventoryScan;
import com.pinetechs.orvix.ims.inventory.common.dto.HierarchyScanProgress;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SparePartInventoryScanRepository extends JpaRepository<SparePartInventoryScan, Long> {

    long countByInventoryTaskId(Long taskId);

    @Query("select distinct scan.scanImage.id from SparePartInventoryScan scan where scan.inventoryTask.id = :taskId and scan.scanImage is not null")
    List<Long> findScanImageIdsByTaskId(@Param("taskId") Long taskId);

    Optional<SparePartInventoryScan> findByInventoryTaskIdAndClientScanId(Long taskId, String clientScanId);

    Optional<SparePartInventoryScan> findByIdAndInventoryTaskId(Long scanId, Long taskId);

    @Query("""
           select new com.pinetechs.orvix.ims.inventory.common.dto.HierarchyScanProgress(
               scan.actualLocation.id,
               count(scan.id),
               max(scan.scannedAt),
               sum(case when scan.reviewRequired = true and scan.reviewResolvedAt is null then 1L else 0L end)
           )
           from SparePartInventoryScan scan
           where scan.inventoryTask.id = :taskId
             and scan.actualBranch.id = :branchId
           group by scan.actualLocation.id
           """)
    List<HierarchyScanProgress> summarizeLocationProgress(
            @Param("taskId") Long taskId,
            @Param("branchId") Long branchId
    );

    @Query("""
           select count(scan.id)
           from SparePartInventoryScan scan
           where scan.inventoryTask.id = :taskId
             and scan.actualLocation.id = :locationId
           """)
    long countLocationScans(
            @Param("taskId") Long taskId,
            @Param("locationId") Long locationId
    );

    @Query("""
           select count(scan.id)
           from SparePartInventoryScan scan
           where scan.inventoryTask.id = :taskId
             and scan.actualLocation.id = :locationId
             and scan.reviewRequired = true
             and scan.reviewResolvedAt is null
           """)
    long countOpenLocationReviews(
            @Param("taskId") Long taskId,
            @Param("locationId") Long locationId
    );

    @Modifying(flushAutomatically = true)
    @Query("""
           update SparePartInventoryScan scan
              set scan.reviewResolvedAt = :resolvedAt,
                  scan.reviewResolvedBy = :resolvedBy
            where scan.inventoryTask.id = :taskId
              and scan.item.id = :itemId
              and scan.reviewRequired = true
              and scan.reviewResolvedAt is null
           """)
    int resolveOpenItemReviews(
            @Param("taskId") Long taskId,
            @Param("itemId") Long itemId,
            @Param("resolvedAt") LocalDateTime resolvedAt,
            @Param("resolvedBy") User resolvedBy
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
           delete from SparePartInventoryScan scan
           where scan.inventoryTask.id = :taskId
           """)
    int deleteByTaskId(@Param("taskId") Long taskId);

    @Transactional
    @Modifying
    @Query("update SparePartInventoryScan s set s.item = null , s.correctsScan = null where s.inventoryTask.id = :taskId")
    void removeItemsByTaskId(@Param("taskId") Long taskId);






}

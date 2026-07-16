package com.pinetechs.orvix.ims.inventory.sparepart.repository;

import com.pinetechs.orvix.ims.inventory.sparepart.entity.SparePartInventoryItem;
import com.pinetechs.orvix.ims.inventory.sparepart.entity.SparePartInventoryScan;
import com.pinetechs.orvix.ims.inventory.sparepart.enums.SparePartInventoryItemStatus;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface SparePartInventoryItemRepository extends JpaRepository<SparePartInventoryItem, Long> {

    Optional<SparePartInventoryItem> findByInventoryTaskIdAndItemNoAndPlannedBranchIdAndPlannedLocationId(Long taskId, String itemNo, Long branchId, Long locationId);

    List<SparePartInventoryItem> findByInventoryTaskIdAndItemNo(Long taskId, String itemNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
           select item from SparePartInventoryItem item
           where item.inventoryTask.id = :taskId
             and upper(item.itemNo) = upper(:itemNo)
             and item.plannedBranch.id = :branchId
             and item.plannedLocation.id = :locationId
           """)
    Optional<SparePartInventoryItem> findExactForUpdate(
            @Param("taskId") Long taskId,
            @Param("itemNo") String itemNo,
            @Param("branchId") Long branchId,
            @Param("locationId") Long locationId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from SparePartInventoryItem item where item.inventoryTask.id = :taskId and upper(item.itemNo) = upper(:itemNo) order by item.id")
    List<SparePartInventoryItem> findCandidatesForUpdate(
            @Param("taskId") Long taskId,
            @Param("itemNo") String itemNo
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from SparePartInventoryItem item where item.id = :itemId and item.inventoryTask.id = :taskId")
    Optional<SparePartInventoryItem> findForUpdateByTaskIdAndId(
            @Param("taskId") Long taskId,
            @Param("itemId") Long itemId
    );

    Page<SparePartInventoryItem> findByInventoryTaskIdOrderByIdAsc(Long taskId, Pageable pageable);

    Page<SparePartInventoryItem> findByInventoryTask_Id(Long taskId, Pageable pageable);

    long countByInventoryTaskId(Long taskId);

    long countByInventoryTaskIdAndStatus(Long taskId, SparePartInventoryItemStatus status);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
           delete from SparePartInventoryItem item
           where item.inventoryTask.id = :taskId
           """)
    int deleteByTaskId(@Param("taskId") Long taskId);





    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update SparePartInventoryItem s set s.currentScan = null where s.inventoryTask.id = :taskId")
    void removeScanItemByTaskId(@Param("taskId") Long taskId);
}

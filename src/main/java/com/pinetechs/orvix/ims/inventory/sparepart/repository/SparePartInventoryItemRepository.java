package com.pinetechs.orvix.ims.inventory.sparepart.repository;

import com.pinetechs.orvix.ims.inventory.sparepart.entity.SparePartInventoryItem;
import com.pinetechs.orvix.ims.inventory.sparepart.enums.SparePartInventoryItemStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SparePartInventoryItemRepository extends JpaRepository<SparePartInventoryItem, Long> {

    Optional<SparePartInventoryItem> findByInventoryTaskIdAndItemNoAndPlannedBranchIdAndPlannedLocationId(Long taskId, String itemNo, Long branchId, Long locationId);

    List<SparePartInventoryItem> findByInventoryTaskIdAndItemNo(Long taskId, String itemNo);

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
}

package com.pinetechs.orvix.ims.inventory.asset.repository;

import com.pinetechs.orvix.ims.inventory.asset.entity.AssetInventoryItem;
import com.pinetechs.orvix.ims.inventory.asset.entity.AssetInventoryScan;
import com.pinetechs.orvix.ims.inventory.asset.enums.AssetInventoryItemStatus;
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

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AssetInventoryItemRepository extends JpaRepository<AssetInventoryItem, Long> {

    Optional<AssetInventoryItem> findByInventoryTaskIdAndBarcode(Long taskId, String barcode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from AssetInventoryItem item where item.inventoryTask.id = :taskId and upper(item.barcode) = upper(:barcode)")
    Optional<AssetInventoryItem> findForUpdateByTaskIdAndBarcode(
            @Param("taskId") Long taskId,
            @Param("barcode") String barcode
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from AssetInventoryItem item where item.id = :itemId and item.inventoryTask.id = :taskId")
    Optional<AssetInventoryItem> findForUpdateByTaskIdAndId(
            @Param("taskId") Long taskId,
            @Param("itemId") Long itemId
    );

    boolean existsByInventoryTaskIdAndBarcode(Long taskId, String barcode);

    List<AssetInventoryItem> findByInventoryTaskId(Long taskId);

    Page<AssetInventoryItem> findByInventoryTaskIdOrderByIdAsc(Long taskId, Pageable pageable);

    Page<AssetInventoryItem> findByInventoryTask_Id(Long taskId, Pageable pageable);

    long countByInventoryTaskId(Long taskId);

    long countByInventoryTaskIdAndStatus(Long taskId, AssetInventoryItemStatus status);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
           delete from AssetInventoryItem item
           where item.inventoryTask.id = :taskId
           """)
    int deleteByTaskId(@Param("taskId") Long taskId);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update AssetInventoryItem a set a.currentScan = null where a.inventoryTask.id =:taskId")
    void removeScanItemByTaskId(@Param("taskId") Long taskId);





}

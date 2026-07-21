package com.pinetechs.orvix.ims.inventory.vehicle.repository;

import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryItem;
import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryScan;
import com.pinetechs.orvix.ims.inventory.vehicle.enums.VehicleInventoryItemStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Range;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface VehicleInventoryItemRepository
        extends JpaRepository<VehicleInventoryItem, Long> {

    boolean existsByInventoryTaskIdAndVinNo(Long taskId, String vinNo);

    Optional<VehicleInventoryItem> findByInventoryTaskIdAndVinNo(
            Long taskId,
            String vinNo
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from VehicleInventoryItem item where item.inventoryTask.id = :taskId and upper(item.vinNo) = upper(:vinNo)")
    Optional<VehicleInventoryItem> findForUpdateByTaskIdAndVinNo(
            @Param("taskId") Long taskId,
            @Param("vinNo") String vinNo
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from VehicleInventoryItem item where item.id = :itemId and item.inventoryTask.id = :taskId")
    Optional<VehicleInventoryItem> findForUpdateByTaskIdAndId(
            @Param("taskId") Long taskId,
            @Param("itemId") Long itemId
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

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
           delete from VehicleInventoryItem i
           where i.inventoryTask.id = :taskId
           """)
    int deleteByTaskId(@Param("taskId") Long taskId);



    Page<VehicleInventoryItem> findByInventoryTaskIdOrderByIdAsc(Long taskId, Pageable pageable);

    Page<VehicleInventoryItem> findByInventoryTask_Id(Long id, Pageable pageable);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update VehicleInventoryItem v set v.currentScan =null where v.inventoryTask.id =:taskId")
    void removeScanItemByTaskId(@Param("taskId")Long taskId);




}

package com.pinetechs.orvix.ims.inventory.sparepart.repository;

import com.pinetechs.orvix.ims.inventory.sparepart.entity.SparePartInventoryScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SparePartInventoryScanRepository extends JpaRepository<SparePartInventoryScan, Long> {

    long countByInventoryTaskId(Long taskId);

    Optional<SparePartInventoryScan> findByInventoryTaskIdAndClientScanId(Long taskId, String clientScanId);

    Optional<SparePartInventoryScan> findByIdAndInventoryTaskId(Long scanId, Long taskId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
           delete from SparePartInventoryScan scan
           where scan.inventoryTask.id = :taskId
           """)
    int deleteByTaskId(@Param("taskId") Long taskId);
}

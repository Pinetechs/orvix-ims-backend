package com.pinetechs.orvix.ims.inventory.sparepart.repository;

import com.pinetechs.orvix.ims.inventory.sparepart.entity.SparePartInventoryScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SparePartInventoryScanRepository extends JpaRepository<SparePartInventoryScan, Long> {

    long countByInventoryTaskId(Long taskId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
           delete from SparePartInventoryScan scan
           where scan.inventoryTask.id = :taskId
           """)
    int deleteByTaskId(@Param("taskId") Long taskId);
}

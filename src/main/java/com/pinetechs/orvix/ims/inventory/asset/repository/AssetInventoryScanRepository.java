package com.pinetechs.orvix.ims.inventory.asset.repository;

import com.pinetechs.orvix.ims.inventory.asset.entity.AssetInventoryScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AssetInventoryScanRepository extends JpaRepository<AssetInventoryScan, Long> {

    long countByInventoryTaskId(Long taskId);

    Optional<AssetInventoryScan> findByInventoryTaskIdAndClientScanId(Long taskId, String clientScanId);

    Optional<AssetInventoryScan> findByIdAndInventoryTaskId(Long scanId, Long taskId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
           delete from AssetInventoryScan scan
           where scan.inventoryTask.id = :taskId
           """)
    int deleteByTaskId(@Param("taskId") Long taskId);
}

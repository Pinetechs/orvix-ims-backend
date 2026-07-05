package com.pinetechs.orvix.ims.inventory.asset.repository;

import com.pinetechs.orvix.ims.inventory.asset.entity.AssetInventoryCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AssetInventoryCategoryRepository extends JpaRepository<AssetInventoryCategory, Long> {

    List<AssetInventoryCategory> findByInventoryTaskId(Long taskId);

    long countByInventoryTaskId(Long taskId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
           delete from AssetInventoryCategory category
           where category.inventoryTask.id = :taskId
           """)
    int deleteByTaskId(@Param("taskId") Long taskId);
}

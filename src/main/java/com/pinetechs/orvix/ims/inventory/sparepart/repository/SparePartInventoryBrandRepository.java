package com.pinetechs.orvix.ims.inventory.sparepart.repository;

import com.pinetechs.orvix.ims.inventory.sparepart.entity.SparePartInventoryBrand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SparePartInventoryBrandRepository extends JpaRepository<SparePartInventoryBrand, Long> {

    Optional<SparePartInventoryBrand> findByInventoryTaskIdAndBrandName(Long taskId, String brandName);

    List<SparePartInventoryBrand> findByInventoryTaskId(Long taskId);

    long countByInventoryTaskId(Long taskId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
           delete from SparePartInventoryBrand brand
           where brand.inventoryTask.id = :taskId
           """)
    int deleteByTaskId(@Param("taskId") Long taskId);
}

package com.pinetechs.orvix.ims.inventory.asset.repository;

import com.pinetechs.orvix.ims.inventory.asset.entity.AssetInventoryFloor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssetInventoryFloorRepository extends JpaRepository<AssetInventoryFloor, Long> {

    Optional<AssetInventoryFloor> findByInventoryTaskIdAndLocationIdAndFloorName(Long taskId, Long locationId, String floorName);

    List<AssetInventoryFloor> findByInventoryTaskId(Long taskId);

    List<AssetInventoryFloor> findByLocationIdOrderByFloorNameAsc(Long locationId);

    long countByInventoryTaskId(Long taskId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
           delete from AssetInventoryFloor floor
           where floor.inventoryTask.id = :taskId
           """)
    int deleteByTaskId(@Param("taskId") Long taskId);
}

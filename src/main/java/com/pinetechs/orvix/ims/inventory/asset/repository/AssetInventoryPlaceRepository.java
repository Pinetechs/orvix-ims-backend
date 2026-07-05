package com.pinetechs.orvix.ims.inventory.asset.repository;

import com.pinetechs.orvix.ims.inventory.asset.entity.AssetInventoryPlace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssetInventoryPlaceRepository extends JpaRepository<AssetInventoryPlace, Long> {

    Optional<AssetInventoryPlace> findByInventoryTaskIdAndLocationIdAndFloorIdAndPlaceName(Long taskId, Long locationId, Long floorId, String placeName);

    List<AssetInventoryPlace> findByInventoryTaskId(Long taskId);

    List<AssetInventoryPlace> findByFloorIdOrderByPlaceNameAsc(Long floorId);

    long countByInventoryTaskId(Long taskId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
           delete from AssetInventoryPlace place
           where place.inventoryTask.id = :taskId
           """)
    int deleteByTaskId(@Param("taskId") Long taskId);
}

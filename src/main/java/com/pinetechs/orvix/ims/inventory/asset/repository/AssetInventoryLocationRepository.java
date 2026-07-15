package com.pinetechs.orvix.ims.inventory.asset.repository;

import com.pinetechs.orvix.ims.inventory.asset.entity.AssetInventoryLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssetInventoryLocationRepository extends JpaRepository<AssetInventoryLocation, Long> {

    Optional<AssetInventoryLocation> findByInventoryTaskIdAndLocationName(Long taskId, String locationName);

    List<AssetInventoryLocation> findByInventoryTaskId(Long taskId);

    boolean existsByInventoryTaskIdAndLocationName(Long taskId, String locationName);

    long countByInventoryTaskId(Long taskId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
           delete from AssetInventoryLocation location
           where location.inventoryTask.id = :taskId
           """)
    int deleteByTaskId(@Param("taskId") Long taskId);

    @Modifying(flushAutomatically = true)
    @Query("""
           update AssetInventoryLocation location
              set location.processedAssets = location.processedAssets + :processedDelta,
                  location.matchedAssets = location.matchedAssets + :matchedDelta
            where location.id = :locationId
           """)
    int adjustScanCounters(
            @Param("locationId") Long locationId,
            @Param("processedDelta") int processedDelta,
            @Param("matchedDelta") int matchedDelta
    );
}

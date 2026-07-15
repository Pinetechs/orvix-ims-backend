package com.pinetechs.orvix.ims.inventory.sparepart.repository;

import com.pinetechs.orvix.ims.inventory.sparepart.entity.SparePartInventoryLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SparePartInventoryLocationRepository extends JpaRepository<SparePartInventoryLocation, Long> {

    Optional<SparePartInventoryLocation> findByInventoryTaskIdAndBranchIdAndLocationCode(Long taskId, Long branchId, String locationCode);

    List<SparePartInventoryLocation> findByInventoryTaskId(Long taskId);

    List<SparePartInventoryLocation> findByBranchIdOrderByLocationCodeAsc(Long branchId);

    long countByInventoryTaskId(Long taskId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
           delete from SparePartInventoryLocation location
           where location.inventoryTask.id = :taskId
           """)
    int deleteByTaskId(@Param("taskId") Long taskId);

    @Modifying(flushAutomatically = true)
    @Query("""
           update SparePartInventoryLocation location
              set location.countedItems = location.countedItems + :countedDelta,
                  location.matchedItems = location.matchedItems + :matchedDelta,
                  location.shortageItems = location.shortageItems + :shortageDelta,
                  location.overageItems = location.overageItems + :overageDelta,
                  location.locationMismatchItems = location.locationMismatchItems + :locationMismatchDelta
            where location.id = :locationId
           """)
    int adjustScanCounters(
            @Param("locationId") Long locationId,
            @Param("countedDelta") int countedDelta,
            @Param("matchedDelta") int matchedDelta,
            @Param("shortageDelta") int shortageDelta,
            @Param("overageDelta") int overageDelta,
            @Param("locationMismatchDelta") int locationMismatchDelta
    );
}

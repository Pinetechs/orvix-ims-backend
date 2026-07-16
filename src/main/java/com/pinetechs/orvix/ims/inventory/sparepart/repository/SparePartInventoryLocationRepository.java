package com.pinetechs.orvix.ims.inventory.sparepart.repository;

import com.pinetechs.orvix.ims.inventory.sparepart.entity.SparePartInventoryLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

public interface SparePartInventoryLocationRepository extends JpaRepository<SparePartInventoryLocation, Long> {

    Optional<SparePartInventoryLocation> findByInventoryTaskIdAndBranchIdAndLocationCode(Long taskId, Long branchId, String locationCode);

    List<SparePartInventoryLocation> findByInventoryTaskId(Long taskId);

    List<SparePartInventoryLocation> findByBranchIdOrderByLocationCodeAsc(Long branchId);

    @Query("""
           select location
           from SparePartInventoryLocation location
           where location.inventoryTask.id = :taskId
             and location.branch.id = :branchId
             and (:search is null or lower(location.locationCode) like lower(concat('%', :search, '%')))
           order by location.locationCode asc
           """)
    List<SparePartInventoryLocation> searchForApp(
            @Param("taskId") Long taskId,
            @Param("branchId") Long branchId,
            @Param("search") String search
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
           select location
           from SparePartInventoryLocation location
           where location.id = :locationId
             and location.inventoryTask.id = :taskId
             and location.branch.id = :branchId
           """)
    Optional<SparePartInventoryLocation> findForUpdate(
            @Param("taskId") Long taskId,
            @Param("branchId") Long branchId,
            @Param("locationId") Long locationId
    );

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

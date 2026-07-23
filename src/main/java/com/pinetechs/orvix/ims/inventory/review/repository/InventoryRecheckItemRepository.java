package com.pinetechs.orvix.ims.inventory.review.repository;

import com.pinetechs.orvix.ims.inventory.review.entity.InventoryRecheckItem;
import com.pinetechs.orvix.ims.inventory.review.enums.RecheckItemStatus;
import com.pinetechs.orvix.ims.inventory.review.enums.RecheckRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InventoryRecheckItemRepository
        extends JpaRepository<InventoryRecheckItem, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
           select distinct item
           from InventoryRecheckItem item
           join fetch item.recheckRequest request
           join fetch request.inventoryTask task
           join fetch task.company
           left join fetch item.issues
           where item.id = :itemId
             and request.id = :requestId
           """)
    Optional<InventoryRecheckItem> findForUpdate(
            @Param("requestId") Long requestId,
            @Param("itemId") Long itemId
    );

    @Query("""
           select case when count(item.id) > 0 then true else false end
           from InventoryRecheckItem item
           join item.issues issue
           where issue.id = :issueId
             and item.status in :statuses
           """)
    boolean existsByIssueIdAndStatusIn(
            @Param("issueId") Long issueId,
            @Param("statuses") Collection<RecheckItemStatus> statuses
    );

    @Query("""
           select distinct item.evidenceImage.id
           from InventoryRecheckItem item
           where item.recheckRequest.inventoryTask.id = :taskId
             and item.evidenceImage is not null
           """)
    List<Long> findEvidenceFileIdsByTaskId(@Param("taskId") Long taskId);

    @Query("""
           select case when count(distinct item.id) > 0 then true else false end
           from InventoryRecheckItem item
           join item.issues issue
           where item.recheckRequest.inventoryTask.id = :taskId
             and item.recheckRequest.assignedTo.id = :userId
             and item.recheckRequest.status in :requestStatuses
             and (issue.sourceScanId = :scanId or issue.currentScanId = :scanId)
           """)
    boolean existsAssignedActiveRecheckForScan(
            @Param("taskId") Long taskId,
            @Param("scanId") Long scanId,
            @Param("userId") Long userId,
            @Param("requestStatuses") Collection<RecheckRequestStatus> requestStatuses
    );
}

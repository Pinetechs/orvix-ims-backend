package com.pinetechs.orvix.ims.inventory.review.repository;

import com.pinetechs.orvix.ims.inventory.review.entity.InventoryRecheckRequest;
import com.pinetechs.orvix.ims.inventory.review.enums.RecheckRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface InventoryRecheckRequestRepository
        extends JpaRepository<InventoryRecheckRequest, Long> {

    @EntityGraph(attributePaths = {"inventoryTask", "assignedTo", "requestedBy"})
    Page<InventoryRecheckRequest> findByInventoryTaskId(
            Long taskId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"inventoryTask", "assignedTo", "requestedBy"})
    Page<InventoryRecheckRequest> findByInventoryTaskIdAndStatus(
            Long taskId,
            RecheckRequestStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"inventoryTask", "inventoryTask.company", "assignedTo", "requestedBy"})
    Page<InventoryRecheckRequest> findByAssignedToIdAndStatusIn(
            Long userId,
            Collection<RecheckRequestStatus> statuses,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
           select request
           from InventoryRecheckRequest request
           join fetch request.inventoryTask task
           join fetch task.company
           join fetch request.assignedTo
           join fetch request.requestedBy
           where request.id = :requestId
           """)
    Optional<InventoryRecheckRequest> findForUpdate(@Param("requestId") Long requestId);

    long countByInventoryTaskIdAndStatusIn(
            Long taskId,
            Collection<RecheckRequestStatus> statuses
    );
}

package com.pinetechs.orvix.ims.inventory.task.repository;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface InventoryTaskRepository extends JpaRepository<InventoryTask, Long>, JpaSpecificationExecutor<InventoryTask> {

    Optional<InventoryTask> findByTaskNumber(String taskNumber);

    List<InventoryTask> findByCompanyId(Long companyId);

    List<InventoryTask> findByCompanyIdAndInventoryDomain(Long companyId, InventoryDomain inventoryDomain);

    List<InventoryTask> findByCompanyIdAndStatus(Long companyId, InventoryTaskStatus status);

    List<InventoryTask> findByCompanyIdAndInventoryDomainAndStatus(
            Long companyId,
            InventoryDomain inventoryDomain,
            InventoryTaskStatus status
    );

    boolean existsByTaskNumber(String taskNumber);

    long countByCompanyIdAndStatus(Long companyId, InventoryTaskStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from InventoryTask t where t.id = :taskId")
    Optional<InventoryTask> findByIdForUpdate(@Param("taskId") Long taskId);

    @Modifying(flushAutomatically = true)
    @Query("""
           update InventoryTask task
              set task.processedRecords = task.processedRecords + :processedDelta,
                  task.matchedRecords = task.matchedRecords + :matchedDelta
            where task.id = :taskId
           """)
    int adjustScanCounters(
            @Param("taskId") Long taskId,
            @Param("processedDelta") int processedDelta,
            @Param("matchedDelta") int matchedDelta
    );

    @Modifying(flushAutomatically = true)
    @Query("""
           update InventoryTask task
              set task.status = :inProgress,
                  task.startDate = coalesce(task.startDate, :startDate),
                  task.startedAt = coalesce(task.startedAt, :startedAt)
            where task.id = :taskId
              and task.status = :readyToStart
           """)
    int markInProgressOnFirstScan(
            @Param("taskId") Long taskId,
            @Param("startDate") LocalDate startDate,
            @Param("startedAt") LocalDateTime startedAt,
            @Param("readyToStart") InventoryTaskStatus readyToStart,
            @Param("inProgress") InventoryTaskStatus inProgress
    );
}

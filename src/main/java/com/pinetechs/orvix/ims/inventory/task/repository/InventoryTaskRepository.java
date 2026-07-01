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
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

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
}

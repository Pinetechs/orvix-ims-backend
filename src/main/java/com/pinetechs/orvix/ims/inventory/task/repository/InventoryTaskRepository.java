package com.pinetechs.orvix.ims.inventory.task.repository;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import com.pinetechs.orvix.ims.inventory.vehicle.entity.VehicleInventoryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryTaskRepository extends JpaRepository<InventoryTask, Long> {
    Page<InventoryTask> findByCompany_Id(Long companyId, Pageable pageable);
    Page<InventoryTask> findByCompany_IdAndInventoryDomain(Long companyId, InventoryDomain inventoryDomain, Pageable pageable);
    Optional<InventoryTask> findByTaskNumber(String taskNumber);

    List<InventoryTask> findByCompanyId(Long companyId);

    List<InventoryTask> findByCompanyIdAndInventoryDomain(Long companyId, InventoryDomain inventoryDomain);

    List<InventoryTask> findByCompanyIdAndStatus(Long companyId, InventoryTaskStatus status);

    List<InventoryTask> findByCompanyIdAndInventoryDomainAndStatus(Long companyId, InventoryDomain inventoryDomain, InventoryTaskStatus status);

    boolean existsByTaskNumber(String taskNumber);

    long countByCompanyIdAndStatus(Long companyId, InventoryTaskStatus status);

    Page<VehicleInventoryItem> findByInventoryTaskId(Long taskId, Pageable pageable);


}

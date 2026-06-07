package com.pinetechs.orvix.ims.inventory.task.repository;

import com.pinetechs.orvix.ims.inventory.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryTaskRepository extends JpaRepository<InventoryTask, Long> {
    Page<InventoryTask> findByCompany_Id(Long companyId, Pageable pageable);
    Page<InventoryTask> findByCompany_IdAndInventoryDomain(Long companyId, InventoryDomain inventoryDomain, Pageable pageable);
}

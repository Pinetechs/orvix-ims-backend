package com.pinetechs.orvix.ims.company.repository;

import com.pinetechs.orvix.ims.company.entity.Company;
import com.pinetechs.orvix.ims.inventory.task.entity.InventoryTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> , JpaSpecificationExecutor<Company> {
    boolean existsByCodeIgnoreCase(String code);
    Optional<Company> findByCodeIgnoreCase(String code);



}

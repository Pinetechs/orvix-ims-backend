package com.pinetechs.orvix.ims.company.repository;

import com.pinetechs.orvix.ims.company.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    boolean existsByCodeIgnoreCase(String code);
    Optional<Company> findByCodeIgnoreCase(String code);
}

package com.pinetechs.orvix.ims.user.repository;

import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.user.entity.User;
import com.pinetechs.orvix.ims.user.enums.AccessChannel;
import com.pinetechs.orvix.ims.user.enums.UserType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;
@EnableJpaRepositories
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsernameIgnoreCaseAndDeletedFalse(String username);
    boolean existsByUsernameIgnoreCase(String username);
    Page<User> findByDeletedFalse(Pageable pageable);

    @Query("""
        select distinct u
        from User u
        left join u.companies companies
        where (
            :search is null
            or upper(u.username) like upper(concat('%', :search, '%'))
            or upper(u.firstName) like upper(concat('%', :search, '%'))
            or upper(u.lastName) like upper(concat('%', :search, '%'))
            or upper(companies.name) like upper(concat('%', :search, '%'))
            or upper(companies.code) like upper(concat('%', :search, '%'))
        )
        and (:userType is null or u.userType = :userType)
        and (:enabled is null or u.enabled = :enabled)
        and (:accessChannel is null or u.accessChannel = :accessChannel)
        and (
            :domainFilterEnabled = false
            or exists (
                select 1
                from User ux
                join ux.inventoryDomains d
                where ux = u
                and d in :domains
            )
        )
        and u.deleted = false
        """)
    Page<User> findByDeletedFalseAndSearchCriteria(
            @Param("search") String search,
            @Param("userType") UserType userType,
            @Param("accessChannel") AccessChannel accessChannel,
            @Param("enabled") Boolean enabled,
            @Param("domains") Set<InventoryDomain> domains,
            @Param("domainFilterEnabled") boolean domainFilterEnabled,
            Pageable pageable
    );


    Page<User> findDistinctByDeletedFalseAndCompanies_IdIn(Set<Long> companyIds, Pageable pageable);


    boolean existsByUserTypeAndDeletedFalse(UserType userType);

    long countByDeletedFalse();

    long countByDeletedFalseAndEnabledTrue();

    @Query("""
        select distinct u
        from User u
        join u.companies company
        join u.inventoryDomains domain
        where company.id = :companyId
          and domain = :domain
          and u.userType = :userType
          and u.enabled = true
          and u.deleted = false
          and (
              :search is null
              or upper(u.username) like upper(concat('%', :search, '%'))
              or upper(u.firstName) like upper(concat('%', :search, '%'))
              or upper(u.lastName) like upper(concat('%', :search, '%'))
              or upper(concat(concat(u.firstName, ' '), u.lastName)) like upper(concat('%', :search, '%'))
          )
        """)
    Page<User> findEligibleInventoryStaff(
            @Param("companyId") Long companyId,
            @Param("domain") InventoryDomain domain,
            @Param("search") String search,
            @Param("userType") UserType userType,
            Pageable pageable
    );


}

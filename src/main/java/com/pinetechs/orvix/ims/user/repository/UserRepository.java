package com.pinetechs.orvix.ims.user.repository;

import com.pinetechs.orvix.ims.user.entity.User;
import com.pinetechs.orvix.ims.user.enums.AccessChannel;
import com.pinetechs.orvix.ims.user.enums.UserType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Optional;
import java.util.Set;
@EnableJpaRepositories
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsernameIgnoreCaseAndDeletedFalse(String username);
    boolean existsByUsernameIgnoreCase(String username);
    Page<User> findByDeletedFalse(Pageable pageable);

    @Query("""
            select u from User u left join u.companies companies
            where (:search is null or Upper(u.username) like Upper(concat('%', :search, '%')) or Upper(u.firstName) like Upper(concat('%', :search, '%')) or Upper(u.lastName) like Upper(concat('%', :search, '%')) or Upper(companies.name) like Upper(concat('%', :search, '%')) or Upper(companies.code) like Upper(concat('%', :search, '%')))
            and (:userType is null or u.userType = :userType)
            and (:enabled is null or u.enabled = :enabled)
            and (:accessChannel is null or u.accessChannel = :accessChannel)
            and u.deleted = false
           
             """)
    Page<User> findByDeletedFalseAndSearchCriteria(String search, UserType userType, AccessChannel accessChannel,Boolean enabled, Pageable pageable);




    Page<User> findDistinctByDeletedFalseAndCompanies_IdIn(Set<Long> companyIds, Pageable pageable);


    boolean existsByUserTypeAndDeletedFalse(UserType userType);


}

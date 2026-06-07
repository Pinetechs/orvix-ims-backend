package com.pinetechs.orvix.ims.user.repository;

import com.pinetechs.orvix.ims.user.entity.User;
import com.pinetechs.orvix.ims.user.enums.UserType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Optional;
import java.util.Set;
@EnableJpaRepositories
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsernameIgnoreCaseAndDeletedFalse(String username);
    boolean existsByUsernameIgnoreCase(String username);
    Page<User> findByDeletedFalse(Pageable pageable);
    Page<User> findDistinctByDeletedFalseAndCompanies_IdIn(Set<Long> companyIds, Pageable pageable);


    boolean existsByUserTypeAndDeletedFalse(UserType userType);


}

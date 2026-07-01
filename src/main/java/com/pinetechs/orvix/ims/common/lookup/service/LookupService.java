package com.pinetechs.orvix.ims.common.lookup.service;

import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.common.lookup.dto.LookupResponse;
import com.pinetechs.orvix.ims.company.entity.Company;
import com.pinetechs.orvix.ims.company.repository.CompanyRepository;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryDomain;
import com.pinetechs.orvix.ims.inventory.common.enums.InventoryTaskStatus;
import com.pinetechs.orvix.ims.user.entity.User;
import com.pinetechs.orvix.ims.user.enums.UserType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Service
public class LookupService {

    private final CompanyRepository companyRepository;

    public LookupService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public List<LookupResponse> searchAllowedCompanies(String search, User currentUser, int limit) {

        if (currentUser == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }

        int safeLimit = Math.min(Math.max(limit, 1), 50);

        boolean allowAllCompanies =
                currentUser.getUserType() == UserType.SYSTEM_ADMIN
                        || currentUser.getUserType() == UserType.PINETECHS_SUPPORT_STAFF;

        List<Long> companyIds = List.of();

        if (!allowAllCompanies) {
            if (currentUser.getCompanies() == null || currentUser.getCompanies().isEmpty()) {
                throw new BusinessException(
                        HttpStatus.FORBIDDEN,
                        "User has no assigned companies"
                );
            }

            companyIds = currentUser.getCompanies()
                    .stream()
                    .map(Company::getId)
                    .toList();
        }

        Pageable pageable = PageRequest.of(
                0,
                safeLimit,
                Sort.by(Sort.Direction.ASC, "name")
        );

        Page<Company> page = companyRepository.findAll(
                getSpecificationCompany(search, companyIds, allowAllCompanies),
                pageable
        );

        return page.getContent()
                .stream()
                .map(company -> LookupResponse.of(
                        company.getId(),
                        company.getName() + " (" + company.getCode() + ")"
                ))
                .toList();
    }

    private Specification<Company> getSpecificationCompany(
            String search,
            List<Long> companyIds,
            boolean allowAllCompanies
    ) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            query.distinct(true);

            if (!allowAllCompanies) {
                predicates.add(root.get("id").in(companyIds));
            }

            if (search != null && !search.isBlank()) {
                String searchPattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";

                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), searchPattern),
                        cb.like(cb.lower(root.get("code")), searchPattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public List<LookupResponse> searchAllowedDomains(String search, User currentUser, int limit) {

        if (currentUser == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }

        if (currentUser.getInventoryDomains() == null || currentUser.getInventoryDomains().isEmpty()) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "User has no assigned inventory domains"
            );
        }

        int safeLimit = Math.min(Math.max(limit, 1), 50);

        Stream<InventoryDomain> stream = currentUser.getInventoryDomains().stream();

        if (search != null && !search.isBlank()) {
            String searchPattern = search.trim().toLowerCase(Locale.ROOT);

            stream = stream.filter(domain ->
                    domain.name().toLowerCase(Locale.ROOT).contains(searchPattern)
            );
        }

        return stream
                .limit(safeLimit)
                .map(domain -> LookupResponse.of(domain.name(),domain.name()))
                .toList();
    }

    public List<LookupResponse> searchTaskStatuses(String search, User currentUser, int limit) {

        if (currentUser == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }

        int safeLimit = Math.min(Math.max(limit, 1), 50);

        Stream<InventoryTaskStatus> stream = Stream.of(InventoryTaskStatus.values());

        if (search != null && !search.isBlank()) {
            String searchPattern = search.trim().toLowerCase(Locale.ROOT);

            stream = stream.filter(status ->
                    status.name().toLowerCase(Locale.ROOT).contains(searchPattern)
            );
        }

        return stream
                .limit(safeLimit)
                .map(status -> LookupResponse.of(status.name(),status.name()))
                .toList();
    }

    public List<LookupResponse> searchUserTypes(String search, User currentUser, int limit) {
        if(currentUser == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }

          int safeLimit = Math.min(Math.max(limit, 1), 50);

        Stream<UserType> stream = Stream.of(UserType.values());

        if (currentUser.getUserType() != UserType.PINETECHS_SUPPORT_STAFF) {
            stream = stream.filter(userType -> userType != UserType.PINETECHS_SUPPORT_STAFF);
        }

        if (search != null && !search.isBlank()) {
            String searchPattern = search.trim().toLowerCase(Locale.ROOT);

            stream = stream.filter(userType ->
                    userType.name().toLowerCase(Locale.ROOT).contains(searchPattern)
            );
        }

        return stream
                .limit(safeLimit)
                .map(userType -> LookupResponse.of(userType.name(), userType.name()))
                .toList();

    }
}
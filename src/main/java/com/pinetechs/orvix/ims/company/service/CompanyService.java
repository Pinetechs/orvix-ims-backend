package com.pinetechs.orvix.ims.company.service;

import com.pinetechs.orvix.ims.common.exception.BusinessException;
import com.pinetechs.orvix.ims.company.dto.CompanyResponse;
import com.pinetechs.orvix.ims.company.dto.CreateCompanyRequest;
import com.pinetechs.orvix.ims.company.dto.UpdateCompanyRequest;
import com.pinetechs.orvix.ims.company.entity.Company;
import com.pinetechs.orvix.ims.company.repository.CompanyRepository;
import com.pinetechs.orvix.ims.security.AccessPolicyService;
import com.pinetechs.orvix.ims.user.entity.User;
import com.pinetechs.orvix.ims.user.enums.PermissionCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final AccessPolicyService accessPolicyService;

    public CompanyService(CompanyRepository companyRepository, AccessPolicyService accessPolicyService) {
        this.companyRepository = companyRepository;
        this.accessPolicyService = accessPolicyService;
    }

    public CompanyResponse create(CreateCompanyRequest request, User currentUser) {
        accessPolicyService.assertPermission(currentUser, PermissionCode.COMPANY_CREATE, "User does not have permission to create a company");
        String code = normalizeCode(request.getCode());
        if (companyRepository.existsByCodeIgnoreCase(code)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Company code already exists");
        }
        Company company = new Company();
        company.setCode(code);
        company.setName(request.getName());
        company.setActive(true);
        return CompanyResponse.from(companyRepository.save(company));
    }

    public CompanyResponse update(Long id, UpdateCompanyRequest request, User currentUser) {
        requireSystemAdmin(currentUser, PermissionCode.COMPANY_UPDATE);
        Company company = findCompany(id);
        company.setName(request.getName());
        company.setActive(request.getActive() == null || request.getActive());
        return CompanyResponse.from(companyRepository.save(company));
    }

    @Transactional(readOnly = true)
    public Page<CompanyResponse> getAll(Pageable pageable, User currentUser) {
        accessPolicyService.assertPermission(currentUser, PermissionCode.COMPANY_VIEW, "User does not have permission to view companies");
        return companyRepository.findAll(pageable).map(CompanyResponse::from);
    }

    public void disable(Long id, User currentUser) {
        requireSystemAdmin(currentUser, PermissionCode.COMPANY_DISABLE);
        Company company = findCompany(id);
        company.setActive(false);
        companyRepository.save(company);
    }

    private Company findCompany(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Company not found"));
    }

    private void requireSystemAdmin(User user, PermissionCode permission) {
        if (user == null || !user.isSystemAdmin() || !user.hasPermission(permission)) {
            throw new BusinessException(HttpStatus.FORBIDDEN,"System admin permission is required");
        }
    }

    private String normalizeCode(String code) { return trimRequired(code, "Code").toUpperCase(); }
    private String trimRequired(String value, String field) {
        if (value == null || value.trim().isEmpty()) throw new BusinessException(HttpStatus.BAD_REQUEST, field + " is required");
        return value.trim();
    }
    private String trimToNull(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
}

package com.pinetechs.orvix.ims.company.service;

import com.pinetechs.orvix.ims.company.dto.CompanyResponse;
import com.pinetechs.orvix.ims.company.dto.CreateCompanyRequest;
import com.pinetechs.orvix.ims.company.dto.UpdateCompanyRequest;
import com.pinetechs.orvix.ims.company.entity.Company;
import com.pinetechs.orvix.ims.company.repository.CompanyRepository;
import com.pinetechs.orvix.ims.user.entity.User;
import com.pinetechs.orvix.ims.user.enums.PermissionCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public CompanyResponse create(CreateCompanyRequest request, User currentUser) {
        requireSystemAdmin(currentUser, PermissionCode.COMPANY_CREATE);
        String code = normalizeCode(request.getCode());
        if (companyRepository.existsByCodeIgnoreCase(code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Company code already exists");
        }
        Company company = new Company();
        company.setCode(code);
        company.setNameAr(trimRequired(request.getNameAr(), "Arabic name"));
        company.setNameEn(trimToNull(request.getNameEn()));
        company.setActive(true);
        return CompanyResponse.from(companyRepository.save(company));
    }

    public CompanyResponse update(Long id, UpdateCompanyRequest request, User currentUser) {
        requireSystemAdmin(currentUser, PermissionCode.COMPANY_UPDATE);
        Company company = findCompany(id);
        company.setNameAr(trimRequired(request.getNameAr(), "Arabic name"));
        company.setNameEn(trimToNull(request.getNameEn()));
        company.setActive(request.getActive() == null || request.getActive());
        return CompanyResponse.from(companyRepository.save(company));
    }

    @Transactional(readOnly = true)
    public Page<CompanyResponse> getAll(Pageable pageable, User currentUser) {
        requireSystemAdmin(currentUser, PermissionCode.COMPANY_VIEW);
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));
    }

    private void requireSystemAdmin(User user, PermissionCode permission) {
        if (user == null || !user.isSystemAdmin() || !user.hasPermission(permission)) {
            throw new AccessDeniedException("System admin permission is required");
        }
    }

    private String normalizeCode(String code) { return trimRequired(code, "Code").toUpperCase(); }
    private String trimRequired(String value, String field) {
        if (value == null || value.trim().isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
        return value.trim();
    }
    private String trimToNull(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
}

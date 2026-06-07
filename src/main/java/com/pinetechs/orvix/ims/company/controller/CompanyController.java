package com.pinetechs.orvix.ims.company.controller;

import com.pinetechs.orvix.ims.auth.security.JwtUserDetails;
import com.pinetechs.orvix.ims.common.ApiResponse;
import com.pinetechs.orvix.ims.company.dto.CompanyResponse;
import com.pinetechs.orvix.ims.company.dto.CreateCompanyRequest;
import com.pinetechs.orvix.ims.company.dto.UpdateCompanyRequest;
import com.pinetechs.orvix.ims.company.service.CompanyService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping
    public ResponseEntity<CompanyResponse> create(@Valid @RequestBody CreateCompanyRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(companyService.create(request, currentUser(authentication)));
    }

    @PutMapping("/{id}")
    public CompanyResponse update(@PathVariable Long id, @Valid @RequestBody UpdateCompanyRequest request, Authentication authentication) {
        return companyService.update(id, request, currentUser(authentication));
    }

    @GetMapping
    public Page<CompanyResponse> getAll(@RequestParam(name = "page", defaultValue = "0") int page,
                                        @RequestParam(name = "size", defaultValue = "20") int size,
                                        @RequestParam(name = "sortBy", defaultValue = "id") String sortBy,
                                        @RequestParam(name = "sortOrder", defaultValue = "desc") String sortOrder,
                                        Authentication authentication) {
        Sort sort = "asc".equalsIgnoreCase(sortOrder) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return companyService.getAll(pageable, currentUser(authentication));
    }

    @PutMapping("/{id}/disable")
    public ApiResponse disable(@PathVariable Long id, Authentication authentication) {
        companyService.disable(id, currentUser(authentication));
        return ApiResponse.ok("Company disabled successfully");
    }

    private com.pinetechs.orvix.ims.user.entity.User currentUser(Authentication authentication) {
        return ((JwtUserDetails) authentication.getPrincipal()).getUser();
    }
}

package com.pinetechs.orvix.ims.company.dto;

import com.pinetechs.orvix.ims.company.entity.Company;
import java.time.LocalDateTime;

public class CompanyResponse {
    private Long id;
    private String code;
    private String nameAr;
    private String nameEn;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CompanyResponse from(Company company) {
        CompanyResponse response = new CompanyResponse();
        response.id = company.getId();
        response.code = company.getCode();
        response.nameAr = company.getNameAr();
        response.nameEn = company.getNameEn();
        response.active = company.getActive();
        response.createdAt = company.getCreatedAt();
        response.updatedAt = company.getUpdatedAt();
        return response;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getNameAr() { return nameAr; }
    public String getNameEn() { return nameEn; }
    public Boolean getActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

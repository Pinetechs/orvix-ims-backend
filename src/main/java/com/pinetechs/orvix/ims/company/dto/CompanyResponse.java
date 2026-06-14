package com.pinetechs.orvix.ims.company.dto;

import com.pinetechs.orvix.ims.company.entity.Company;
import java.time.LocalDateTime;

public class CompanyResponse {
    private Long id;
    private String code;
    private String name;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CompanyResponse from(Company company) {
        CompanyResponse response = new CompanyResponse();
        response.id = company.getId();
        response.code = company.getCode();
        response.name = company.getName();
        response.active = company.getActive();
        response.createdAt = company.getCreatedAt();
        response.updatedAt = company.getUpdatedAt();
        return response;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }

    public String getName() {
        return name;
    }



    public Boolean getActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

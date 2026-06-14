package com.pinetechs.orvix.ims.company.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateCompanyRequest {
    @NotBlank
    private String name;
    private Boolean active;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}

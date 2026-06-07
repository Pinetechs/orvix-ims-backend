package com.pinetechs.orvix.ims.company.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateCompanyRequest {
    @NotBlank
    private String nameAr;
    private String nameEn;
    private Boolean active;

    public String getNameAr() { return nameAr; }
    public void setNameAr(String nameAr) { this.nameAr = nameAr; }
    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}

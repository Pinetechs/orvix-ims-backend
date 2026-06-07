package com.pinetechs.orvix.ims.company.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateCompanyRequest {
    @NotBlank
    private String code;
    @NotBlank
    private String nameAr;
    private String nameEn;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getNameAr() { return nameAr; }
    public void setNameAr(String nameAr) { this.nameAr = nameAr; }
    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }
}

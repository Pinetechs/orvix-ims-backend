package com.pinetechs.orvix.ims.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResetPasswordRequest {
    @NotBlank
    @Size(min = 6)
    private String newPassword;

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}

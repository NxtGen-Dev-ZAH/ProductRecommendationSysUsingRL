package com.datasaz.ecommerce.models.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdatePasswordRequest {
    @NotBlank
    private String email;
    @NotBlank
    private String oldPassword;
    @NotBlank
    @Size(min = 8, message = "New password must be at least 8 characters long")
    private String newPassword;
}
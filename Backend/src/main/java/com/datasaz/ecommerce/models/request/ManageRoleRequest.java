package com.datasaz.ecommerce.models.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ManageRoleRequest {

    @NotBlank
    @Email(message = "Invalid email format")
    private String email;
    @NotBlank
    private String role;
}
package com.datasaz.ecommerce.models.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoleRequest {
    @NotBlank
    private String role;
}
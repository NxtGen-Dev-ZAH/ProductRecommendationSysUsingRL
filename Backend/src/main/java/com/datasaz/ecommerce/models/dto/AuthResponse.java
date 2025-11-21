package com.datasaz.ecommerce.models.dto;

import com.datasaz.ecommerce.repositories.entities.Roles;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private String refreshToken;
    private String email;
    private Set<Roles> userRoles;
    private String provider;
    private String message;
}


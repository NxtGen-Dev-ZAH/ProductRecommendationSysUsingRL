package com.datasaz.ecommerce.models.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class RegisterRequest {

    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;

    @NotBlank
    @Email(message = "Invalid email format")
    private String emailAddress;
    private String phoneNumber;
    private String registrationIp;

    @NotBlank
    @Size(min = 8, message = "New password must be at least 8 characters long")
    private String password;
    @NotBlank
    @Size(min = 8, message = "New password must be at least 8 characters long")
    private String confirmPassword;
    //private Set<RoleRequest> roles;
}

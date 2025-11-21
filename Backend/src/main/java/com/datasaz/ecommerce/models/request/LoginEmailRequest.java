package com.datasaz.ecommerce.models.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

//@Data
//@Builder
@Getter
@Setter
public class LoginEmailRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String emailAddress;
    @NotBlank(message = "New password is required")
    private String password;
}

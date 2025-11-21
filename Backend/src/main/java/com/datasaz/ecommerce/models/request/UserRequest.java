package com.datasaz.ecommerce.models.request;

import com.datasaz.ecommerce.repositories.entities.Product;
import com.datasaz.ecommerce.repositories.entities.Roles;
import com.datasaz.ecommerce.repositories.entities.User;
import jakarta.validation.constraints.Email;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class UserRequest {

    private String firstName;
    private String lastName;
    @Email(message = "Invalid email format")
    private String emailAddress;
    private String password;
    private Boolean isResetPassword;
    private String resetToken;
    private String phoneNumber;
    private String provider;
    private LocalDateTime registrationDate;
    private String registrationIp;
    private String activationCode;
    private Boolean isActivated;
    private Boolean isBlocked;
    private LocalDateTime lastLoginDate;
    private LocalDateTime lastPasswordResetDate;

    private String location;

    private Set<Roles> userRoles;

    private Set<Product> favoriteProducts;

    private Set<User> following;
    private Set<User> followers;

    private String profilePictureUrl;
}

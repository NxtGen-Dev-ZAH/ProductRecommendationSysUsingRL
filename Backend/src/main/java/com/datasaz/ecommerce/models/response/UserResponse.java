package com.datasaz.ecommerce.models.response;

import com.datasaz.ecommerce.repositories.entities.Product;
import com.datasaz.ecommerce.repositories.entities.Roles;
import com.datasaz.ecommerce.repositories.entities.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class UserResponse {
    //Users users;
    private Long id;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
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

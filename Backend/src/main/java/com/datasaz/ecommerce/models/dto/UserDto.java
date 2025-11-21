package com.datasaz.ecommerce.models.dto;

import com.datasaz.ecommerce.repositories.entities.Roles;
import com.datasaz.ecommerce.repositories.entities.UserPrivacySettings;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDto {
    private Long id;

    @Size(max = 100, message = "First name must be less than 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name must be less than 100 characters")
    private String lastName;

    private LocalDate dateOfBirth;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String emailAddress;

    private String password;
    private String phoneNumber;
    private String provider;
    private String resetToken;
    private Boolean isResetPassword;
    private Boolean isActivated;
    private Boolean isBlocked;
    private String activationCode;
    private String location;
    private String registrationIp;
    private LocalDateTime registrationDate;
    private LocalDateTime lastLoginDate;
    private LocalDateTime lastPasswordResetDate;

    private Set<Long> favoriteProductIds;
    private Set<Long> followingIds;
    private Set<Long> followerIds;
    private Set<Roles> userRoles;
    private Long companyId;

    private String profilePictureUrl;
    private String imageContent;
    private String imageContentType;

    private Boolean deleted;
    private String deletionToken;

    private Long followerCount;
    private Long followingCount;

    private UserPrivacySettings.Visibility profileVisibility;
    private UserPrivacySettings.Visibility followersVisibility;
    private UserPrivacySettings.Visibility followingVisibility;
    private UserPrivacySettings.Visibility favoritesVisibility;

    private Set<CustomFieldDto> customFields;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomFieldDto {
        private Long id;
        @NotBlank(message = "Field key is required")
        @Size(max = 100, message = "Field key must be less than 100 characters")
        private String fieldKey;
        @NotBlank(message = "Field value is required")
        @Size(max = 250, message = "Field value must be less than 250 characters")
        private String fieldValue;
        @Size(max = 500, message = "Description must be less than 500 characters")
        private String description;
    }
}
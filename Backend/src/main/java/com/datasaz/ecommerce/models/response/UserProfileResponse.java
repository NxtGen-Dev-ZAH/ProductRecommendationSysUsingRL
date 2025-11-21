package com.datasaz.ecommerce.models.response;

import com.datasaz.ecommerce.repositories.entities.UserPrivacySettings;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.Set;

@Data
@Builder
public class UserProfileResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String emailAddress;
    private String phoneNumber;
    private String location;
    private String profilePictureUrl;
    private String imageContent;
    private String imageContentType;
    private UserPrivacySettings privacySettings;
    private Set<String> userRoles;
    private Set<Long> favoriteProducts;
    private Set<Long> followers;
    private Integer followersCount;
    private Set<Long> following;
    private Integer followingCount;
    private Long companyId;
}
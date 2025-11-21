package com.datasaz.ecommerce.models.request;

import lombok.Data;

// del redunt with UserProfileRequest
@Data
public class ProfileUpdateRequest {
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String location;
    private String profilePictureUrl;
}

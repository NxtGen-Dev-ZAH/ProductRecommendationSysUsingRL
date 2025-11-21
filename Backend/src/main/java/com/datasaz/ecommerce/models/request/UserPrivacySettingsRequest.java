package com.datasaz.ecommerce.models.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserPrivacySettingsRequest {
    private String profileVisibility;
    private String followersVisibility;
    private String followingVisibility;
    private String favoritesVisibility;
}

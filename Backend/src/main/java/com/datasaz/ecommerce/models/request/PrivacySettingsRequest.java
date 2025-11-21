package com.datasaz.ecommerce.models.request;

import com.datasaz.ecommerce.repositories.entities.UserPrivacySettings;
import lombok.Data;

@Data
public class PrivacySettingsRequest {
    private UserPrivacySettings.Visibility profileVisibility;
    private UserPrivacySettings.Visibility followersVisibility;
    private UserPrivacySettings.Visibility followingVisibility;
    private UserPrivacySettings.Visibility favoritesVisibility;
}

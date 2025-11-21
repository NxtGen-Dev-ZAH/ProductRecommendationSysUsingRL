package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.repositories.entities.User;
import com.datasaz.ecommerce.repositories.entities.UserPrivacySettings;
import com.datasaz.ecommerce.services.interfaces.IUserPrivacySettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPrivacySettingsService implements IUserPrivacySettingsService {

    @Override
    public UserPrivacySettings createDefaultPrivacySettings(User user) {
        UserPrivacySettings settings = UserPrivacySettings.builder()
                .user(user)
                .profileVisibility(UserPrivacySettings.Visibility.PUBLIC)
                .followersVisibility(UserPrivacySettings.Visibility.PUBLIC)
                .followingVisibility(UserPrivacySettings.Visibility.PUBLIC)
                .favoritesVisibility(UserPrivacySettings.Visibility.PUBLIC)
                .build();
        log.info("Created default privacy settings for user: {}", user.getEmailAddress());
        return settings;
    }
}


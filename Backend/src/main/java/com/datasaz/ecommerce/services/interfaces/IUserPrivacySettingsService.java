package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.repositories.entities.User;
import com.datasaz.ecommerce.repositories.entities.UserPrivacySettings;

public interface IUserPrivacySettingsService {

    UserPrivacySettings createDefaultPrivacySettings(User user);
}

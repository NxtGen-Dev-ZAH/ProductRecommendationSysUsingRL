package com.datasaz.ecommerce.repositories;


import com.datasaz.ecommerce.repositories.entities.UserPrivacySettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPrivacySettingsRepository extends JpaRepository<UserPrivacySettings, Long> {
    Optional<UserPrivacySettings> findByUserId(Long userId);

}

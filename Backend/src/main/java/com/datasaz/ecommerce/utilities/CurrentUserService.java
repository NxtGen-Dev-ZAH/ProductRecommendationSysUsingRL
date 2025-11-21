package com.datasaz.ecommerce.utilities;

import com.datasaz.ecommerce.exceptions.UserNotFoundException;
import com.datasaz.ecommerce.repositories.UserRepository;
import com.datasaz.ecommerce.repositories.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CurrentUserService {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailAddress(email)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found").build());
    }
}

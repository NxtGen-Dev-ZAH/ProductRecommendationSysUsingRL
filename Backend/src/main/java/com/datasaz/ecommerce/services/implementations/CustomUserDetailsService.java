package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.repositories.UserRepository;
import com.datasaz.ecommerce.repositories.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // Check if user is activated and not blocked
        boolean isEnabled = user.getIsActivated() != null && user.getIsActivated() && !user.getIsBlocked();

        // Optional: Check account expiration (e.g., 1 year after registration)
        boolean isAccountNonExpired = user.getRegistrationDate() == null ||
                user.getRegistrationDate().plusYears(1).isAfter(LocalDateTime.now());

        // Optional: Check credentials expiration (e.g., 90 days after last password reset)
        boolean isCredentialsNonExpired = user.getLastPasswordResetDate() == null ||
                user.getLastPasswordResetDate().plusDays(90).isAfter(LocalDateTime.now());

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmailAddress())
                .password(user.getPassword() != null ? user.getPassword() : "nopassword")
                .authorities(user.getUserRoles().stream()
                        .map(userRoles -> new SimpleGrantedAuthority("ROLE_" + userRoles.getRole().name()))
                        .collect(Collectors.toList()))
//                .accountNonExpired(isAccountNonExpired)
//                .accountNonLocked(true) // Assuming no lockout mechanism; set to true
//                .credentialsNonExpired(isCredentialsNonExpired)
                .disabled(!isEnabled)
                .build();
    }
}

//    @Override
//    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
//        User user = userRepository.findByEmailAddress(email)
//                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
//
////        String[] roles = user.getUserRoles().stream()
////                .map(userRole -> "ROLE_" + userRole.getRole().name())
////                .toArray(String[]::new);
//
//        return org.springframework.security.core.userdetails.User.builder()
//                .username(user.getEmailAddress())
//                .password(user.getPassword() != null ? user.getPassword() : "nopassword")
//                .roles(user.getUserRoles().stream()
//                        .map(userRole -> userRole.getRole().name())
//                        .toArray(String[]::new))
//                .build();
//    }




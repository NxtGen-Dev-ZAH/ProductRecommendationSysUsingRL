package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.TechnicalException;
import com.datasaz.ecommerce.exceptions.UserNotFoundException;
import com.datasaz.ecommerce.mappers.UserMapper;
import com.datasaz.ecommerce.models.dto.UserDto;
import com.datasaz.ecommerce.repositories.ProductRepository;
import com.datasaz.ecommerce.repositories.RefreshTokenRepository;
import com.datasaz.ecommerce.repositories.UserRepository;
import com.datasaz.ecommerce.repositories.entities.Company;
import com.datasaz.ecommerce.repositories.entities.RefreshToken;
import com.datasaz.ecommerce.repositories.entities.User;
import com.datasaz.ecommerce.services.interfaces.IEmailService;
import com.datasaz.ecommerce.services.interfaces.IUserService;
import com.datasaz.ecommerce.utilities.JwtBlacklistService;
import com.datasaz.ecommerce.utilities.Utility;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserService implements IUserService {

    private final PasswordEncoder passwordEncoder;

    // private static final String DEFAULT_PROFILE_PICTURE = "/uploads/profile-pictures/default-profile-picture.jpg";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    private final SellerUserRoleService sellerUserRoleService;
    private final ProductRepository productRepository;

    private final AuditLogService auditLogService;
    private final IEmailService emailService;

    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;

    private final JwtBlacklistService jwtBlacklistService;

    //private static final String UPLOAD_DIR = "uploads/profile-pictures/";
    private static final Utility utility = new Utility();


    @Override
    @Transactional
    public UserDto updateUserPassword(String email, String oldPassword, String newPassword, String jwtToken) {
        log.info("updateUserPassword: Attempting to update password for user: {}", email);

        utility.validateEmail(email);

        // Verify authenticated user matches the request
        String authenticatedEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!email.equals(authenticatedEmail)) {
            log.error("updateUserPassword: Unauthorized attempt to update password for {} by {}", email, authenticatedEmail);
            throw BadRequestException.builder().message("Unauthorized: Can only update your own password").build();
        }

        // Find user
        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + email).build());

        // For OAuth2 users, check if password is "nopassword"
        if (user.getProvider() != null && user.getPassword().equals("nopassword")) {
            // OAuth2 users can set a password without old password
            if (newPassword == null || newPassword.isEmpty()) {
                log.error("updateUserPassword: New password cannot be empty for user: {}", email);
                throw BadRequestException.builder().message("New password cannot be empty").build();
            }
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setProvider(null); // Transition to password-based authentication
        } else {
            // For non-OAuth2 users, verify old password
            if (oldPassword == null || oldPassword.isEmpty()) {
                log.error("updateUserPassword: Old password required for non-OAuth2 user: {}", email);
                throw BadRequestException.builder().message("Old password is required").build();
            }
            try {
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(email, oldPassword));
            } catch (BadCredentialsException e) {
                log.error("updateUserPassword: Invalid old password for user: {}", email);
                throw BadRequestException.builder().message("Invalid old password").build();
            }
            if (newPassword == null || newPassword.isEmpty()) {
                log.error("updateUserPassword: New password cannot be empty for user: {}", email);
                throw BadRequestException.builder().message("New password cannot be empty").build();
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        // Save updated user
        userRepository.save(user);

        // Log audit event
        auditLogService.logAction(email, "UPDATE_PASSWORD", "Password updated for user: " + email);

        // Send notification
        try {
            emailService.sendPasswordChangeNotification(email, LocalDateTime.now());
        } catch (Exception e) {
            log.error("updateUserPassword: Failed to send password change notification to {}: {}", email, e.getMessage());
        }

        revokeAllRefreshTokens(email, jwtToken);
        log.info("updateUserPassword: Revoked all refresh tokens for user: {}", email);

        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    public UserDto updateUser(UserDto userDto, String jwtToken) {
        log.info("updateUser: Attempting to update user profile for email: {}", userDto.getEmailAddress());

        // Verify authenticated user matches the request
        String authenticatedEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!userDto.getEmailAddress().equals(authenticatedEmail)) {
            log.error("updateUser: Unauthorized attempt to update user {} by {}", userDto.getEmailAddress(), authenticatedEmail);
            throw BadRequestException.builder().message("Unauthorized: Can only update your own profile").build();
        }

        // Find user
        User user = userRepository.findByEmailAddressAndDeletedFalse(userDto.getEmailAddress())
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + userDto.getEmailAddress()).build());

        // Update allowed fields
        if (userDto.getFirstName() != null && !userDto.getFirstName().isEmpty()) {
            user.setFirstName(userDto.getFirstName());
        } else {
            log.warn("updateUser: First Name not provided or empty for user: {}", userDto.getEmailAddress());
        }

        if (userDto.getLastName() != null && !userDto.getLastName().isEmpty()) {
            user.setLastName(userDto.getLastName());
        } else {
            log.warn("updateUser: First Name not provided or empty for user: {}", userDto.getEmailAddress());
        }

        if (userDto.getDateOfBirth() != null && !user.getDateOfBirth().equals(userDto.getDateOfBirth())) {
            if (userDto.getDateOfBirth().isAfter(LocalDate.now())) {
                log.error("updateUser: Date of birth cannot be in the future: {}", userDto.getDateOfBirth());
                throw BadRequestException.builder().message("Date of birth cannot be in the future").build();
            }
            if (Period.between(userDto.getDateOfBirth(), LocalDate.now()).getYears() < 18) {
                log.error("updateUser: User must be at least 18 years old: {}", userDto.getDateOfBirth());
                throw BadRequestException.builder().message("User must be at least 18 years old").build();
            }
            user.setDateOfBirth(userDto.getDateOfBirth());
        }

        // changes to critical fields

        if ((userDto.getPhoneNumber() != null && !userDto.getPhoneNumber().isEmpty()) && !userDto.getPhoneNumber().equals(user.getPhoneNumber())) {
            log.info("updateUser: Phone number changed for user: {} from {} to {}.", userDto.getEmailAddress(), user.getPhoneNumber(), userDto.getPhoneNumber());
            auditLogService.logAction(user.getEmailAddress(), "PHONE NUMBER CHANGE", "Phone number changed from " + user.getPhoneNumber() + " to " + userDto.getPhoneNumber());
            user.setPhoneNumber(userDto.getPhoneNumber());
        }

//       moved to the dedicated endpoint and method changeEmail
//        if (!user.getEmailAddress().equals(userDto.getEmailAddress())) {
//            log.info("updateUser: Changed email address from {} to {}", user.getEmailAddress(), userDto.getEmailAddress());
//            auditLogService.logAction(user.getEmailAddress(), "EMAIL ADDRESS CHANGE", "Email address changed from " + user.getEmailAddress() + " to " + userDto.getEmailAddress());
//            user.setEmailAddress(userDto.getEmailAddress());
//        }

        // Not allowed changes to critical fields
        if (userDto.getPassword() != null && !userDto.getPassword().isEmpty()) {
            log.error("updateUser: Attempt to change password for user: {}", user.getEmailAddress());
            throw BadRequestException.builder().message("Changing password is not allowed.").build();
        }

        // Save updated user
        userRepository.save(user);

        // Log audit event
        auditLogService.logAction(userDto.getEmailAddress(), "UPDATE_PROFILE", "User profile updated for user: " + userDto.getEmailAddress());

        // Send notification
        try {
            emailService.sendProfileUpdateNotification(userDto.getEmailAddress(), LocalDateTime.now());
        } catch (Exception e) {
            log.error("updateUser: Failed to send profile update notification to {}: {}", userDto.getEmailAddress(), e.getMessage());
        }

        revokeAllRefreshTokens(userDto.getEmailAddress(), jwtToken);

        log.info("updateUser: Revoked all refresh tokens for user: {}", userDto.getEmailAddress());

        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    public UserDto changeEmail(String oldEmail, String newEmail, String password, String jwtToken) {
        utility.validateEmail(newEmail);

        String authenticatedEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        if (!oldEmail.equals(authenticatedEmail)) {
            throw BadRequestException.builder().message("Unauthorized: Can only change your own email").build();
        }
        User user = userRepository.findByEmailAddressAndDeletedFalse(oldEmail)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + oldEmail).build());
        if (userRepository.findByEmailAddress(newEmail).isPresent()) {
            throw BadRequestException.builder().message("New email already in use").build();
        }
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(oldEmail, password));
        String verificationToken = UUID.randomUUID().toString();
        user.setEmailAddress(newEmail);
        user.setActivationCode(verificationToken);
        user.setIsActivated(false); // Require re-verification
        userRepository.save(user);

        revokeAllRefreshTokens(oldEmail, jwtToken);

        emailService.sendEmailChangeVerification(newEmail, verificationToken);
        auditLogService.logAction(oldEmail, "EMAIL_CHANGE", "Email changed to " + newEmail);
        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    public void deleteUser(String password, String jwtToken) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("deleteUser: User {} is initiating account deletion", email);

        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + email).build());

        if (user.getDeleted()) {
            log.warn("deleteUser: User {} is already deleted", email);
            throw new RuntimeException("User is already deleted");
        }

        if (user.getProvider() != null && user.getPassword().equals("nopassword")) {
            // OAuth2 user: generate and send deletion token
            String deletionToken = UUID.randomUUID().toString();
            user.setDeletionToken(deletionToken);
            userRepository.save(user);

            // Send deletion confirmation email
            try {
                emailService.sendDeletionConfirmationEmail(email, deletionToken);
            } catch (Exception e) {
                log.error("Failed to send deletion confirmation email to {}: {}", email, e.getMessage());
                throw new RuntimeException("Failed to send deletion confirmation email", e);
            }
            log.info("deleteUser: Deletion confirmation email sent to {}", email);
        } else {
            // Password-based user: verify password
            try {
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(email, password));
            } catch (BadCredentialsException e) {
                log.error("deleteUser: Invalid password for user {}", email);
                throw new IllegalArgumentException("Invalid password");
            }
            completeUserDeletion(user, jwtToken);
        }
    }

    @Override
    @Transactional
    public UserDto confirmDeleteUser(String token, String jwtToken) {
        log.info("confirmDeleteUser: Confirming deletion with token");
        Optional<User> userOptional = userRepository.findByDeletionToken(token);
        if (userOptional.isEmpty()) {
            log.error("confirmDeleteUser: Invalid deletion token");
            throw new IllegalArgumentException("Invalid deletion token");
        }
        User user = userOptional.get();
        String email = user.getEmailAddress();

        String authenticatedEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!email.equals(authenticatedEmail)) {
            log.error("confirmDeleteUser: Unauthorized attempt to delete user {} by {}", email, authenticatedEmail);
            throw new IllegalArgumentException("Unauthorized deletion attempt");
        }

        if (user.getDeleted()) {
            log.warn("confirmDeleteUser: User {} is already deleted", email);
            throw new RuntimeException("User is already deleted");
        }

        completeUserDeletion(user, jwtToken);
        return userMapper.toDto(user);
    }

    @Transactional
    @Override
    public void completeUserDeletion(User user, String jwtToken) {
        String email = user.getEmailAddress();
        log.info("completeUserDeletion: Deleting user {}", email);

        // Handle company association
        if (user.getCompany() != null) {
            Company company = user.getCompany();
            if (company.getPrimaryAdmin() != null && company.getPrimaryAdmin().getId().equals(user.getId())) {
                try {
                    sellerUserRoleService.deleteCompany(company.getId());
                } catch (MessagingException e) {
                    log.error("completeUserDeletion: Failed to delete company {} for user {}: {}",
                            company.getId(), email, e.getMessage());
                    throw TechnicalException.builder().message("An error occurred while attempt to delete company " + company.getName() + " (" + company.getId() + ").").build();
                }
            } else {
                user.setCompany(null);
                User primaryAdmin = company.getPrimaryAdmin();
                if (primaryAdmin != null) {
                    // Move user-authored products to company's primary admin for the same company
                    productRepository.updateAuthorForUserProducts(user.getId(), primaryAdmin.getId(), company.getId());
                }
                userRepository.save(user);
            }
        }

        // Delete all user-authored products (soft delete)
        productRepository.deleteByAuthorId(user.getId());

        user.setDeleted(true);
        user.setIsActivated(false);
        user.setDeletionToken(null);
        userRepository.save(user);

        LocalDateTime timestamp = LocalDateTime.now();

        auditLogService.logAction(email, "DELETE_USER",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                "Deleted user at: " + timestamp);

        try {
            emailService.sendUserDeletionNotification(email, timestamp);
        } catch (Exception e) {
            log.error("Failed to send deletion notification to {}: {}", email, e.getMessage());
        }

        revokeAllRefreshTokens(email, jwtToken);
        log.info("completeUserDeletion: Revoked all refresh tokens for user {}", email);
    }

    @Override
    @Transactional
    public String logout(String username, String refreshToken, String jwtToken) {
        log.info("logout: Attempting to logout for user: {}, refreshToken: {}", username, refreshToken);

        User user = userRepository.findByEmailAddressAndDeletedFalse(username)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + username).build());

        if (user.getIsBlocked()) {
            log.error("User {} is blocked", username);
            throw BadRequestException.builder().message("Account is blocked. Please contact support.").build();
        }

        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            log.warn("logout: Invalid refresh token provided for user: {}", username);
            throw new IllegalArgumentException("Invalid login session");
        }

        Optional<RefreshToken> refreshTokenEntity = refreshTokenRepository.findByToken(refreshToken);
        if (refreshTokenEntity.isPresent()) {
            RefreshToken token = refreshTokenEntity.get();
            if (!token.getUserEmail().equals(username)) {
                log.error("logout: Refresh token does not belong to user: {}", username);
                throw BadRequestException.builder().message("Invalid login session for user").build();
            }
            if (token.isRevoked()) {
                log.warn("logout: Refresh token already revoked for user: {}", username);
            } else {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
                log.info("logout: Refresh token revoked for user: {}", username);
            }
        } else {
            log.warn("logout: Invalid refresh token for user: {}", username);
            throw new IllegalArgumentException("Invalid login session");
        }

        revokeAllRefreshTokens(username, jwtToken);

        log.info("logout: All refresh tokens revoked for user: {}", username);

        auditLogService.logAction(username, "LOGOUT", "User logged out at: " + LocalDateTime.now());

        return "You have been successfully logged out.";
    }

    private void revokeAllRefreshTokens(String email, String jwtToken) {
        if (jwtToken == null || jwtToken.trim().isEmpty()) {
            log.error("revokeAllRefreshTokens: Invalid or missing JWT token for user: {}", email);
            throw BadRequestException.builder().message("Invalid or missing JWT token").build();
        }
        jwtBlacklistService.blacklistToken(jwtToken);
        refreshTokenRepository.deleteByUserEmail(email);
        log.info("Revoked all refresh tokens and blacklisted JWT for user: {}", email);
    }

}

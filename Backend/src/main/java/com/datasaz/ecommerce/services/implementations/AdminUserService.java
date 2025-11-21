package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.UserNotFoundException;
import com.datasaz.ecommerce.mappers.UserMapper;
import com.datasaz.ecommerce.models.dto.UserDto;
import com.datasaz.ecommerce.models.request.UpdatePasswordRequest;
import com.datasaz.ecommerce.repositories.RefreshTokenRepository;
import com.datasaz.ecommerce.repositories.UserRepository;
import com.datasaz.ecommerce.repositories.entities.User;
import com.datasaz.ecommerce.services.interfaces.IAdminUserService;
import com.datasaz.ecommerce.services.interfaces.IEmailService;
import com.datasaz.ecommerce.utilities.JwtBlacklistService;
import com.datasaz.ecommerce.utilities.Utility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserService implements IAdminUserService {

    private final UserRepository userRepository;
    private final IEmailService emailService;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtBlacklistService jwtBlacklistService;

    private static final Utility utility = new Utility();

    @Override
    @Transactional(readOnly = true)
    public Page<UserDto> findAll(int page, int size) {
        log.info("findAll: Finding all users, page: {}, size: {}", page, size);
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<User> users = userRepository.findAllWithAllCollections(pageable);
            Page<UserDto> userDtos = users.map(userMapper::toDto);
            log.info("findAll: Found {} users", userDtos.getTotalElements());
            return userDtos;
        } catch (Exception e) {
            log.error("Error finding all users: {}", e.getMessage());
            throw BadRequestException.builder().message("Error finding all users.").build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDto> findById(Long id) {
        log.info("findById: Admin {} finding user by id: {}",
                SecurityContextHolder.getContext().getAuthentication().getName(), id);
        if (id == null) {
            log.error("findById: ID cannot be null");
            throw new IllegalArgumentException("ID cannot be null");
        }
        Optional<User> user = userRepository.findByIdWithAllCollections(id);
        if (user.isEmpty()) {
            log.error("findById: User not found with id: {}", id);
            throw UserNotFoundException.builder().message("User not found with id: " + id).build();
        }
        return user.map(userMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDto> findByEmail(String email) {
        log.info("findByEmail: Admin {} finding user by email: {}",
                SecurityContextHolder.getContext().getAuthentication().getName(), email);
        utility.validateEmail(email);
        Optional<User> user = userRepository.findByEmailAddressWithAllCollections(email);
        if (user.isEmpty()) {
            log.error("findByEmail: User not found with email: {}", email);
            throw UserNotFoundException.builder().message("User not found with email: " + email).build();
        }
        return user.map(userMapper::toDto);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        log.info("deleteById: Admin {} deleting user by id: {}",
                SecurityContextHolder.getContext().getAuthentication().getName(), id);
        if (id == null) {
            log.error("deleteById: ID cannot be null");
            throw new IllegalArgumentException("ID cannot be null");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with id: " + id).build());
        if (user.getDeleted()) {
            log.warn("deleteById: User {} is already deleted", id);
            throw new RuntimeException("User is already deleted");
        }
        user.setDeleted(true);
        user.setIsActivated(false);
        user.setDeletionToken(null);
        userRepository.save(user);
        auditLogService.logAction(user.getEmailAddress(), "DELETE_USER_ADMIN",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                "User deleted by admin at: " + LocalDateTime.now());
        try {
            emailService.sendUserDeletionNotification(user.getEmailAddress(), LocalDateTime.now());
        } catch (Exception e) {
            log.error("deleteById: Failed to send deletion notification to {}: {}", user.getEmailAddress(), e.getMessage());
        }
        revokeAllRefreshTokens(user.getEmailAddress());
    }

    @Override
    @Transactional
    public UserDto updateUserPassword(UpdatePasswordRequest updatePasswordRequest) {
        log.info("updateUserPassword: Admin {} updating password for user: {}",
                SecurityContextHolder.getContext().getAuthentication().getName(), updatePasswordRequest.getEmail());
        utility.validateEmail(updatePasswordRequest.getEmail());
        String newPassword = updatePasswordRequest.getNewPassword();
        if (newPassword == null || newPassword.length() < 8) {
            log.error("updateUserPassword: New password must be at least 8 characters for user: {}", updatePasswordRequest.getEmail());
            throw BadRequestException.builder().message("New password must be at least 8 characters").build();
        }
        User user = userRepository.findByEmailAddressAndDeletedFalse(updatePasswordRequest.getEmail())
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found: " + updatePasswordRequest.getEmail()).build());
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        auditLogService.logAction(user.getEmailAddress(), "UPDATE_PASSWORD_ADMIN",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                "Password updated by admin at: " + LocalDateTime.now());
        try {
            emailService.sendPasswordChangeNotification(user.getEmailAddress(), LocalDateTime.now());
        } catch (Exception e) {
            log.error("updateUserPassword: Failed to send password change notification to {}: {}", user.getEmailAddress(), e.getMessage());
        }
        revokeAllRefreshTokens(user.getEmailAddress());
        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    public UserDto restoreUser(String email) {
        log.info("restoreUser: Admin {} restoring user with email: {}",
                SecurityContextHolder.getContext().getAuthentication().getName(), email);
        utility.validateEmail(email);
        User user = userRepository.findByEmailAddress(email)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + email).build());
        if (!user.getDeleted()) {
            log.warn("restoreUser: User {} is not deleted", email);
            throw new RuntimeException("User is not deleted");
        }
        user.setDeleted(false);
        user.setIsActivated(true);
        user.setDeletionToken(null);
        userRepository.save(user);
        auditLogService.logAction(email, "RESTORE_USER",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                "Restored user at: " + LocalDateTime.now());
        try {
            emailService.sendUserRestorationNotification(email, LocalDateTime.now());
        } catch (Exception e) {
            log.error("restoreUser: Failed to send restoration notification to {}: {}", email, e.getMessage());
        }
        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    public String blockUser(String email, String reason) {
        log.info("blockUser: Attempting to block user with email: {}", email);
        utility.validateEmail(email);
        if (reason == null || reason.trim().isEmpty() || reason.length() > 255) {
            log.error("blockUser: Invalid reason for blocking user: {}", email);
            throw BadRequestException.builder().message("Reason must be non-empty and less than 255 characters").build();
        }
        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + email).build());
        if (user.getIsBlocked()) {
            log.info("blockUser: User {} is already blocked", email);
            return "User is already blocked.";
        }
        user.setIsBlocked(true);
        userRepository.save(user);
        auditLogService.logAction(email, "BLOCK_USER",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                "User blocked at: " + LocalDateTime.now() + ", reason: " + reason);
        try {
            emailService.sendBlockNotification(email, reason, LocalDateTime.now());
        } catch (Exception e) {
            log.error("blockUser: Failed to send block notification to {}: {}", email, e.getMessage());
        }
        revokeAllRefreshTokens(email);
        return "User blocked successfully.";
    }

    @Override
    @Transactional
    public String unblockUser(String email) {
        log.info("unblockUser: Attempting to unblock user with email: {}", email);
        utility.validateEmail(email);
        User user = userRepository.findByEmailAddressAndDeletedFalse(email)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + email).build());
        if (!user.getIsBlocked()) {
            log.info("unblockUser: User {} is not blocked", email);
            return "User is not blocked.";
        }
        user.setIsBlocked(false);
        userRepository.save(user);
        auditLogService.logAction(email, "UNBLOCK_USER",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                "User unblocked at: " + LocalDateTime.now());
        try {
            emailService.sendUnblockNotification(email, LocalDateTime.now());
        } catch (Exception e) {
            log.error("unblockUser: Failed to send unblock notification to {}: {}", email, e.getMessage());
        }
        return "User unblocked successfully.";
    }

    private void revokeAllRefreshTokens(String email) {
        refreshTokenRepository.deleteByUserEmail(email);
        log.info("Revoked all refresh tokens and blacklisted JWT for user: {}", email);
    }
}
package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.IllegalParameterException;
import com.datasaz.ecommerce.exceptions.ResourceNotFoundException;
import com.datasaz.ecommerce.exceptions.UserNotFoundException;
import com.datasaz.ecommerce.mappers.UserMapper;
import com.datasaz.ecommerce.models.dto.UserDto;
import com.datasaz.ecommerce.repositories.RefreshTokenRepository;
import com.datasaz.ecommerce.repositories.RolesRepository;
import com.datasaz.ecommerce.repositories.UserRepository;
import com.datasaz.ecommerce.repositories.entities.RoleTypes;
import com.datasaz.ecommerce.repositories.entities.Roles;
import com.datasaz.ecommerce.repositories.entities.User;
import com.datasaz.ecommerce.services.interfaces.IAdminUserRoleService;
import com.datasaz.ecommerce.services.interfaces.IAuditLogService;
import com.datasaz.ecommerce.services.interfaces.IEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Service
@Slf4j
public class AdminUserRoleService implements IAdminUserRoleService {

    private final UserRepository userRepository;
    private final RolesRepository rolesRepository;
    private final UserMapper userMapper;
    private final IEmailService emailService;
    private final IAuditLogService auditLogService;
    private final RefreshTokenRepository refreshTokenRepository;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    @Deprecated
    @Override
    @Transactional
    public UserDto addUserRole(Long userId, String role) {
        log.info("addUserRole: Adding role {} to userId: {}", role, userId);
        User user = userRepository.findByIdWithAllCollections(userId)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with id: " + userId).build());

        RoleTypes roleType;
        try {
            roleType = RoleTypes.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Invalid role: {}", role);
            throw IllegalParameterException.builder().message("Invalid role: " + role).build();
        }

        boolean hasRole = user.getUserRoles().stream()
                .anyMatch(r -> r.getRole().equals(roleType));
        if (hasRole) {
            log.error("addUserRole: User {} already has role {}", userId, roleType.name());
            throw BadRequestException.builder().message("User already has role " + roleType.name()).build();
        }

        try {
            Roles roleEntity = rolesRepository.findByRole(roleType)
                    .orElseGet(() -> rolesRepository.save(Roles.builder().role(roleType).build()));
            user.getUserRoles().add(roleEntity);
            userRepository.save(user);
            auditLogService.logAction(user.getEmailAddress(), "ADD_USER_ROLE",
                    SecurityContextHolder.getContext().getAuthentication().getName(),
                    "Added role " + roleType.name() + " to user " + user.getEmailAddress());
            refreshTokenRepository.deleteByUserEmail(user.getEmailAddress());
            log.info("addUserRole: Revoked all refresh tokens for user {}", user.getEmailAddress());
        } catch (Exception e) {
            log.error("Error updating user: {}", e.getMessage());
            throw BadRequestException.builder().message("Error updating user.").build();
        }

        return userMapper.toDto(user);
    }

    @Deprecated
    @Override
    @Transactional
    public UserDto removeUserRole(Long userId, String role) {
        log.info("removeUserRole: Removing role {} from userId: {}", role, userId);
        User user = userRepository.findByIdWithAllCollections(userId)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with id: " + userId).build());

        RoleTypes roleType;
        try {
            roleType = RoleTypes.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Invalid role: {}", role);
            throw IllegalParameterException.builder().message("Invalid role: " + role).build();
        }

        Roles roleEntity = rolesRepository.findByRole(roleType)
                .orElseThrow(() -> ResourceNotFoundException.builder().message("Role not found: " + role).build());

        if (!user.getUserRoles().contains(roleEntity)) {
            log.warn("removeUserRole: User {} does not have role {}", userId, role);
            throw BadRequestException.builder().message("User does not have role: " + role).build();
        }

        user.getUserRoles().remove(roleEntity);
        userRepository.save(user);
        auditLogService.logAction(user.getEmailAddress(), "REMOVE_USER_ROLE",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                "Removed role " + roleType.name() + " from user " + user.getEmailAddress());

        refreshTokenRepository.deleteByUserEmail(user.getEmailAddress());
        log.info("removeUserRole: Revoked all refresh tokens for user {}", user.getEmailAddress());

        return userMapper.toDto(user);
    }

    @Override
    public Set<Roles> getUserRoles(Long userId) {
        log.info("getUserRoles: Retrieving roles for userId: {}", userId);
        return userRepository.findRolesByUserId(userId);
    }

    @Override
    public UserDto assignSellerRole(String email) {
        log.info("assignSellerRole: Assigning SELLER role to email: {}", email);
        return assignRole(email, RoleTypes.SELLER.name());
    }

    @Override
    public UserDto removeSellerRole(String email) {
        log.info("removeSellerRole: Removing SELLER role from email: {}", email);
        return removeRole(email, RoleTypes.SELLER.name());
    }

    @Override
    @Transactional
    public UserDto assignRole(String email, String roleName) {
        log.info("assignRole: Assigning role {} to email: {}", roleName, email);
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            log.error("Invalid email format: {}", email);
            throw IllegalParameterException.builder().message("Invalid email format").build();
        }
        RoleTypes roleType;
        try {
            roleType = RoleTypes.valueOf(roleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Invalid role: {}", roleName);
            throw IllegalParameterException.builder().message("Invalid role: " + roleName).build();
        }
        User user = userRepository.findByEmailAddressWithAllCollections(email)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + email).build());

        boolean hasRole = user.getUserRoles().stream()
                .anyMatch(role -> role.getRole().equals(roleType));
        if (hasRole) {
            log.warn("assignRole: User {} already has role {}", email, roleName);
            throw BadRequestException.builder().message("User already has role " + roleName).build();
        }

        Roles role = rolesRepository.findByRole(roleType)
                .orElseGet(() -> rolesRepository.save(Roles.builder().role(roleType).build()));
        user.getUserRoles().add(role);
        userRepository.save(user);

        auditLogService.logAction(email, "ASSIGN_ROLE",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                roleName,
                "Assigned role: " + roleName + " to user: " + email);
        emailService.sendRoleChangeNotification(email, roleName, "ASSIGN_ROLE", LocalDateTime.now());

        refreshTokenRepository.deleteByUserEmail(email);
        log.info("assignRole: Revoked all refresh tokens for user {}", email);

        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    public UserDto removeRole(String email, String roleName) {
        log.info("removeRole: Removing role {} from email: {}", roleName, email);
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            log.error("Invalid email format: {}", email);
            throw IllegalParameterException.builder().message("Invalid email format").build();
        }
        RoleTypes roleType;
        try {
            roleType = RoleTypes.valueOf(roleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Invalid role: {}", roleName);
            throw IllegalParameterException.builder().message("Invalid role: " + roleName).build();
        }
        if (roleType == RoleTypes.BUYER) {
            log.warn("removeRole: Cannot remove BUYER role from user {}", email);
            throw IllegalParameterException.builder().message("Cannot remove BUYER role").build();
        }
        User user = userRepository.findByEmailAddressWithAllCollections(email)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + email).build());

        boolean hasRole = user.getUserRoles().stream()
                .anyMatch(role -> role.getRole().equals(roleType));
        if (!hasRole) {
            log.warn("removeRole: User {} does not have role {}", email, roleName);
            throw BadRequestException.builder().message("User does not have role " + roleName).build();
        }

        user.getUserRoles().removeIf(role -> role.getRole().equals(roleType));
        userRepository.save(user);

        auditLogService.logAction(email, "REMOVE_ROLE",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                roleName,
                "Removed role: " + roleName + " from user: " + email);
        emailService.sendRoleChangeNotification(email, roleName, "REMOVE_ROLE", LocalDateTime.now());

        refreshTokenRepository.deleteByUserEmail(email);
        log.info("removeRole: Revoked all refresh tokens for user {}", email);

        return userMapper.toDto(user);
    }
}
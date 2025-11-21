package com.datasaz.ecommerce.controllers.admin;

import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.models.dto.AuditLogDto;
import com.datasaz.ecommerce.models.dto.UserDto;
import com.datasaz.ecommerce.models.request.UpdatePasswordRequest;
import com.datasaz.ecommerce.services.interfaces.IAdminUserService;
import com.datasaz.ecommerce.services.interfaces.IAuditLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/user")
public class AdminUserController {

    private final IAdminUserService adminUserService;
    private final IAuditLogService auditLogService;

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('ROLE_APP_ADMIN')")
    public ResponseEntity<Page<UserDto>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("AdminUserController.getAllUsers: Retrieving users, page: {}, size: {}", page, size);
        return ResponseEntity.ok(adminUserService.findAll(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_APP_ADMIN')")
    public ResponseEntity<UserDto> getUserById(@PathVariable("id") Long id) {
        log.info("AdminUserController.getUserById: Retrieving user with id: {}", id);
        Optional<UserDto> userDto = adminUserService.findById(id);
        return userDto.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/email/{email}")
    @PreAuthorize("hasAuthority('ROLE_APP_ADMIN')")
    public ResponseEntity<UserDto> getUserByEmail(@PathVariable("email") String email) {
        log.info("AdminUserController.getUserByEmail: Retrieving user with email: {}", email);
        if (email != null) {
            email = email.trim();
        }
        Optional<UserDto> userDto = adminUserService.findByEmail(email);
        return userDto.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_APP_ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") Long id) {
        log.info("AdminUserController.deleteUser: Deleting user with id: {}", id);
        adminUserService.deleteById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PutMapping("/update")
    @PreAuthorize("hasAuthority('ROLE_APP_ADMIN')")
    public ResponseEntity<UserDto> updateUserPassword(@Valid @RequestBody UpdatePasswordRequest updatePasswordRequest) {
        log.info("AdminUserController.updateUserPassword: Updating password for email: {}", updatePasswordRequest.getEmail());
        if (updatePasswordRequest.getEmail() == null || updatePasswordRequest.getNewPassword() == null) {
            log.error("Invalid UpdatePasswordRequest: email or newPassword is null");
            throw BadRequestException.builder().message("Email and new password are required").build();
        }
        UserDto updatedUser = adminUserService.updateUserPassword(updatePasswordRequest);
        return ResponseEntity.ok(updatedUser);
    }

    @PostMapping("/restore-user")
    @PreAuthorize("hasAuthority('ROLE_APP_ADMIN')")
    public ResponseEntity<UserDto> restoreUser(@RequestBody String email) {
        log.info("AdminUserController.restoreUser: Restoring user with email: {}", email);
        if (email != null) {
            email = email.trim();
        }
        return ResponseEntity.ok(adminUserService.restoreUser(email));
    }

    @PostMapping("/block-user")
    @PreAuthorize("hasAuthority('ROLE_APP_ADMIN')")
    public ResponseEntity<String> blockUser(@RequestBody Map<String, String> request) {
        log.info("AdminUserController.blockUser: Processing block request for email: {}", request.get("email"));
        String email = request.get("email");
        if (email != null) {
            email = email.trim();
        }
        String reason = request.getOrDefault("reason", "No reason provided");
        String response = adminUserService.blockUser(email, reason);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/unblock-user")
    @PreAuthorize("hasAuthority('ROLE_APP_ADMIN')")
    public ResponseEntity<String> unblockUser(@RequestBody Map<String, String> request) {
        log.info("AdminUserController.unblockUser: Processing unblock request for email: {}", request.get("email"));
        String email = request.get("email");
        if (email != null) {
            email = email.trim();
        }
        String response = adminUserService.unblockUser(email);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasAuthority('ROLE_APP_ADMIN')")
    public ResponseEntity<Page<AuditLogDto>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String userEmail,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String details,
            @RequestParam(required = false) String performedBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("AdminUserController.getAuditLogs: Retrieving audit logs for page {}, size {}", page, size);
        return ResponseEntity.ok(auditLogService.getAuditLogs(page, size, userEmail, roleName, action, details, performedBy, startDate, endDate));
    }
}